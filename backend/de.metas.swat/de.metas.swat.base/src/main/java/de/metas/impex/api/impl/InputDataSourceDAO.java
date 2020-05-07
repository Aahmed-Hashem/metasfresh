package de.metas.impex.api.impl;

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


import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.proxy.Cached;
import org.compiere.model.Query;

import de.metas.adempiere.util.CacheCtx;
import de.metas.adempiere.util.CacheTrx;
import de.metas.impex.api.IInputDataSourceDAO;
import de.metas.impex.model.I_AD_InputDataSource;

public class InputDataSourceDAO implements IInputDataSourceDAO
{

	@Override
	public I_AD_InputDataSource retrieveInputDataSource(
			final Properties ctx,
			final String internalName,
			final boolean throwEx,
			final String trxName)
	{
		final I_AD_InputDataSource result = retriveDataSource(ctx, internalName, trxName);

		if (result == null && throwEx)
		{
			throw new AdempiereException("missing data source for internal name " + internalName);
		}

		return result;

	}

	@Cached(cacheName = I_AD_InputDataSource.Table_Name)
	/* package */ I_AD_InputDataSource retriveDataSource(
			final @CacheCtx Properties ctx,
			final String internalName,
			final @CacheTrx String trxName)
	{
		final String whereClause = I_AD_InputDataSource.COLUMNNAME_InternalName + " = ?";

		final I_AD_InputDataSource result = new Query(ctx, I_AD_InputDataSource.Table_Name, whereClause, trxName)
				.setParameters(internalName)
				.setApplyAccessFilter(true)
				.setOnlyActiveRecords(true)
				.firstOnly(I_AD_InputDataSource.class);
		return result;
	}

}
