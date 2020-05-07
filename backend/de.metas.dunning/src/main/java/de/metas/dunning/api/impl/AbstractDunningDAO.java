package de.metas.dunning.api.impl;

/*
 * #%L
 * de.metas.dunning
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.proxy.Cached;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.MTable;

import de.metas.adempiere.util.CacheCtx;
import de.metas.adempiere.util.CacheTrx;
import de.metas.dunning.api.IDunningCandidateQuery;
import de.metas.dunning.api.IDunningCandidateQuery.ApplyAccessFilter;
import de.metas.dunning.api.IDunningContext;
import de.metas.dunning.api.IDunningDAO;
import de.metas.dunning.exception.DunningException;
import de.metas.dunning.interfaces.I_C_Dunning;
import de.metas.dunning.interfaces.I_C_DunningLevel;
import de.metas.dunning.model.I_C_Dunning_Candidate;

public abstract class AbstractDunningDAO implements IDunningDAO
{
	@Override
	public final I_C_Dunning retrieveDunningForBPartner(final I_C_BPartner bpartner)
	{
		I_C_Dunning dunning = InterfaceWrapperHelper.create(bpartner.getC_Dunning(), I_C_Dunning.class);
		if (dunning != null)
		{
			return dunning;
		}

		dunning = InterfaceWrapperHelper.create(bpartner.getC_BP_Group().getC_Dunning(), I_C_Dunning.class);
		return dunning;
	}

	@Override
	public final I_C_Dunning retrieveDunningByOrg(final Properties ctx, final int adOrgId)
	{
		Check.assume(adOrgId >= 0, "adOrgId >= 0");

		final List<I_C_Dunning> result = new ArrayList<I_C_Dunning>();
		final List<I_C_Dunning> dunnings = retrieveDunnings(ctx);
		for (final I_C_Dunning dunning : dunnings)
		{
			if (!dunning.isActive())
			{
				continue;
			}

			if (dunning.getAD_Org_ID() != adOrgId)
			{
				continue;
			}

			if (!dunning.isDefault())
			{
				continue;
			}

			result.add(dunning);
		}

		if (result.isEmpty())
		{
			return null;
		}
		else if (result.size() > 1)
		{
			throw new DunningException("More then one dunning found: " + result);
		}

		return result.get(0);
	}

	@Override
	public final I_C_Dunning_Candidate retrieveDunningCandidate(IDunningContext context, int tableId, int recordId, I_C_DunningLevel dunningLevel)
	{
		final DunningCandidateQuery query = new DunningCandidateQuery();
		query.setAD_Table_ID(tableId);
		query.setRecord_ID(recordId);
		query.setC_DunningLevels(Collections.singletonList(dunningLevel));
		
		// 04766 this method is also called from the server side, so for now the check for AD_Client_ID must suffice
		query.setApplyClientSecurity(true);
		query.setApplyAccessFilter(ApplyAccessFilter.ACCESS_FILTER_NONE);   

		return retrieveDunningCandidate(context, query);
	}

	@Override
	public final I_C_Dunning_Candidate retrieveDunningCandidate(IDunningContext context, Object model, I_C_DunningLevel dunningLevel)
	{
		final String tableName = InterfaceWrapperHelper.getModelTableName(model);
		final int tableId = MTable.getTable_ID(tableName);
		final int recordId = InterfaceWrapperHelper.getId(model);
		return retrieveDunningCandidate(context, tableId, recordId, dunningLevel);
	}

	@Override
	public final List<I_C_Dunning_Candidate> retrieveDunningCandidates(IDunningContext context, int tableId, int recordId)
	{
		final List<I_C_DunningLevel> dunningLevels = null; // don't filter by dunning levels
		return retrieveDunningCandidates(context, tableId, recordId, dunningLevels);
	}

	@Override
	public final List<I_C_Dunning_Candidate> retrieveDunningCandidates(IDunningContext context, int tableId, int recordId, List<I_C_DunningLevel> dunningLevels)
	{
		final DunningCandidateQuery query = new DunningCandidateQuery();
		query.setAD_Table_ID(tableId);
		query.setRecord_ID(recordId);
		query.setC_DunningLevels(dunningLevels);
		query.setApplyClientSecurity(false); // we need to return all candidates for given table/record

		return retrieveDunningCandidates(context, query);
	}

	@Override
	public final Iterator<I_C_Dunning_Candidate> retrieveNotProcessedCandidatesIterator(final IDunningContext dunningContext)
	{
		final DunningCandidateQuery query = new DunningCandidateQuery();
		query.setProcessed(false);
		query.setApplyClientSecurity(true);
		query.setActive(true);

		return retrieveDunningCandidatesIterator(dunningContext, query);
	}

	@Override
	public final Iterator<I_C_Dunning_Candidate> retrieveNotProcessedCandidatesIteratorRW(final IDunningContext dunningContext, final String additionalWhere)
	{
		final DunningCandidateQuery query = new DunningCandidateQuery();
		query.setProcessed(false);
		query.setApplyClientSecurity(true);
		query.setActive(true);
		query.setAdditionalWhere(additionalWhere);
		query.setApplyAccessFilter(ApplyAccessFilter.ACCESS_FILTER_RW);
		
		return retrieveDunningCandidatesIterator(dunningContext, query);
	}

	
	@Override
	public final Iterator<I_C_Dunning_Candidate> retrieveNotProcessedCandidatesIteratorByLevel(IDunningContext dunningContext, final I_C_DunningLevel dunningLevel)
	{
		Check.assumeNotNull(dunningLevel, "dunningLevel not null");

		final DunningCandidateQuery query = new DunningCandidateQuery();
		query.setProcessed(false);
		query.setActive(true);
		query.setApplyClientSecurity(true);
		query.setC_DunningLevels(Collections.singletonList(dunningLevel));

		return retrieveDunningCandidatesIterator(dunningContext, query);
	}

	@Override
	public final List<I_C_DunningLevel> retrieveDunningLevels(final I_C_Dunning dunning)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(dunning);
		final String trxName = InterfaceWrapperHelper.getTrxName(dunning);
		final int dunningId = dunning.getC_Dunning_ID();
		return retrieveDunningLevels(ctx, dunningId, trxName);
	}

	@Cached(cacheName = I_C_DunningLevel.Table_Name + "_for_C_Dunning_ID")
	/* package */ List<I_C_DunningLevel> retrieveDunningLevels(@CacheCtx Properties ctx, int dunningId, @CacheTrx String trxName)
	{
		return Services.get(IQueryBL.class).createQueryBuilder(I_C_DunningLevel.class, ctx, trxName)
		.addEqualsFilter( I_C_DunningLevel.COLUMNNAME_C_Dunning_ID, dunningId)
		.addOnlyActiveRecordsFilter()
		.orderBy()
			.addColumn(I_C_DunningLevel.COLUMNNAME_DaysAfterDue)
			.addColumn(I_C_DunningLevel.COLUMNNAME_DaysBetweenDunning)
			.addColumn(I_C_DunningLevel.COLUMNNAME_C_DunningLevel_ID)
			.endOrderBy()
			.create()
			.list();
	}
	
	protected abstract List<I_C_Dunning_Candidate> retrieveDunningCandidates(IDunningContext context, IDunningCandidateQuery query);

	protected abstract I_C_Dunning_Candidate retrieveDunningCandidate(IDunningContext context, IDunningCandidateQuery query);

	protected abstract Iterator<I_C_Dunning_Candidate> retrieveDunningCandidatesIterator(IDunningContext context, IDunningCandidateQuery query);
}
