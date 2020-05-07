package de.metas.handlingunits.material.interceptor;

import static org.adempiere.model.InterfaceWrapperHelper.load;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.compiere.model.I_M_InOutLine;
import org.compiere.model.I_M_InventoryLine;
import org.compiere.model.I_M_MovementLine;
import org.eevolution.model.I_PP_Cost_Collector;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import de.metas.handlingunits.model.I_M_ShipmentSchedule_QtyPicked;
import de.metas.handlingunits.movement.api.IHUMovementBL;
import de.metas.material.event.MaterialEvent;
import de.metas.material.event.commons.HUOnHandQtyChangeDescriptor;
import de.metas.material.event.commons.MaterialDescriptor;
import de.metas.material.event.transactions.AbstractTransactionEvent;
import de.metas.material.event.transactions.TransactionCreatedEvent;
import de.metas.material.event.transactions.TransactionDeletedEvent;
import de.metas.materialtransaction.MTransactionUtil;
import lombok.NonNull;

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

public class M_Transaction_TransactionEventCreator
{
	public static final M_Transaction_TransactionEventCreator INSTANCE = new M_Transaction_TransactionEventCreator();

	private M_Transaction_TransactionEventCreator()
	{
	}

	public final List<MaterialEvent> createEventsForTransaction(
			@NonNull final TransactionDescriptor transaction,
			final boolean deleted)
	{
		final Builder<MaterialEvent> result = ImmutableList.builder();

		if (transaction.getInoutLineId() > 0)
		{
			result.add(createEventForInOutLine(transaction, deleted));
		}
		else if (transaction.getCostCollectorId() > 0)
		{
			result.add(createEventForCostCollector(transaction, deleted));
		}
		else if (transaction.getMovementLineId() > 0)
		{
			result.add(createEventForMovementLine(transaction, deleted));
		}
		else if (transaction.getInventoryLineId() > 0)
		{
			result.add(createEventForInventoryLine(transaction, deleted));
		}
		return result.build();
	}

	private static boolean isDirectMovementWarehouse(final int warehouseId)
	{
		final int intValue = Services.get(ISysConfigBL.class).getIntValue(IHUMovementBL.SYSCONFIG_DirectMove_Warehouse_ID, -1);
		return intValue == warehouseId;
	}

	private MaterialEvent createEventForCostCollector(
			@NonNull final TransactionDescriptor transaction,
			final boolean deleted)
	{
		final MaterialDescriptor materialDescriptor = createMaterialDescriptor(
				transaction,
				transaction.getMovementQty());

		final boolean directMovementWarehouse = isDirectMovementWarehouse(transaction.getWarehouseId());

		final AbstractTransactionEvent event;

		final I_PP_Cost_Collector costCollector = load(transaction.getCostCollectorId(), I_PP_Cost_Collector.class);

		final List<HUOnHandQtyChangeDescriptor> huDescriptors = //
				M_Transaction_HuOnHandQtyChangeDescriptor.INSTANCE.createHuDescriptorsForCostCollector(costCollector, deleted);

		if (deleted)
		{
			event = TransactionDeletedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.directMovementWarehouse(directMovementWarehouse)
					.ppOrderId(costCollector.getPP_Order_ID())
					.ppOrderLineId(costCollector.getPP_Order_BOMLine_ID())
					.huOnHandQtyChangeDescriptors(huDescriptors)
					.build();
		}
		else
		{
			event = TransactionCreatedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.directMovementWarehouse(directMovementWarehouse)
					.ppOrderId(costCollector.getPP_Order_ID())
					.ppOrderLineId(costCollector.getPP_Order_BOMLine_ID())
					.huOnHandQtyChangeDescriptors(huDescriptors)
					.build();
		}
		return event;
	}

	private static MaterialDescriptor createMaterialDescriptor(
			@NonNull final TransactionDescriptor transaction,
			@NonNull final BigDecimal quantity)
	{//TODO move down
		return MaterialDescriptor.builder()
				.warehouseId(transaction.getWarehouseId())
				.date(transaction.getMovementDate())
				.productDescriptor(transaction.getProductDescriptor())
				.bPartnerId(transaction.getBPartnerId())
				.quantity(quantity)
				.build();
	}

	private MaterialEvent createEventForInOutLine(
			@NonNull final TransactionDescriptor transaction,
			final boolean deleted)
	{
		final Map<Integer, BigDecimal> shipmentScheduleIds2Qtys = retrieveShipmentScheduleId2Qty(transaction);

		final AbstractTransactionEvent event = createEventForShipmentScheduleToQtyMapping(transaction, shipmentScheduleIds2Qtys, deleted);
		return event;
	}

	@VisibleForTesting
	static Map<Integer, BigDecimal> retrieveShipmentScheduleId2Qty(
			@NonNull final TransactionDescriptor transaction)
	{
		final Map<Integer, BigDecimal> shipmentScheduleId2quantity = new TreeMap<>();

		BigDecimal qtyLeftToDistribute = transaction.getMovementQty();

		final List<I_M_ShipmentSchedule_QtyPicked> shipmentScheduleQtysPicked = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_ShipmentSchedule_QtyPicked.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_ShipmentSchedule_QtyPicked.COLUMNNAME_M_InOutLine_ID, transaction.getInoutLineId())
				.create()
				.list();

		for (final I_M_ShipmentSchedule_QtyPicked shipmentScheduleQtyPicked : shipmentScheduleQtysPicked)
		{
			assertSignumsOfQuantitiesMatch(shipmentScheduleQtyPicked, transaction);

			final BigDecimal qtyPicked = shipmentScheduleQtyPicked.getQtyPicked();
			final BigDecimal quantityForMaterialDescriptor = MTransactionUtil.isInboundMovementType(transaction.getMovementType())
					? qtyPicked
					: qtyPicked.negate();

			shipmentScheduleId2quantity.merge(
					shipmentScheduleQtyPicked.getM_ShipmentSchedule_ID(),
					quantityForMaterialDescriptor,
					BigDecimal::add);

			qtyLeftToDistribute = qtyLeftToDistribute.subtract(quantityForMaterialDescriptor);
		}

