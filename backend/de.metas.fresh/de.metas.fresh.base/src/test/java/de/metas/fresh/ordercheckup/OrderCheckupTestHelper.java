package de.metas.fresh.ordercheckup;

/*
 * #%L
 * de.metas.fresh.base
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.warehouse.model.I_M_Warehouse;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_S_Resource;
import org.compiere.model.X_S_Resource;
import org.compiere.util.Env;
import org.eevolution.model.I_PP_Product_Planning;
import org.eevolution.model.X_PP_Product_Planning;
import org.junit.Assert;

import de.metas.adempiere.model.I_M_Product;
import de.metas.fresh.model.I_C_Order_MFGWarehouse_Report;
import de.metas.fresh.model.I_C_Order_MFGWarehouse_ReportLine;
import de.metas.fresh.ordercheckup.printing.spi.impl.OrderCheckupPrintingQueueHandler;
import de.metas.printing.api.IPrintingQueueBL;
import de.metas.printing.model.I_AD_Archive;
import de.metas.printing.model.I_C_Printing_Queue;
import de.metas.printing.model.I_C_Printing_Queue_Recipient;
import de.metas.printing.model.validator.AD_Archive;

public class OrderCheckupTestHelper
{
	private Properties ctx;

	public OrderCheckupTestHelper()
	{
		super();
	}

	public void init()
	{
		AdempiereTestHelper.get().init();
		ctx = Env.getCtx();

		Services.get(IPrintingQueueBL.class).registerHandler(OrderCheckupPrintingQueueHandler.instance);
	}

	public Masterdata createMasterdata()
	{
		return new Masterdata(this);
	}

	public I_AD_User createAD_User(final String name)
	{
		final I_AD_User user = InterfaceWrapperHelper.create(ctx, I_AD_User.class, ITrx.TRXNAME_None);
		user.setName(name);
		InterfaceWrapperHelper.save(user);
		return user;
	}

	public I_S_Resource createPlant(final String name, final I_AD_User responsibleUser)
	{
		final I_S_Resource plant = InterfaceWrapperHelper.create(ctx, I_S_Resource.class, ITrx.TRXNAME_None);
		plant.setIsManufacturingResource(true);
		plant.setManufacturingResourceType(X_S_Resource.MANUFACTURINGRESOURCETYPE_Plant);
		plant.setValue(name);
		plant.setName(name);
		plant.setAD_User_ID(responsibleUser.getAD_User_ID());
		InterfaceWrapperHelper.save(plant);
		return plant;
	}

	public I_M_Warehouse createWarehouse(final String name, final I_AD_User responsibleUser, final I_S_Resource plant)
	{
		final I_M_Warehouse warehouse = InterfaceWrapperHelper.create(ctx, I_M_Warehouse.class, ITrx.TRXNAME_None);
		warehouse.setValue(name);
		warehouse.setName(name);
		warehouse.setAD_User(responsibleUser);
		warehouse.setPP_Plant(plant);
		InterfaceWrapperHelper.save(warehouse);
		return warehouse;
	}

	public I_M_Product createProduct(final String name, final I_M_Warehouse mfgWarehouse)
	{
		final I_M_Product product = InterfaceWrapperHelper.create(ctx, I_M_Product.class, ITrx.TRXNAME_None);
		product.setValue(name);
		product.setName(name);
		InterfaceWrapperHelper.save(product);

		createManufacturingProductPlanning(product, mfgWarehouse);

		return product;
	}

	public void createManufacturingProductPlanning(final I_M_Product product, final I_M_Warehouse warehouse)
	{
		final I_PP_Product_Planning productPlanning = InterfaceWrapperHelper.create(ctx, I_PP_Product_Planning.class, ITrx.TRXNAME_None);
		productPlanning.setM_Product(product);
		productPlanning.setM_Warehouse(warehouse);
		productPlanning.setAD_Org_ID(warehouse.getAD_Org_ID());
		productPlanning.setS_Resource(warehouse.getPP_Plant());
		productPlanning.setIsManufactured(X_PP_Product_Planning.ISMANUFACTURED_Yes);
		InterfaceWrapperHelper.save(productPlanning);
	}

	public I_C_Order createSalesOrder(final I_M_Warehouse warehouse)
	{
		final I_C_Order order = InterfaceWrapperHelper.create(ctx, I_C_Order.class, ITrx.TRXNAME_None);
		order.setIsSOTrx(true);
		order.setM_Warehouse(warehouse);
		InterfaceWrapperHelper.save(order);
		return order;
	}

	public I_C_OrderLine createOrderLine(final I_C_Order order, final I_M_Product product)
	{
		Check.assumeNotNull(order, "order not null");
		Check.assumeNotNull(product, "product not null");

		final I_C_OrderLine orderLine = InterfaceWrapperHelper.create(ctx, I_C_OrderLine.class, ITrx.TRXNAME_None);
		orderLine.setC_Order(order);
		orderLine.setM_Product(product);
		InterfaceWrapperHelper.save(orderLine);
		return orderLine;
	}

	public I_C_Order_MFGWarehouse_Report retrieveReport(final String documentType, final I_M_Warehouse warehouse, final I_S_Resource plant)
	{
		final Integer warehouseId = warehouse == null ? null : warehouse.getM_Warehouse_ID();
		final Integer plantId = plant == null ? null : plant.getS_Resource_ID();

		return Services.get(IQueryBL.class)
				.createQueryBuilder(I_C_Order_MFGWarehouse_Report.class, ctx, ITrx.TRXNAME_None)
				.addEqualsFilter(I_C_Order_MFGWarehouse_Report.COLUMN_DocumentType, documentType)
				.addEqualsFilter(I_C_Order_MFGWarehouse_Report.COLUMN_M_Warehouse_ID, warehouseId)
				.addEqualsFilter(I_C_Order_MFGWarehouse_Report.COLUMN_PP_Plant_ID, plantId)
				.create()
				.firstOnly(I_C_Order_MFGWarehouse_Report.class);
	}

	public void assertReportHasOrderLines(final I_C_Order_MFGWarehouse_Report report, final I_C_OrderLine... expectedOrderLines)
	{
		final Map<Integer, I_C_OrderLine> expectedOrderLinesMap = new HashMap<>();
		for (final I_C_OrderLine orderLine : expectedOrderLines)
		{
			expectedOrderLinesMap.put(orderLine.getC_OrderLine_ID(), orderLine);
		}

		final List<I_C_Order_MFGWarehouse_ReportLine> reportLines = Services.get(IOrderCheckupDAO.class).retrieveAllReportLines(report);
		for (final I_C_Order_MFGWarehouse_ReportLine reportLine : reportLines)
		{
			final I_C_OrderLine expectedOrderLine = expectedOrderLinesMap.remove(reportLine.getC_OrderLine_ID());
			Assert.assertNotNull("Unexpected report line: " + reportLine, expectedOrderLine);

			Assert.assertEquals("Product for " + reportLine, expectedOrderLine.getM_Product_ID(), reportLine.getM_Product_ID());
		}

		Assert.assertTrue("All expected order lines were found in report lines: " + expectedOrderLinesMap, expectedOrderLinesMap.isEmpty());
	}

	public void generateReportsAndEnqueueToPrinting(final I_C_Order order)
	{
		Services.get(IOrderCheckupBL.class).generateReportsIfEligible(order);
		enqueueToPrinting(order);
	}

	public void enqueueToPrinting(final I_C_Order order)
	{
		final List<I_C_Order_MFGWarehouse_Report> reports = Services.get(IOrderCheckupDAO.class).retrieveAllReports(order);

		for (final I_C_Order_MFGWarehouse_Report report : reports)
		{
			enqueueToPrinting(report);
		}
	}

	private void enqueueToPrinting(final I_C_Order_MFGWarehouse_Report report)
	{
		// Create the archive
		final I_AD_Archive archive = InterfaceWrapperHelper.newInstance(I_AD_Archive.class, report);
		archive.setAD_Table_ID(InterfaceWrapperHelper.getModelTableId(report));
		archive.setRecord_ID(report.getC_Order_MFGWarehouse_Report_ID());
		archive.setIsDirectEnqueue(true);
		InterfaceWrapperHelper.save(archive);

		// Enqueue to printing
		new AD_Archive().printArchive(archive);

		// Get the printing queue item and the recipient(s)
		final I_C_Printing_Queue printingItem = Services.get(IQueryBL.class)
				.createQueryBuilder(I_C_Printing_Queue.class, report)
				.addEqualsFilter(I_C_Printing_Queue.COLUMN_AD_Archive_ID, archive.getAD_Archive_ID())
				.create()
				.firstOnlyNotNull(I_C_Printing_Queue.class);
		final List<I_C_Printing_Queue_Recipient> printoutRecipients = Services.get(IQueryBL.class)
				.createQueryBuilder(I_C_Printing_Queue_Recipient.class, report)
				.addEqualsFilter(I_C_Printing_Queue.COLUMNNAME_C_Printing_Queue_ID, printingItem.getC_Printing_Queue_ID())
				.create()
				.list(I_C_Printing_Queue_Recipient.class);

		// Validate the printing queue item
		Assert.assertEquals("Printing queue item - PrintoutForOtherUser", true, printingItem.isPrintoutForOtherUser());
		Assert.assertEquals("Printing queue item - IsActive", true, printingItem.isActive());

		assertThat("Printout recipients - wrong number", printoutRecipients.size(), is(1));
		assertThat("Printout recipient - wrong AD_User_ToPrint_ID", printoutRecipients.get(0).getAD_User_ToPrint_ID(), is(report.getAD_User_Responsible_ID()));
	}

}
