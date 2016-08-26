package de.metas.ui.web.window.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.datatypes.DataTypes;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.model.IDocumentChangesCollector.ReasonSupplier;

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

public final class DocumentChanges
{
	private final DocumentPath documentPath;
	private final Map<String, DocumentFieldChange> fieldChangesByName = new LinkedHashMap<>();
	private DocumentValidStatus documentValidStatus = null;
	private DocumentSaveStatus documentSaveStatus = null;

	/* package */ DocumentChanges(final DocumentPath documentPath)
	{
		super();

		Preconditions.checkNotNull(documentPath, "documentPath");
		this.documentPath = documentPath;
	}

	public DocumentPath getDocumentPath()
	{
		return documentPath;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("documentPath", documentPath)
				.add("fields", fieldChangesByName)
				.toString();
	}

	public Set<String> getFieldNames()
	{
		return ImmutableSet.copyOf(fieldChangesByName.keySet());
	}

	public boolean isEmpty()
	{
		return fieldChangesByName.isEmpty()
				&& documentValidStatus == null
				&& documentSaveStatus == null;
	}

	private DocumentFieldChange fieldChangesOf(final IDocumentFieldView documentField)
	{
		// Make sure the field is about same document path
		if (!documentPath.equals(documentField.getDocumentPath()))
		{
			throw new IllegalArgumentException("Field " + documentField + " does not have expected path: " + documentPath);
		}

		return fieldChangesByName.computeIfAbsent(documentField.getFieldName(), (fieldName) -> {
			final DocumentFieldChange event = DocumentFieldChange.of(fieldName, documentField.isKey(), documentField.isPublicField());
			if (WindowConstants.isProtocolDebugging())
			{
				event.putDebugProperty(DocumentFieldChange.DEBUGPROPERTY_FieldInfo, documentField.toString());
			}
			return event;
		});
	}

	private DocumentFieldChange fieldChangesOf(final String fieldName, final boolean key, final boolean publicField)
	{
		return fieldChangesByName.computeIfAbsent(fieldName, (newFieldName) -> DocumentFieldChange.of(newFieldName, key, publicField));
	}

	public List<DocumentFieldChange> getFieldChangesList()
	{
		return ImmutableList.copyOf(fieldChangesByName.values());
	}

	private static final String extractReason(final ReasonSupplier reasonSupplier)
	{
		if (reasonSupplier == null)
		{
			return null;
		}

		// Extract the reason only if debugging is enabled
		if (!WindowConstants.isProtocolDebugging())
		{
			return null;
		}

		return reasonSupplier.get();
	}

	private static final String mergeReasons(final ReasonSupplier reason, final String previousReason)
	{
		final Object previousValue = null;
		return mergeReasons(reason, previousReason, previousValue);
	}

	private static final String mergeReasons(final ReasonSupplier reasonSupplier, final String previousReason, final Object previousValue)
	{
		// Collect the reason only if debugging is enabled
		if (!WindowConstants.isProtocolDebugging())
		{
			return null;
		}

		final String reason = reasonSupplier == null ? null : reasonSupplier.get();
		if (previousReason == null && previousValue == null)
		{
			return reason;
		}

		final StringBuilder reasonNew = new StringBuilder();
		reasonNew.append(reason == null ? "unknown reason" : reason);

		if (previousReason != null)
		{
			reasonNew.append(" | previous reason: ").append(previousReason);
		}
		if (previousValue != null)
		{
			reasonNew.append(" | previous value: ").append(previousValue);
		}
		return reasonNew.toString();
	}

	/* package */ void collectValueChanged(final IDocumentFieldView documentField, final ReasonSupplier reason)
	{
		fieldChangesOf(documentField).setValue(documentField.getValue(), extractReason(reason));
	}

	/* package */ void collectReadonlyChanged(final IDocumentFieldView documentField, final ReasonSupplier reason)
	{
		fieldChangesOf(documentField).setReadonly(documentField.isReadonly(), extractReason(reason));
	}

