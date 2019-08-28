package de.metas.impexp.processing.inventory;

import java.util.List;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.warehouse.LocatorId;
import org.adempiere.warehouse.WarehouseId;
import org.adempiere.warehouse.api.IWarehouseDAO;
import org.compiere.model.I_I_Inventory;
import org.compiere.model.I_M_Locator;
import org.compiere.util.DB;
import org.slf4j.Logger;

import de.metas.logging.LogManager;
import de.metas.util.Services;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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

/**
 * A helper class for {@link InventoryImportProcess} that performs the "dirty" but efficient SQL updates on the {@link I_I_Inventory} table.
 * Those updates complements the data from existing metasfresh records and flag those import records that can't yet be imported.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@UtilityClass
final class MInventoryImportTableSqlUpdater
{
	private static final transient Logger logger = LogManager.getLogger(MInventoryImportTableSqlUpdater.class);

	public void updateInventoryImportTable(@NonNull final String whereClause)
	{
		dbUpdateLocatorDimensions(whereClause);
		dbUpdateWarehouse(whereClause);
		dbUpdateCreateLocators(whereClause);
		dbUpdateProducts(whereClause);
		dbUpdateSubProducer(whereClause);

		dbUpdateErrorMessages(whereClause);
	}

	private void dbUpdateLocatorDimensions(@NonNull final String whereClause)
	{
		// Set M_Warehouse_ID
		StringBuilder sql = new StringBuilder("UPDATE I_Inventory i ")
				.append("SET warehouseValue = dimensions.warehouseValue, X = dimensions.locatorX , Y = dimensions.locatorY, Z = dimensions.locatorZ, X1 = dimensions.locatorX1 ")
				.append("FROM (SELECT d.warehouseValue, d.locatorValue, d.locatorX, d.locatorY, d.locatorZ, d.locatorX1 ")
				.append("	FROM I_Inventory as inv")
				.append("	JOIN extractLocatorDimensions(inv.locatorvalue) as d on d.locatorvalue=inv.locatorvalue")
				.append(") AS dimensions ")
				.append("WHERE I_IsImported<>'Y' AND dimensions.locatorvalue = i.locatorvalue ")
				.append(whereClause);
		DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
	}

	private void dbUpdateWarehouse(@NonNull final String whereClause)
	{
		// Set M_Warehouse_ID
		final StringBuilder sql = new StringBuilder("UPDATE I_Inventory i ")
				.append("SET M_Warehouse_ID=(SELECT M_Warehouse_ID FROM M_Warehouse w WHERE i.WarehouseValue=w.Value) ")
				.append("WHERE M_Warehouse_ID IS NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
	}

	private void dbUpdateCreateLocators(@NonNull final String whereClause)
	{
		dbUpdateLocators(whereClause);
		dbCreateLocators();
	}

	private void dbUpdateLocators(@NonNull final String whereClause)
	{
		StringBuilder sql = new StringBuilder("UPDATE I_Inventory i ")
				.append("SET M_Locator_ID=(SELECT MAX(M_Locator_ID) FROM M_Locator l ")
				.append("WHERE i.LocatorValue=l.Value AND i.M_Warehouse_ID = l.M_Warehouse_ID AND i.AD_Client_ID=l.AD_Client_ID) ")
				.append("WHERE M_Locator_ID IS NULL AND LocatorValue IS NOT NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		//
		// update DateLastInventory
		sql = new StringBuilder("UPDATE M_Locator l ")
				.append("SET DateLastInventory=(SELECT DateLastInventory FROM I_Inventory i ")
				.append("WHERE i.LocatorValue=l.Value AND i.AD_Client_ID=l.AD_Client_ID ")
				.append("AND I_IsImported<>'Y' ")
				.append("ORDER BY i.DateLastInventory DESC LIMIT 1 ) ")
				.append("WHERE 1=1  ");
		DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		try
		{
			DB.commit(true, ITrx.TRXNAME_ThreadInherited);
		}
		catch (final Exception e)
		{
			throw new AdempiereException(e);
		}
	}

	private void dbCreateLocators()
	{
		final List<I_I_Inventory> unmatchedLocator = Services.get(IQueryBL.class).createQueryBuilder(I_I_Inventory.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_I_Inventory.COLUMNNAME_I_IsImported, false)
				.addNotNull(I_I_Inventory.COLUMN_LocatorValue)
				.addNotNull(I_I_Inventory.COLUMN_WarehouseValue)
				.addEqualsFilter(I_I_Inventory.COLUMNNAME_M_Locator_ID, null)
				.create()
				.list(I_I_Inventory.class);

		unmatchedLocator.forEach(importRecord -> {
			final I_M_Locator locator = getCreateNewMLocator(importRecord);
			if (locator != null)
			{
				importRecord.setM_Locator(locator);
				InterfaceWrapperHelper.save(importRecord);
			}
		});
	}

	private I_M_Locator getCreateNewMLocator(@NonNull final I_I_Inventory importRecord)
	{
		final IWarehouseDAO warehousesRepo = Services.get(IWarehouseDAO.class);

		//
		// check if exists, because might be created meanwhile
		if (importRecord.getM_Warehouse_ID() <= 0)
		{
			return null;
		}
		final WarehouseId warehouseId = WarehouseId.ofRepoId(importRecord.getM_Warehouse_ID());
		final LocatorId locatorId = warehousesRepo.retrieveLocatorIdByValueAndWarehouseId(importRecord.getLocatorValue(), warehouseId);
		final I_M_Locator locator;
		if (locatorId != null)
		{
			locator = warehousesRepo.getLocatorByIdInTrx(locatorId, I_M_Locator.class);
		}
		else
		{
			locator = InterfaceWrapperHelper.newInstance(I_M_Locator.class);
		}

		locator.setAD_Org_ID(importRecord.getAD_Org_ID());
		locator.setM_Warehouse_ID(warehouseId.getRepoId());
		locator.setValue(importRecord.getLocatorValue());
		locator.setX(importRecord.getX());
		locator.setY(importRecord.getY());
		locator.setZ(importRecord.getZ());
		locator.setX1(importRecord.getX1());
		locator.setDateLastInventory(importRecord.getDateLastInventory());
		InterfaceWrapperHelper.save(locator);

		return locator;
	}

	private void dbUpdateProducts(@NonNull final String sqlImportTableWhereClause)
	{
		// Match by product value
		dbUpdateProducts(
				sqlImportTableWhereClause,
				"i.Value LIKE 'val-%'",
				"p.Value = substr(i.Value, 5)");

		// Match by M_Product_ID
		dbUpdateProducts(
				sqlImportTableWhereClause,
				"i.Value ~ E'^\\\\d+$'",
				"p.M_Product_ID = i.Value::numeric");

		// Match by UPC
		dbUpdateProducts(
				sqlImportTableWhereClause,
				"i.UPC IS NOT NULL",
				"p.UPC = i.UPC");
	}

	private static int dbUpdateProducts(
			@NonNull final String importTableWhereClause,
			@NonNull final String importValueFormatMatcher,
			@NonNull final String importValueMatchCondition)
	{
		final String sqlProductId = "SELECT MAX(M_Product_ID)"
				+ " FROM M_Product p"
				+ " WHERE"
				+ " i.AD_Client_ID=p.AD_Client_ID"
				+ " AND (" + importValueMatchCondition + ")";

		final String sql = "UPDATE I_Inventory i "
				+ " SET M_Product_ID=(" + sqlProductId + ")"
				+ " WHERE"
				+ " I_IsImported<>'Y'"
				+ " AND M_Product_ID IS NULL"
				+ " AND i.Value IS NOT NULL"
				+ " AND (" + importValueFormatMatcher + ")"
				+ " " + importTableWhereClause;

		return DB.executeUpdateEx(sql, ITrx.TRXNAME_ThreadInherited);
	}

	private void dbUpdateSubProducer(@NonNull final String whereClause)
	{
		// Set M_Warehouse_ID
		final StringBuilder sql = new StringBuilder("UPDATE I_Inventory i ")
				.append("SET SubProducer_BPartner_ID=(SELECT C_BPartner_ID FROM C_BPartner bp WHERE i.SubProducerBPartner_Value=bp.value) ")
				.append("WHERE SubProducer_BPartner_ID IS NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
	}

	private void dbUpdateErrorMessages(@NonNull final String whereClause)
	{
		StringBuilder sql;
		int no;

		sql = new StringBuilder("UPDATE I_Inventory ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Locator, ' ")
				.append("WHERE M_Locator_ID IS NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		if (no != 0)
		{
			logger.warn("No Locator = {}", no);
		}

		sql = new StringBuilder("UPDATE I_Inventory ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Warehouse, ' ")
				.append("WHERE M_Warehouse_ID IS NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		if (no != 0)
		{
			logger.warn("No Warehouse = {}", no);
		}

		sql = new StringBuilder("UPDATE I_Inventory ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Product, ' ")
				.append("WHERE M_Product_ID IS NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		if (no != 0)
		{
			logger.warn("No Product = {}", no);
		}

		// No QtyCount
		sql = new StringBuilder("UPDATE I_Inventory ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No qtycount, ' ")
				.append("WHERE qtycount IS NULL ")
				.append("AND I_IsImported<>'Y' ")
				.append(whereClause);
		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		if (no != 0)
		{
			logger.warn("No qtycount = {}", no);
		}

	}
}
