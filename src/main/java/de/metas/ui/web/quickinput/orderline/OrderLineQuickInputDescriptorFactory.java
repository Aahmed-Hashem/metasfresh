package de.metas.ui.web.quickinput.orderline;

import java.util.Optional;
import java.util.Set;

import org.adempiere.ad.callout.api.ICalloutField;
import org.adempiere.ad.expression.api.ConstantLogicExpression;
import org.compiere.model.I_C_OrderLine;
import org.compiere.util.DisplayType;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

import de.metas.adempiere.model.I_C_Order;
import de.metas.bpartner.ShipmentAllocationBestBeforePolicy;
import de.metas.handlingunits.order.api.IHUOrderBL;
import de.metas.i18n.IMsgBL;
import de.metas.i18n.ITranslatableString;
import de.metas.lang.SOTrx;
import de.metas.product.ProductId;
import de.metas.ui.web.material.adapter.AvailableToPromiseAdapter;
import de.metas.ui.web.quickinput.IQuickInputDescriptorFactory;
import de.metas.ui.web.quickinput.QuickInput;
import de.metas.ui.web.quickinput.QuickInputConstants;
import de.metas.ui.web.quickinput.QuickInputDescriptor;
import de.metas.ui.web.quickinput.QuickInputLayoutDescriptor;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentType;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.descriptor.DetailId;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor.Characteristic;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.sql.ProductLookupDescriptor;
import de.metas.ui.web.window.descriptor.sql.ProductLookupDescriptor.ProductAndAttributes;
import de.metas.ui.web.window.descriptor.sql.SqlLookupDescriptor;
import de.metas.ui.web.window.model.Document;
import de.metas.util.Services;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

