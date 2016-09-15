package de.metas.ui.web.window.model.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import org.adempiere.ad.persistence.TableModelLoader;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.exceptions.DBMoreThenOneRecordsFoundException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import com.google.common.base.Joiner;

import de.metas.logging.LogManager;
import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.datatypes.DataTypes;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDataBindingDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor;
import de.metas.ui.web.window.descriptor.sql.SqlDocumentFieldDataBindingDescriptor;
import de.metas.ui.web.window.descriptor.sql.SqlDocumentFieldDataBindingDescriptor.DocumentFieldValueLoader;
import de.metas.ui.web.window.model.Document;
import de.metas.ui.web.window.model.Document.FieldValueSupplier;
import de.metas.ui.web.window.model.DocumentQuery;
import de.metas.ui.web.window.model.DocumentsRepository;
import de.metas.ui.web.window.model.IDocumentFieldView;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
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

/**
 *
 * IMPORTANT: please make sure this is state-less and thread-safe
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Repository
public class SqlDocumentRepository implements DocumentsRepository
{
	private static final transient Logger logger = LogManager.getLogger(SqlDocumentRepository.class);

	private static final AtomicInteger _nextMissingId = new AtomicInteger(-10000);

	/* package */ SqlDocumentRepository()
	{
		super();
	}

	private int getNextId(final DocumentEntityDescriptor entityDescriptor)
	{
		final int adClientId = Env.getAD_Client_ID(Env.getCtx());
		final String tableName = entityDescriptor.getDataBinding().getTableName();
		final int nextId = DB.getNextID(adClientId, tableName, ITrx.TRXNAME_ThreadInherited);
		if (nextId <= 0)
		{
			throw new DBException("Cannot retrieve next ID from database for " + entityDescriptor);
		}

		logger.trace("Acquired next ID={} for {}", nextId, entityDescriptor);
		return nextId;
	}

	@Override
	public List<Document> retriveDocuments(final DocumentQuery query)
	{
		final int limit = query.getPageLength();
		return retriveDocuments(query, limit);
	}

	public List<Document> retriveDocuments(final DocumentQuery query, final int limit)
	{
		logger.debug("Retrieving records: query={}, limit={}", query, limit);

		final DocumentEntityDescriptor entityDescriptor = query.getEntityDescriptor();
		final Document parentDocument = query.getParentDocument();

		final List<Object> sqlParams = new ArrayList<>();
		final String sql = SqlDocumentQueryBuilder.of(query).getSql(sqlParams);
		logger.debug("Retrieving records: SQL={} -- {}", sql, sqlParams);

		final List<Document> documentsCollector = new ArrayList<>(limit > 0 ? limit + 1 : 0);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, ITrx.TRXNAME_ThreadInherited);
			DB.setParameters(pstmt, sqlParams);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				final Document document = retriveDocument(entityDescriptor, parentDocument, rs);
				documentsCollector.add(document);

				// Stop if we reached the limit
				if (limit > 0 && documentsCollector.size() > limit)
				{
					break;
				}
			}
		}
		catch (final Exception e)
		{
			throw new DBException(e, sql, sqlParams);
		}
		finally
		{
			DB.close(rs, pstmt);
		}

		logger.debug("Retrieved {} records.", documentsCollector.size());
		return documentsCollector;
	}

	@Override
	public Document retriveDocument(final DocumentQuery query)
	{
		final int limit = 2;
		final List<Document> documents = retriveDocuments(query, limit);
		if (documents.isEmpty())
		{
			return null;
		}
		else if (documents.size() > 1)
		{
			throw new DBMoreThenOneRecordsFoundException("More than one record found for " + query + " on " + this
					+ "\n First " + limit + " records: " + Joiner.on("\n").join(documents));
		}
		else
		{
			return documents.get(0);
		}
	}

	@Override
	public Document createNewDocument(final DocumentEntityDescriptor entityDescriptor, final Document parentDocument)
	{
		final int documentId = getNextId(entityDescriptor);
		return Document.builder()
				.setDocumentRepository(this)
				.setEntityDescriptor(entityDescriptor)
				.setParentDocument(parentDocument)
				.setDocumentIdSupplier(() -> documentId)
				.initializeAsNewDocument()
				.build();
	}

	private Document retriveDocument(final DocumentEntityDescriptor entityDescriptor, final Document parentDocument, final ResultSet rs)
	{
		final IntSupplier documentIdSupplier;
		final DocumentFieldDescriptor idField = entityDescriptor.getIdField();
		final ResultSetFieldValueSupplier fieldValueSupplier = new ResultSetFieldValueSupplier(rs);
		if (idField == null)
		{
			// FIXME: workaround to bypass the missing ID field for views
			final int missingId = _nextMissingId.decrementAndGet();
			documentIdSupplier = () -> missingId;
		}
		else
		{
			documentIdSupplier = () -> (Integer)fieldValueSupplier.getValue(idField);
		}

		return Document.builder()
				.setDocumentRepository(this)
				.setEntityDescriptor(entityDescriptor)
				.setParentDocument(parentDocument)
				.setDocumentIdSupplier(documentIdSupplier)
				.initializeAsExistingRecord(fieldValueSupplier)
				.build();
	}

	private static final class ResultSetFieldValueSupplier implements FieldValueSupplier
	{
		private final ResultSet rs;

		public ResultSetFieldValueSupplier(final ResultSet rs)
		{
			super();
			Check.assumeNotNull(rs, "Parameter rs is not null");
			this.rs = rs;
		}

		@Override
		public Object getValue(final DocumentFieldDescriptor fieldDescriptor)
		{
			final SqlDocumentFieldDataBindingDescriptor fieldDataBinding = SqlDocumentFieldDataBindingDescriptor.castOrNull(fieldDescriptor.getDataBinding());

			// If there is no SQL databinding, we cannot provide a value
			if (fieldDataBinding == null)
			{
				return NO_VALUE;
			}

			final DocumentFieldValueLoader fieldValueLoader = fieldDataBinding.getDocumentFieldValueLoader();

			try
			{
				return fieldValueLoader.retrieveFieldValue(rs);
			}
			catch (final SQLException e)
			{
				throw new DBException("Failed retrieving the value for " + fieldDescriptor + " using " + fieldValueLoader, e);
			}
		}
	}

	@Override
	public void refresh(final Document document)
	{
		refresh(document, document.getDocumentIdAsInt());
	}

	private void refresh(final Document document, final int documentId)
	{
		logger.debug("Refreshing: {}, using ID={}", document, documentId);

		final DocumentQuery query = DocumentQuery.ofRecordId(document.getEntityDescriptor(), documentId);

		final List<Object> sqlParams = new ArrayList<>();
		final String sql = SqlDocumentQueryBuilder.of(query).getSql(sqlParams);
		logger.debug("Retrieving records: SQL={} -- {}", sql, sqlParams);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, ITrx.TRXNAME_ThreadInherited);
			DB.setParameters(pstmt, sqlParams);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				final ResultSetFieldValueSupplier fieldValueSupplier = new ResultSetFieldValueSupplier(rs);
				document.refresh(fieldValueSupplier);
			}
			else
			{
				throw new AdempiereException("No data found while trying to reload the document: " + document);
			}

			if (rs.next())
			{
				throw new AdempiereException("More than one record found while trying to reload document: " + document);
			}
		}
		catch (final Exception e)
		{
			throw new DBException(e, sql, sqlParams);
		}
		finally
		{
			DB.close(rs, pstmt);
		}
	}

	@Override
	public void save(final Document document)
	{
		Services.get(ITrxManager.class).assertThreadInheritedTrxExists();

		//
		// Load the PO / Create new PO instance
		final PO po = retrieveOrCreatePO(document);

		// TODO: handle the case of composed primary key!
		if (po.getPOInfo().getKeyColumnName() == null)
		{
			throw new UnsupportedOperationException("Composed primary key is not supported");
		}

		//
		// Set values to PO
		final boolean isNew = document.isNew();
		boolean changes = false;
		for (final IDocumentFieldView documentField : document.getFieldViews())
		{
			if (!isNew && !documentField.hasChanges())
			{
				logger.trace("Skip setting PO value because document field has no changes: {}", documentField);
				continue;
			}

			if (setPOValue(po, documentField))
			{
				changes = true;
			}
		}

		if (!changes)
		{
			logger.trace("Skip saving {} because there was no actual change", po);
			return;
		}

		//
		// Actual save
		// TODO: advice the PO to not reload after save.
		InterfaceWrapperHelper.save(po);
		document.markAsNotNew();

		//
		// Reload the document
		final int idNew = InterfaceWrapperHelper.getId(po);
		refresh(document, idNew);
	}

	private PO retrieveOrCreatePO(final Document document)
	{
		final String sqlTableName = document.getEntityDescriptor().getDataBinding().getTableName();

		//
		// Load the PO / Create new PO instance
		final PO po;
		if (document.isNew())
		{
			po = TableModelLoader.instance.newPO(document.getCtx(), sqlTableName, ITrx.TRXNAME_ThreadInherited);
		}
		else
		{
			final boolean checkCache = false;
			po = TableModelLoader.instance.getPO(document.getCtx(), sqlTableName, document.getDocumentIdAsInt(), checkCache, ITrx.TRXNAME_ThreadInherited);

			if (po == null)
			{
				throw new DBException("No PO found for " + document);
			}
		}

		//
		//
		po.set_ManualUserAction(document.getWindowNo());
		InterfaceWrapperHelper.ATTR_ReadOnlyColumnCheckDisabled.setValue(po, true); // allow changing any columns

		return po;
	}

	/**
	 * Sets PO's value from given <code>documentField</code>.
	 *
	 * @param po
	 * @param documentField
	 * @return true if value was set and really changed
	 */
	private boolean setPOValue(final PO po, final IDocumentFieldView documentField)
	{
		final DocumentFieldDataBindingDescriptor dataBinding = documentField.getDescriptor().getDataBinding().orElse(null);
		if (dataBinding == null)
		{
			logger.trace("Skip setting PO's column because it has no databinding: {}", documentField);
			return false;
		}

		final POInfo poInfo = po.getPOInfo();
		final String columnName = dataBinding.getColumnName();

		final int poColumnIndex = poInfo.getColumnIndex(columnName);
		if (poColumnIndex < 0)
		{
			logger.trace("Skip setting PO's column because it's missing: {} -- PO={}", columnName, po);
			return false;
		}

		//
		// Virtual column => skip setting it
		if (poInfo.isVirtualColumn(poColumnIndex))
		{
			logger.trace("Skip setting PO's virtual column: {} -- PO={}", columnName, po);
			return false; // no change
		}
		//
		// ID
		else if (poInfo.isKey(poColumnIndex))
		{
			final int id = documentField.getValueAsInt(-1);
			if (id >= 0)
			{
				final int idOld = po.get_ValueAsInt(poColumnIndex);
				if (id == idOld)
				{
					logger.trace("Skip setting PO's key column because it's the same as the old value: {} (old={}), PO={}", columnName, idOld, po);
					return false; // no change
				}

				final boolean idSet = po.set_ValueNoCheck(poColumnIndex, id);
				if (!idSet)
				{
					throw new AdempiereException("Failed setting ID=" + id + " to " + po);
				}

				logger.trace("Setting PO ID: {}={} -- PO={}", columnName, id, po);
				return true;
			}
			else
			{
				logger.trace("Skip setting PO's key column: {} -- PO={}", columnName, po);
				return false; // no change
			}
		}
		//
		// Created/Updated columns
		else if (WindowConstants.FIELDNAMES_CreatedUpdated.contains(columnName))
		{
			logger.trace("Skip setting PO's created/updated column: {} -- PO={}", columnName, po);
			return false; // no change
		}
		//
		// Regular column
		else
		{
			//
			// Check if value was changed, compared with PO's current value
			final Object poValue = po.get_Value(poColumnIndex);
			final Class<?> poValueClass = poInfo.getColumnClass(poColumnIndex);
			final Object fieldValueConv = convertValueToPO(documentField.getValue(), columnName, poValueClass);
			if (DataTypes.equals(fieldValueConv, poValue))
			{
				logger.trace("Skip setting PO's column because it was not changed: {}={} (old={}) -- PO={}", columnName, fieldValueConv, poValue, po);
				return false; // no change
			}

			//
			// Check if the field value was changed from when we last queried it
			if (!po.is_new())
			{
				final Object fieldInitialValueConv = convertValueToPO(documentField.getInitialValue(), columnName, poValueClass);
				if (!DataTypes.equals(fieldInitialValueConv, poValue))
				{
					throw new AdempiereException("Document's field was changed from when we last queried it. Please re-query."
							+ "\n Document field initial value: " + fieldInitialValueConv
							+ "\n PO value: " + poValue
							+ "\n Document field: " + documentField
							+ "\n PO: " + po);
				}
			}

			// TODO: handle not updateable columns... i think we shall set them only if the PO is new

			// NOTE: at this point we shall not do any other validations like "mandatory but null", value min/max range check,
			// because we shall rely completely on Document level validations and not duplicate the logic here.

			//
			// Try setting the value
			final boolean valueSet = po.set_ValueReturningBoolean(poColumnIndex, fieldValueConv);
			if (!valueSet)
			{
				logger.warn("Failed setting PO's column: {}={} (old={}) -- PO={}", columnName, fieldValueConv, poValue, po);
				return false; // no change
			}

			logger.trace("Setting PO value: {}={} (old={}) -- PO={}", columnName, fieldValueConv, poValue, po);
			return true;
		}
	}

	static Object convertValueToPO(final Object value, final String columnName, final Class<?> targetClass)
	{
		final Class<?> valueClass = value == null ? null : value.getClass();

		if (valueClass != null && targetClass.isAssignableFrom(valueClass))
		{
			return value;
		}
		else if (int.class.equals(targetClass) || Integer.class.equals(targetClass))
		{
			if (value == null)
			{
				return null;
			}
			else if (LookupValue.class.isAssignableFrom(valueClass))
			{
				return ((LookupValue)value).getIdAsInt();
			}
			else if (Number.class.isAssignableFrom(valueClass))
			{
				return ((Number)value).intValue();
			}
			else if (String.class.equals(valueClass))
			{
				return Integer.parseInt((String)value);
			}
			else if (Map.class.isAssignableFrom(valueClass))
			{
				@SuppressWarnings("unchecked")
				final Map<String, String> map = (Map<String, String>)value;
				final IntegerLookupValue lookupValue = JSONLookupValue.integerLookupValueFromJsonMap(map);
				return lookupValue == null ? null : lookupValue.getIdAsInt();
			}
		}
		else if (String.class.equals(targetClass))
		{
			if (value == null)
			{
				return null;
			}
			else if (LookupValue.class.isAssignableFrom(valueClass))
			{
				return ((LookupValue)value).getIdAsString();
			}
			else if (Map.class.isAssignableFrom(valueClass))
			{
				@SuppressWarnings("unchecked")
				final Map<String, String> map = (Map<String, String>)value;
				final StringLookupValue lookupValue = JSONLookupValue.stringLookupValueFromJsonMap(map);
				return lookupValue == null ? null : lookupValue.getIdAsString();
			}
		}
		else if (Timestamp.class.equals(targetClass))
		{
			if (value == null)
			{
				return null;
			}
			else if (java.util.Date.class.isAssignableFrom(valueClass))
			{
				return new Timestamp(((java.util.Date)value).getTime());
			}
		}
		else if (Boolean.class.equals(targetClass) || boolean.class.equals(targetClass))
		{
			if (value == null)
			{
				return false;
			}
			else if (String.class.equals(valueClass))
			{
				return DisplayType.toBoolean(value);
			}
			else if (StringLookupValue.class.isAssignableFrom(valueClass))
			{
				// Corner case: e.g. Posted column which is a List but the PO is handling it as boolean
				final StringLookupValue stringLookupValue = (StringLookupValue)value;
				final Boolean valueBoolean = DisplayType.toBoolean(stringLookupValue.getIdAsString(), null);
				if (valueBoolean != null)
				{
					return valueBoolean;
				}
			}
		}

		// Better return the original value and let the PO fail.
		return value;
		// throw new AdempiereException("Cannot convert value '" + value + "' from " + valueClass + " to " + targetClass
		// + "\n ColumnName: " + columnName
		// + "\n PO: " + po);
	}

	@Override
	public void delete(final Document document)
	{
		Services.get(ITrxManager.class).assertThreadInheritedTrxExists();

		if (document.isNew())
		{
			throw new IllegalArgumentException("Cannot delete new document: " + document);
		}

		final PO po = retrieveOrCreatePO(document);

		InterfaceWrapperHelper.delete(po);
	}
}