	/* package */ void collectMandatoryChanged(final IDocumentFieldView documentField, final ReasonSupplier reason)
	{
		fieldChangesOf(documentField).setMandatory(documentField.isMandatory(), extractReason(reason));
	}

	/* package */ void collectDisplayedChanged(final IDocumentFieldView documentField, final ReasonSupplier reason)
	{
		fieldChangesOf(documentField).setDisplayed(documentField.isDisplayed(), extractReason(reason));
	}

	/* package */ void collectLookupValuesStaled(final IDocumentFieldView documentField, final ReasonSupplier reason)
	{
		fieldChangesOf(documentField).setLookupValuesStale(true, extractReason(reason));
	}

	/* package */void collectFrom(final DocumentChanges fromDocumentChanges)
	{
		for (final DocumentFieldChange fromFieldChange : fromDocumentChanges.getFieldChangesList())
		{
			final DocumentFieldChange toFieldChange = fieldChangesOf(fromFieldChange.getFieldName(), fromFieldChange.isKey(), fromFieldChange.isPublicField());
			toFieldChange.mergeFrom(fromFieldChange);
		}

		if (fromDocumentChanges.documentValidStatus != null)
		{
			collectDocumentValidStatusChanged(fromDocumentChanges.documentValidStatus);
		}

		if (fromDocumentChanges.documentSaveStatus != null)
		{
			collectDocumentSaveStatusChanged(fromDocumentChanges.documentSaveStatus);
		}
	}

	/* package */boolean collectFrom(final Document document, final ReasonSupplier reason)
	{
		boolean collected = false;

		for (final IDocumentFieldView documentField : document.getFieldViews())
		{
			if (collectFrom(documentField, reason))
			{
				collected = true;
			}
		}

		return collected;
	}

	private boolean collectFrom(final IDocumentFieldView documentField, final ReasonSupplier reason)
	{
		final DocumentFieldChange toEvent = fieldChangesOf(documentField);

		boolean collected = false;

		//
		// Value
		if (!toEvent.isValueSet())
		{
			final Object value = documentField.getValue();
			toEvent.setValue(value, extractReason(reason));
		}
		else
		{
			final Object value = documentField.getValue();
			final Object previousValue = toEvent.getValue();
			if (!DataTypes.equals(value, previousValue))
			{
				toEvent.setValue(value, mergeReasons(reason, toEvent.getValueReason(), previousValue == null ? "<NULL>" : previousValue));
				collected = true;
			}
		}

		//
		// Readonly
		final boolean readonly = documentField.isReadonly();
		if (!DataTypes.equals(readonly, toEvent.getReadonly()))
		{
			toEvent.setReadonly(readonly, mergeReasons(reason, toEvent.getReadonlyReason()));
			collected = true;
		}

		//
		// Mandatory
		final boolean mandatory = documentField.isMandatory();
		if (!DataTypes.equals(mandatory, toEvent.getMandatory()))
		{
			toEvent.setMandatory(mandatory, mergeReasons(reason, toEvent.getMandatoryReason()));
			collected = true;
		}

		//
		// Displayed
		final boolean displayed = documentField.isDisplayed();
		if (!DataTypes.equals(displayed, toEvent.getDisplayed()))
		{
			toEvent.setDisplayed(displayed, mergeReasons(reason, toEvent.getDisplayedReason()));
			collected = true;
		}

		return collected;
	}

	/* package */void collectDocumentValidStatusChanged(final DocumentValidStatus documentValidStatus)
	{
		this.documentValidStatus = documentValidStatus;
	}

	public DocumentValidStatus getDocumentValidStatus()
	{
		return documentValidStatus;
	}

	/* package */void collectDocumentSaveStatusChanged(final DocumentSaveStatus documentSaveStatus)
	{
		this.documentSaveStatus = documentSaveStatus;
	}

	public DocumentSaveStatus getDocumentSaveStatus()
	{
		return documentSaveStatus;
	}

}