@Component
/* package */ final class OrderLineQuickInputDescriptorFactory implements IQuickInputDescriptorFactory
{
	private final IMsgBL msgBL = Services.get(IMsgBL.class);
	private final IHUOrderBL huOrderBL = Services.get(IHUOrderBL.class);
	private final AvailableToPromiseAdapter availableToPromiseAdapter;

	public OrderLineQuickInputDescriptorFactory(
			@NonNull final AvailableToPromiseAdapter availableToPromiseAdapter)
	{
		this.availableToPromiseAdapter = availableToPromiseAdapter;
	}

	@Override
	public Set<MatchingKey> getMatchingKeys()
	{
		return ImmutableSet.of(MatchingKey.ofTableName(I_C_OrderLine.Table_Name));
	}

	@Override
	public QuickInputDescriptor createQuickInputDescriptor(
			final DocumentType documentType,
			final DocumentId documentTypeId,
			final DetailId detailId,
			@NonNull final Optional<SOTrx> soTrx)
	{
		final DocumentEntityDescriptor entityDescriptor = createEntityDescriptor(
				documentType,
				documentTypeId,
				detailId,
				soTrx);

		final QuickInputLayoutDescriptor layout = createLayout(entityDescriptor);

		return QuickInputDescriptor.of(
				entityDescriptor,
				layout,
				OrderLineQuickInputProcessor.class);
	}

	private DocumentEntityDescriptor createEntityDescriptor(
			final DocumentType documentType,
			final DocumentId documentTypeId,
			final DetailId detailId,
			@NonNull final Optional<SOTrx> soTrx)
	{
		return DocumentEntityDescriptor.builder()
				.setDocumentType(DocumentType.QuickInput, documentTypeId)
				.setIsSOTrx(soTrx)
				.disableDefaultTableCallouts()
				.setDetailId(detailId)
				.setTableName(I_C_OrderLine.Table_Name) // TODO: figure out if it's needed
				//
				.addField(createProductFieldBuilder(soTrx))
				.addFieldIf(QuickInputConstants.isEnablePackingInstructionsField(), () -> createPackingInstructionFieldBuilder())
				.addField(createQuantityFieldBuilder())
				.addFieldIf(QuickInputConstants.isEnableBestBeforePolicy(), () -> createBestBeforePolicyFieldBuilder())
				//
				.build();
	}

	private DocumentFieldDescriptor.Builder createProductFieldBuilder(@NonNull final Optional<SOTrx> soTrx)
	{
		final ProductLookupDescriptor productLookupDescriptor = createProductLookupDescriptor(soTrx);
		final ITranslatableString caption = msgBL.translatable(IOrderLineQuickInput.COLUMNNAME_M_Product_ID);

		return DocumentFieldDescriptor.builder(IOrderLineQuickInput.COLUMNNAME_M_Product_ID)
				.setLookupDescriptorProvider(productLookupDescriptor)
				.setCaption(caption)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.setReadonlyLogic(ConstantLogicExpression.FALSE)
				.setAlwaysUpdateable(true)
				.setMandatoryLogic(ConstantLogicExpression.TRUE)
				.setDisplayLogic(ConstantLogicExpression.TRUE)
				.addCallout(this::onProductChangedCallout)
				.addCharacteristic(Characteristic.PublicField);
	}

	private ProductLookupDescriptor createProductLookupDescriptor(@NonNull final Optional<SOTrx> soTrx)
	{
		if (soTrx.orElse(SOTrx.PURCHASE).isSales())
		{
			return ProductLookupDescriptor
					.builderWithStockInfo()
					.bpartnerParamName(I_C_Order.COLUMNNAME_C_BPartner_ID)
					.pricingDateParamName(I_C_Order.COLUMNNAME_DatePromised)
					.availableStockDateParamName(I_C_Order.COLUMNNAME_PreparationDate)
					.availableToPromiseAdapter(availableToPromiseAdapter)
					.build();
		}
		else
		{
			return ProductLookupDescriptor
					.builderWithStockInfo()
					.bpartnerParamName(I_C_Order.COLUMNNAME_C_BPartner_ID)
					.pricingDateParamName(I_C_Order.COLUMNNAME_DatePromised)
					.availableStockDateParamName(I_C_Order.COLUMNNAME_DatePromised)
					.availableToPromiseAdapter(availableToPromiseAdapter)
					.build();
		}
	}

	private void onProductChangedCallout(final ICalloutField calloutField)
	{
		final QuickInput quickInput = QuickInput.getQuickInputOrNull(calloutField);
		if (quickInput == null)
		{
			return;
		}

		final Document quickInputDocument = quickInput.getQuickInputDocument();
		if (quickInputDocument == null || !quickInputDocument.hasField(IOrderLineQuickInput.COLUMNNAME_M_HU_PI_Item_Product_ID))
		{
			return; // there are users whose systems don't have M_HU_PI_Item_Product_ID in their quick-input
		}

		final IOrderLineQuickInput quickInputModel = quickInput.getQuickInputDocumentAs(IOrderLineQuickInput.class);
		final LookupValue productLookupValue = quickInputModel.getM_Product_ID();
		if (productLookupValue == null)
		{
			return;
		}

		final ProductAndAttributes productAndAttributes = ProductLookupDescriptor.toProductAndAttributes(productLookupValue);
		final ProductId quickInputProductId = productAndAttributes.getProductId();

		final I_C_Order order = quickInput.getRootDocumentAs(I_C_Order.class);
		huOrderBL.findM_HU_PI_Item_Product(order, quickInputProductId, quickInputModel::setM_HU_PI_Item_Product);
	}

	private DocumentFieldDescriptor.Builder createPackingInstructionFieldBuilder()
	{
		return DocumentFieldDescriptor.builder(IOrderLineQuickInput.COLUMNNAME_M_HU_PI_Item_Product_ID)
				.setCaption(msgBL.translatable(IOrderLineQuickInput.COLUMNNAME_M_HU_PI_Item_Product_ID))
				//
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.setLookupDescriptorProvider(SqlLookupDescriptor.builder()
						.setCtxTableName(null) // ctxTableName
						.setCtxColumnName(IOrderLineQuickInput.COLUMNNAME_M_HU_PI_Item_Product_ID)
						.setDisplayType(DisplayType.TableDir)
						.setAD_Val_Rule_ID(540199) // FIXME: hardcoded "M_HU_PI_Item_Product_For_Org_and_Product_and_DateOrdered"
						.buildProvider())
				.setValueClass(IntegerLookupValue.class)
				.setReadonlyLogic(ConstantLogicExpression.FALSE)
				.setAlwaysUpdateable(true)
				.setMandatoryLogic(ConstantLogicExpression.FALSE)
				.setDisplayLogic(ConstantLogicExpression.TRUE)
				.addCharacteristic(Characteristic.PublicField);
	}

	private DocumentFieldDescriptor.Builder createQuantityFieldBuilder()
	{
		return DocumentFieldDescriptor.builder(IOrderLineQuickInput.COLUMNNAME_Qty)
				.setCaption(msgBL.translatable(IOrderLineQuickInput.COLUMNNAME_Qty))
				.setWidgetType(DocumentFieldWidgetType.Quantity)
				.setReadonlyLogic(ConstantLogicExpression.FALSE)
				.setAlwaysUpdateable(true)
				.setMandatoryLogic(ConstantLogicExpression.TRUE)
				.setDisplayLogic(ConstantLogicExpression.TRUE)
				.addCharacteristic(Characteristic.PublicField);
	}

	private DocumentFieldDescriptor.Builder createBestBeforePolicyFieldBuilder()
	{
		return DocumentFieldDescriptor.builder(IOrderLineQuickInput.COLUMNNAME_ShipmentAllocation_BestBefore_Policy)
				.setCaption(msgBL.translatable(IOrderLineQuickInput.COLUMNNAME_ShipmentAllocation_BestBefore_Policy))
				//
				.setWidgetType(DocumentFieldWidgetType.List)
				.setLookupDescriptorProvider(SqlLookupDescriptor.listByAD_Reference_Value_ID(ShipmentAllocationBestBeforePolicy.AD_REFERENCE_ID))
				.setValueClass(StringLookupValue.class)
				.setReadonlyLogic(ConstantLogicExpression.FALSE)
				.setAlwaysUpdateable(true)
				.setMandatoryLogic(ConstantLogicExpression.FALSE)
				.setDisplayLogic(ConstantLogicExpression.TRUE)
				.addCharacteristic(Characteristic.PublicField);
	}

	private static QuickInputLayoutDescriptor createLayout(final DocumentEntityDescriptor entityDescriptor)
	{
		return QuickInputLayoutDescriptor.build(entityDescriptor, new String[][] {
				{ "M_Product_ID", "M_HU_PI_Item_Product_ID" },
				{ "Qty" },
				{ "ShipmentAllocation_BestBefore_Policy" }
		});
	}
}
