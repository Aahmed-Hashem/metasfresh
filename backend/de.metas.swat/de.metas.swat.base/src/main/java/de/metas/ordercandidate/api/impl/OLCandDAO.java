package de.metas.ordercandidate.api.impl;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.List;
import java.util.Properties;

import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

import de.metas.interfaces.I_C_OrderLine;
import de.metas.ordercandidate.model.I_C_OLCand;
import de.metas.ordercandidate.model.I_C_OLCandAggAndOrder;
import de.metas.ordercandidate.model.I_C_OLCandGenerator;
import de.metas.ordercandidate.model.I_C_OLCandProcessor;
import de.metas.ordercandidate.model.I_C_Order_Line_Alloc;

public class OLCandDAO extends AbstractOLCandDAO
{
	@Override
	public List<I_C_OLCand> retrieveReferencing(final Properties ctx, final String tableName, final int recordId, final String trxName)
	{
		Check.assume(!Check.isEmpty(tableName), "Param 'tableName' is not empty");
		Check.assume(recordId > 0, "Param 'recordId' is > 0");

		final String whereClause = I_C_OLCand.COLUMNNAME_AD_Table_ID + "=? AND " + I_C_OLCand.COLUMNNAME_Record_ID + "=?";
		final int tableID = Services.get(IADTableDAO.class).retrieveTableId(tableName);

		return new Query(ctx, I_C_OLCand.Table_Name, whereClause, trxName)
				.setParameters(tableID, recordId)
				.setOnlyActiveRecords(true)
				.setClient_ID()
				.setOrderBy(I_C_OLCand.COLUMNNAME_C_OLCand_ID)
				.list(I_C_OLCand.class);
	}

	@Override
	public <T extends I_C_OLCand> List<T> retrieveOLCands(
			final I_C_OrderLine ol,
			final Class<T> clazz)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(ol);
		final String trxName = InterfaceWrapperHelper.getTrxName(ol);

		final String wc =
				I_C_OLCand.COLUMNNAME_C_OLCand_ID + " IN (" +
						"  select " + I_C_Order_Line_Alloc.COLUMNNAME_C_OLCand_ID +
						"  from " + I_C_Order_Line_Alloc.Table_Name +
						"  where " + I_C_Order_Line_Alloc.COLUMNNAME_C_OrderLine_ID + "=? AND " +
						"     " + I_C_Order_Line_Alloc.COLUMNNAME_IsActive + "=" + DB.TO_STRING("Y") +
						")";

		final List<T> result =
				new Query(ctx, I_C_OLCand.Table_Name, wc, trxName)
						.setParameters(ol.getC_OrderLine_ID())
						.setOnlyActiveRecords(true)
						.setClient_ID()
						.setOrderBy(I_C_OLCand.COLUMNNAME_C_OLCand_ID)
						.list(clazz);

		return result;
	}

	@Override
	public List<I_C_Order_Line_Alloc> retrieveAllOlas(final I_C_OrderLine ol)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(ol);
		final String trxName = InterfaceWrapperHelper.getTrxName(ol);

		final String wc = I_C_Order_Line_Alloc.COLUMNNAME_C_OrderLine_ID + "=?";

		return new Query(ctx, I_C_Order_Line_Alloc.Table_Name, wc, trxName)
				.setParameters(ol.getC_OrderLine_ID())
				.setOnlyActiveRecords(false) // note that we also load records that are inactive or belong to
												// different AD_Clients
				.setOrderBy(I_C_Order_Line_Alloc.COLUMNNAME_C_Order_Line_Alloc_ID)
				.list(I_C_Order_Line_Alloc.class);
	}

	@Override
	public List<I_C_Order_Line_Alloc> retrieveAllOlas(final I_C_OLCand olCand)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(olCand);
		final String trxName = InterfaceWrapperHelper.getTrxName(olCand);

		final String wc = I_C_Order_Line_Alloc.COLUMNNAME_C_OLCand_ID + "=?";

		return new Query(ctx, I_C_Order_Line_Alloc.Table_Name, wc, trxName)
				.setParameters(olCand.getC_OLCand_ID())
				.setOnlyActiveRecords(false) // note that we also load records that are inactive or belong to
												// different AD_Clients
				.setOrderBy(I_C_Order_Line_Alloc.COLUMNNAME_C_Order_Line_Alloc_ID)
				.list(I_C_Order_Line_Alloc.class);
	}

	@Override
	public List<I_C_OLCandGenerator> retrieveforTable(final Properties ctx, final int adTableId, final String trxName)
	{
		return new Query(ctx, I_C_OLCandGenerator.Table_Name, I_C_OLCandGenerator.COLUMNNAME_AD_Table_Source_ID + "=?", trxName)
				.setParameters(adTableId)
				.setOnlyActiveRecords(true)
				.setClient_ID()
				.setOrderBy(I_C_OLCandGenerator.COLUMNNAME_C_OLCandGenerator_ID)
				.list(I_C_OLCandGenerator.class);
	}

	@Override
	public I_C_OLCandGenerator retrieveOlCandCreator(final Properties ctx, final int tableId, final String trxName)
	{
		final I_C_OLCandGenerator olCandGenerator = new Query(ctx, I_C_OLCandGenerator.Table_Name, I_C_OLCandGenerator.COLUMNNAME_AD_Table_Source_ID + "=?", trxName)
				.setParameters(tableId)
				.setOnlyActiveRecords(true)
				.firstOnly(I_C_OLCandGenerator.class);
		return olCandGenerator;
	}

	@Override
	public List<I_C_OLCandGenerator> retrieveOlCandCreatorForOrg(final Properties ctx, final int adOrgId)
	{
		final List<I_C_OLCandGenerator> creators =
				new Query(Env.getCtx(), I_C_OLCandGenerator.Table_Name, I_C_OLCandGenerator.COLUMNNAME_AD_Org_ID + "=?", null)
						.setParameters(adOrgId)
						.setOnlyActiveRecords(true)
						// Note: actually, the order doesn't matter as long as we don't support more than one implementation per table
						.setOrderBy(I_C_OLCandGenerator.COLUMNNAME_C_OLCandGenerator_ID)
						.list(I_C_OLCandGenerator.class);
		return creators;
	}

	@Override
	public List<I_C_OLCandAggAndOrder> retrieveOLCandAggAndOrderForProcessor(final Properties ctx, final I_C_OLCandProcessor processor, final String trxName)
	{
		return new Query(ctx, I_C_OLCandAggAndOrder.Table_Name, I_C_OLCandAggAndOrder.COLUMNNAME_C_OLCandProcessor_ID + "=?", trxName)
				.setParameters(processor.getC_OLCandProcessor_ID())
				.setOnlyActiveRecords(true)
				.setClient_ID()
				.setOrderBy(I_C_OLCandAggAndOrder.COLUMNNAME_OrderBySeqNo)
				.list(I_C_OLCandAggAndOrder.class);
	}
}
