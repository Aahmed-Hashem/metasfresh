package de.metas.handlingunits.inventory;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;

import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.warehouse.LocatorId;
import org.adempiere.warehouse.WarehouseId;
import org.compiere.model.I_C_UOM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.metas.handlingunits.HuId;
import de.metas.inventory.InventoryId;
import de.metas.inventory.InventoryLineId;
import de.metas.material.event.commons.AttributesKey;
import de.metas.product.ProductId;
import de.metas.quantity.Quantity;

/*
 * #%L
 * de.metas.handlingunits.base
 * %%
 * Copyright (C) 2019 metas GmbH
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

class InventoryLineTest
{
	private static final BigDecimal TWO = new BigDecimal("2");
	private static final BigDecimal SEVENTEEN = new BigDecimal("17");

	private static final BigDecimal NINETEEN = new BigDecimal("19");

	private static final BigDecimal TWENTY = new BigDecimal("20");

	private I_C_UOM uomRecord;

	private Quantity qtyZero;
	private Quantity qtyOne;
	private Quantity qtyTwo;
	private Quantity qtyTen;
	private Quantity qtySeventeen;
	private Quantity qtyNineTeen;
	private Quantity qtyTwenty;

	@BeforeEach
	public void beforeEach()
	{
		AdempiereTestHelper.get().init();

		uomRecord = newInstance(I_C_UOM.class);
		saveRecord(uomRecord);

		qtyZero = Quantity.of(ZERO, uomRecord);
		qtyOne = Quantity.of(ONE, uomRecord);
		qtyTwo = Quantity.of(TWO, uomRecord);
		qtyTen = Quantity.of(TEN, uomRecord);
		qtySeventeen = Quantity.of(SEVENTEEN, uomRecord);
		qtyNineTeen = Quantity.of(NINETEEN, uomRecord);
		qtyTwenty = Quantity.of(TWENTY, uomRecord);
	}

	@Test
	void withQtyCount_3_plus_19()
	{

		final InventoryLine inventoryLine = createInventoryLine();
		assertThat(inventoryLine.getInventoryLineHUs()) // guard
				.extracting("huId", "qtyBook", "qtyCount")
				.containsOnly(
						tuple(HuId.ofRepoId(100), qtyTen, qtyOne),
						tuple(HuId.ofRepoId(200), qtyTwenty, qtyTwo));

		// invoke the method under test
		final InventoryLine result = inventoryLine.withQtyCount(qtyNineTeen);

		assertThat(result.getInventoryLineHUs())
				.extracting("huId", "qtyBook", "qtyCount")
				.containsOnly(
						tuple(HuId.ofRepoId(100), qtyTen, qtySeventeen),
						tuple(HuId.ofRepoId(200), qtyTwenty, qtyTwo));
	}

	@Test
	void withQtyCount_3_minus_2()
	{

		final InventoryLine inventoryLine = createInventoryLine();
		assertThat(inventoryLine.getInventoryLineHUs()) // guard
				.extracting("huId", "qtyBook", "qtyCount")
				.containsOnly(
						tuple(HuId.ofRepoId(100), qtyTen, qtyOne),
						tuple(HuId.ofRepoId(200), qtyTwenty, qtyTwo));

		// invoke the method under test
		final InventoryLine result = inventoryLine.withQtyCount(qtyOne);

		assertThat(result.getInventoryLineHUs())
				.extracting("huId", "qtyBook", "qtyCount")
				.containsOnly(
						tuple(HuId.ofRepoId(100), qtyTen, qtyZero),
						tuple(HuId.ofRepoId(200), qtyTwenty, qtyOne));
	}

	@Test
	void withQtyCount_empty_plus_19()
	{
		final InventoryLine inventoryLine = createInventoryLine().toBuilder().clearInventoryLineHUs().build();
		assertThat(inventoryLine.getInventoryLineHUs()).isEmpty();

		// invoke the method under test
		final InventoryLine result = inventoryLine.withQtyCount(qtyNineTeen);

		assertThat(result.getInventoryLineHUs())
				.extracting("huId", "qtyBook", "qtyCount")
				.containsOnly(tuple(null, qtyZero, qtyNineTeen));
	}

	private InventoryLine createInventoryLine()
	{
		final InventoryLine inventoryLine = InventoryLine
				.builder()
				.inventoryId(InventoryId.ofRepoId(10))
				.id(InventoryLineId.ofRepoId(20))
				.locatorId(LocatorId.ofRepoId(WarehouseId.ofRepoId(30), 35))
				.productId(ProductId.ofRepoId(40))
				.storageAttributesKey(AttributesKey.ofAttributeValueIds(10000, 10001, 10002))
				.singleHUAggregation(false)
				.inventoryLineHU(InventoryLineHU
						.builder()
						.huId(HuId.ofRepoId(100))
						.qtyBook(qtyTen)
						.qtyCount(qtyOne)
						.build())
				.inventoryLineHU(InventoryLineHU
						.builder()
						.huId(HuId.ofRepoId(200))
						.qtyBook(qtyTwenty)
						.qtyCount(qtyTwo)
						.build())
				.build();
		return inventoryLine;
	}

}