		return shipmentScheduleId2quantity;
	}

	private static void assertSignumsOfQuantitiesMatch(
			@NonNull final I_M_ShipmentSchedule_QtyPicked shipmentScheduleQtyPicked,
			@NonNull final TransactionDescriptor transaction)
	{
		final BigDecimal qtyPicked = shipmentScheduleQtyPicked.getQtyPicked();
		final BigDecimal movementQty = transaction.getMovementQty();

		if (qtyPicked.signum() == 0 || movementQty.signum() == 0)
		{
			return; // at least one of them is zero
		}
		if (qtyPicked.signum() != movementQty.signum())
		{
			return;
		}

		throw new AdempiereException(
				"For the given shipmentScheduleQtyPicked and transaction, one needs to be positive and one needs to be negative")
						.appendParametersToMessage()
						.setParameter("qtyPicked", qtyPicked)
						.setParameter("movementQty", movementQty)
						.setParameter("shipmentScheduleQtyPicked", shipmentScheduleQtyPicked)
						.setParameter("transaction", transaction);
	}

	private static AbstractTransactionEvent createEventForShipmentScheduleToQtyMapping(
			@NonNull final TransactionDescriptor transaction,
			@NonNull final Map<Integer, BigDecimal> shipmentScheduleIds2Qtys,
			final boolean deleted)
	{
		final boolean directMovementWarehouse = isDirectMovementWarehouse(transaction.getWarehouseId());

		final MaterialDescriptor materialDescriptor = createMaterialDescriptor(
				transaction,
				transaction.getMovementQty());

		final I_M_InOutLine inOutLine = load(transaction.getInoutLineId(), I_M_InOutLine.class);

		final List<HUOnHandQtyChangeDescriptor> huDescriptor = //
				M_Transaction_HuOnHandQtyChangeDescriptor.INSTANCE.createHuDescriptorsForInOutLine(inOutLine, deleted);

		final AbstractTransactionEvent event;
		if (deleted)
		{
			event = TransactionDeletedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.shipmentScheduleIds2Qtys(shipmentScheduleIds2Qtys)
					.directMovementWarehouse(directMovementWarehouse)
					.huOnHandQtyChangeDescriptors(huDescriptor)
					.build();
		}
		else
		{
			event = TransactionCreatedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.shipmentScheduleIds2Qtys(shipmentScheduleIds2Qtys)
					.directMovementWarehouse(directMovementWarehouse)
					.huOnHandQtyChangeDescriptors(huDescriptor)
					.build();
		}
		return event;
	}

	private MaterialEvent createEventForMovementLine(
			@NonNull final TransactionDescriptor transaction,
			final boolean deleted)
	{
		final boolean directMovementWarehouse = isDirectMovementWarehouse(transaction.getWarehouseId());

		final MaterialDescriptor materialDescriptor = createMaterialDescriptor(
				transaction,
				transaction.getMovementQty());

		final AbstractTransactionEvent event;
		final I_M_MovementLine movementLine = load(transaction.getMovementLineId(), I_M_MovementLine.class);

		final int ddOrderId = movementLine.getDD_OrderLine_ID() > 0
				? movementLine.getDD_OrderLine().getDD_Order_ID()
				: 0;

		final List<HUOnHandQtyChangeDescriptor> huDescriptors = //
				M_Transaction_HuOnHandQtyChangeDescriptor.INSTANCE.createHuDescriptorsForMovementLine(movementLine, deleted);

		if (deleted)
		{
			event = TransactionDeletedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.directMovementWarehouse(directMovementWarehouse)
					.ddOrderId(ddOrderId)
					.ddOrderLineId(movementLine.getDD_OrderLine_ID())
					.huOnHandQtyChangeDescriptors(huDescriptors)
					.build();
		}
		else
		{
			event = TransactionCreatedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.directMovementWarehouse(directMovementWarehouse)
					.ddOrderId(ddOrderId)
					.ddOrderLineId(movementLine.getDD_OrderLine_ID())
					.huOnHandQtyChangeDescriptors(huDescriptors)
					.build();
		}

		return event;
	}

	private MaterialEvent createEventForInventoryLine(
			@NonNull final TransactionDescriptor transaction,
			final boolean deleted)
	{
		final boolean directMovementWarehouse = isDirectMovementWarehouse(transaction.getWarehouseId());

		final MaterialDescriptor materialDescriptor = createMaterialDescriptor(
				transaction,
				transaction.getMovementQty());

		final I_M_InventoryLine inventoryLine = load(transaction.getInventoryLineId(), I_M_InventoryLine.class);

		final List<HUOnHandQtyChangeDescriptor> huDescriptors = //
				M_Transaction_HuOnHandQtyChangeDescriptor.INSTANCE.createHuDescriptorsForInventoryLine(inventoryLine, deleted);

		final AbstractTransactionEvent event;

		if (deleted)
		{
			event = TransactionDeletedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.directMovementWarehouse(directMovementWarehouse)
					.huOnHandQtyChangeDescriptors(huDescriptors)
					.build();
		}
		else
		{
			event = TransactionCreatedEvent.builder()
					.eventDescriptor(transaction.getEventDescriptor())
					.transactionId(transaction.getTransactionId())
					.materialDescriptor(materialDescriptor)
					.directMovementWarehouse(directMovementWarehouse)
					.huOnHandQtyChangeDescriptors(huDescriptors)
					.build();
		}

		return event;
	}
}
