package de.metas.handlingunits.inventory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.adempiere.mm.attributes.api.IAttributeSetInstanceBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_M_Attribute;
import org.compiere.model.I_M_AttributeSetInstance;

import de.metas.handlingunits.HuId;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactory;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactoryService;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_InventoryLine;
import de.metas.handlingunits.storage.IHUProductStorage;
import de.metas.inventory.IInventoryDAO;
import de.metas.quantity.Quantity;
import de.metas.util.Services;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

/*
 * #%L
 * de.metas.handlingunits.base
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Creates or updates inventory lines for
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Value
public class DraftInventoryLinesCreator
{

	IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);

	@NonNull
	final DraftInventoryLines draftInventoryLines;

	final Set<Integer> seenLocatorIds = new HashSet<>();

	@NonFinal
	long countInventoryLines = 0;

	public DraftInventoryLinesCreator(DraftInventoryLines draftInventoryLines)
	{
		this.draftInventoryLines = draftInventoryLines;
	}

	public void execute()
	{
		final HUsForInventoryStrategy strategy = draftInventoryLines.getStrategy();

		// create/update new lines
		final Iterator<I_M_HU> hus = strategy.streamHus().iterator();
		while (hus.hasNext())
		{
			final I_M_HU hu = hus.next();
			seenLocatorIds.add(hu.getM_Locator_ID());

			if (strategy.getMaxLocatorsAllowed() > 0 && strategy.getMaxLocatorsAllowed() < seenLocatorIds.size())
			{
				return;
			}

			countInventoryLines = countInventoryLines + createOrUpdateInventoryLines(hu).count();
		}
	}

	private Stream<I_M_InventoryLine> createOrUpdateInventoryLines(@NonNull final I_M_HU hu)
	{
		return handlingUnitsBL
				.getStorageFactory()
				.streamHUProductStorages(hu)
				.filter(huProductStorage -> !huProductStorage.isEmpty())
				.map(this::createOrUpdateInventoryLine);
	}

	private I_M_InventoryLine createOrUpdateInventoryLine(@NonNull final IHUProductStorage huProductStorage)
	{
		final I_M_InventoryLine inventoryLine;
		final I_M_HU hu = huProductStorage.getM_HU();

		final HuId huId = HuId.ofRepoId(hu.getM_HU_ID());

		if (draftInventoryLines.getPreExistingInventoryLinesByHU().containsKey(huId))
		{
			// update line
			inventoryLine = draftInventoryLines.getPreExistingInventoryLinesByHU().get(huId);
		}
		else
		{
			// create line
			inventoryLine = InterfaceWrapperHelper.newInstance(I_M_InventoryLine.class);
			inventoryLine.setM_Inventory(draftInventoryLines.getInventoryRecord());
			inventoryLine.setAD_Org_ID(draftInventoryLines.getInventoryRecord().getAD_Org_ID());
		}

		final I_M_AttributeSetInstance asi = createStorageRelevantASI(hu);
		inventoryLine.setM_AttributeSetInstance(asi);

		inventoryLine.setM_HU_ID(hu.getM_HU_ID());
		inventoryLine.setM_HU_PI_Item_Product(null); // TODO
		inventoryLine.setQtyTU(BigDecimal.ZERO); // TODO

		inventoryLine.setM_Locator_ID(hu.getM_Locator_ID());
		inventoryLine.setM_Product_ID(huProductStorage.getProductId().getRepoId());

		final Quantity qty = huProductStorage.getQty();
		inventoryLine.setC_UOM_ID(qty.getUOMId());
		inventoryLine.setQtyBook(qty.getAsBigDecimal());
		inventoryLine.setQtyCount(qty.getAsBigDecimal());

		Services.get(IInventoryDAO.class).save(inventoryLine);

		return inventoryLine;
	}

	private I_M_AttributeSetInstance createStorageRelevantASI(@NonNull final I_M_HU hu)
	{
		final IAttributeStorageFactoryService attributeStorageFactoryService = Services.get(IAttributeStorageFactoryService.class);
		final IAttributeSetInstanceBL attributeSetInstanceBL = Services.get(IAttributeSetInstanceBL.class);

		final IAttributeStorageFactory attributeStorageFactory = attributeStorageFactoryService	.createHUAttributeStorageFactory();
		final IAttributeStorage attributeStorage = attributeStorageFactory.getAttributeStorage(hu);

		final I_M_AttributeSetInstance asi = attributeSetInstanceBL.createASIFromAttributeSet(
				attributeStorage,
				I_M_Attribute::isStorageRelevant);
		return asi;
	}
}
