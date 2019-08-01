package de.metas.inoutcandidate.api.impl;

import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2015 metas GmbH
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

import java.math.BigDecimal;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_InOutLine;

import de.metas.document.engine.DocStatus;
import de.metas.inoutcandidate.api.IShipmentScheduleAllocBL;
import de.metas.inoutcandidate.api.IShipmentScheduleAllocDAO;
import de.metas.inoutcandidate.api.IShipmentScheduleBL;
import de.metas.inoutcandidate.model.I_M_ShipmentSchedule;
import de.metas.inoutcandidate.model.I_M_ShipmentSchedule_QtyPicked;
import de.metas.product.IProductBL;
import de.metas.product.ProductId;
import de.metas.quantity.Quantity;
import de.metas.quantity.StockQtyAndUOMQty;
import de.metas.quantity.StockQtyAndUOMQty.StockQtyAndUOMQtyBuilder;
import de.metas.uom.IUOMConversionBL;
import de.metas.uom.UOMConversionContext;
import de.metas.util.Services;
import lombok.NonNull;

public class ShipmentScheduleAllocBL implements IShipmentScheduleAllocBL
{
	private enum Mode
	{
		/** Just take the given {@code qtyPicked} (converted to sched's UOM ) and set it as the new {@code schedQtyPicked}'s {@code QtyPicked value}. */
		JUST_SET_QTY,

		/** Retrieve the sched's qty that is picked, but not yet shipped, and subtract that quantity from the given {@code qtyPicked}. */
		SUBTRACT_FROM_ALREADY_PICKED_QTY
	}

	@Override
	public void setQtyPicked(final I_M_ShipmentSchedule sched, final BigDecimal qtyPicked)
	{
		final I_C_UOM uom = Services.get(IShipmentScheduleBL.class).getUomOfProduct(sched);
		setQtyPicked(sched, Quantity.of(qtyPicked, uom), Mode.SUBTRACT_FROM_ALREADY_PICKED_QTY);
	}

	@Override
	public I_M_ShipmentSchedule_QtyPicked addQtyPicked(
			final I_M_ShipmentSchedule sched,
			final Quantity qtyPickedDiff)
	{
		return setQtyPicked(sched, qtyPickedDiff, Mode.JUST_SET_QTY);
	}

	/**
	 * Adds or sets QtyPicked
	 *
	 * @param justAdd if true, then if will create a {@link I_M_ShipmentSchedule_QtyPicked} only for difference between given <code>qtyPicked</code> and current qty picked.
	 *
	 * @return {@link I_M_ShipmentSchedule_QtyPicked} created record
	 */
	private I_M_ShipmentSchedule_QtyPicked setQtyPicked(
			@NonNull final I_M_ShipmentSchedule sched,
			@NonNull final Quantity qtyPicked,
			@NonNull final Mode mode)
	{
		final IShipmentScheduleBL shipmentScheduleBL = Services.get(IShipmentScheduleBL.class);
		final IUOMConversionBL uomConversionBL = Services.get(IUOMConversionBL.class);

		final ProductId productId = ProductId.ofRepoId(sched.getM_Product_ID());
		final I_C_UOM schedUOM = shipmentScheduleBL.getUomOfProduct(sched);

		// Convert QtyPicked to shipment schedule's UOM
		final UOMConversionContext conversionCtx = UOMConversionContext.of(productId);
		final Quantity qtyPickedConv = uomConversionBL.convertQuantityTo(qtyPicked, conversionCtx, schedUOM);

		final Quantity qtyPickedToAdd;
		switch (mode)
		{
			case JUST_SET_QTY:
				qtyPickedToAdd = qtyPickedConv;
				break;
			case SUBTRACT_FROM_ALREADY_PICKED_QTY:
				final IShipmentScheduleAllocDAO shipmentScheduleAllocDAO = Services.get(IShipmentScheduleAllocDAO.class);
				final BigDecimal qtyPickedOld = shipmentScheduleAllocDAO.retrieveNotOnShipmentLineQty(sched);
				qtyPickedToAdd = qtyPickedConv.subtract(qtyPickedOld);
				break;
			default:
				throw new AdempiereException("Unexpected mode=" + mode + "; qtyPicked=" + qtyPicked + "; sched=" + sched);
		}

		final I_M_ShipmentSchedule_QtyPicked schedQtyPicked = newInstance(I_M_ShipmentSchedule_QtyPicked.class, sched);
		schedQtyPicked.setAD_Org_ID(sched.getAD_Org_ID());
		schedQtyPicked.setM_ShipmentSchedule(sched);
		schedQtyPicked.setIsActive(true);
		schedQtyPicked.setQtyPicked(qtyPickedToAdd.toBigDecimal());
		saveRecord(schedQtyPicked);

		return schedQtyPicked;
	}

	@Override
	public boolean isDelivered(final I_M_ShipmentSchedule_QtyPicked alloc)
	{
		// task 08959
		// Only the allocations made on inout lines that belong to a completed inout are considered Delivered.
		final I_M_InOutLine line = alloc.getM_InOutLine();
		if (line == null)
		{
			return false;
		}

		final org.compiere.model.I_M_InOut io = line.getM_InOut();

		return DocStatus.ofCode(io.getDocStatus()).isCompletedOrClosed();
	}

	@Override
	public StockQtyAndUOMQty retrieveQtyPickedAndUnconfirmed(@NonNull final I_M_ShipmentSchedule shipmentSchedule)
	{
		final IProductBL productBL = Services.get(IProductBL.class);

		final IShipmentScheduleAllocDAO shipmentScheduleAllocDAO = Services.get(IShipmentScheduleAllocDAO.class);

		final ProductId productId = ProductId.ofRepoId(shipmentSchedule.getM_Product_ID());
		final I_C_UOM stockingUOM = productBL.getStockingUOM(productId);
		final Optional<I_C_UOM> catchUOM = productBL.getCatchUOM(productId);

		final BigDecimal qtyPicked = shipmentScheduleAllocDAO.retrieveQtyPickedAndUnconfirmed(shipmentSchedule);

		final StockQtyAndUOMQtyBuilder result = StockQtyAndUOMQty.builder()
				.productId(productId)
				.stockQty(Quantity.of(qtyPicked, stockingUOM));
		if (catchUOM.isPresent())
		{
			result.uomQty(Quantity.zero(catchUOM.get())); // TODO retrieve the real catch quantity
		}
		return result.build();
	}

}
