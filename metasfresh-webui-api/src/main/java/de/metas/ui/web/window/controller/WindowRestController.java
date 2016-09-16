package de.metas.ui.web.window.controller;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import de.metas.logging.LogManager;
import de.metas.ui.web.config.WebConfig;
import de.metas.ui.web.login.LoginService;
import de.metas.ui.web.session.UserSession;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.datatypes.json.JSONDocument;
import de.metas.ui.web.window.datatypes.json.JSONDocumentChangedEvent;
import de.metas.ui.web.window.datatypes.json.JSONDocumentLayout;
import de.metas.ui.web.window.datatypes.json.JSONDocumentLayoutTab;
import de.metas.ui.web.window.datatypes.json.JSONDocumentQueryFilter;
import de.metas.ui.web.window.datatypes.json.JSONDocumentViewResult;
import de.metas.ui.web.window.datatypes.json.JSONFilteringOptions;
import de.metas.ui.web.window.datatypes.json.JSONLookupValuesList;
import de.metas.ui.web.window.datatypes.json.JSONViewDataType;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor;
import de.metas.ui.web.window.descriptor.DocumentFieldDescriptor.Characteristic;
import de.metas.ui.web.window.descriptor.DocumentLayoutDescriptor;
import de.metas.ui.web.window.descriptor.DocumentLayoutDetailDescriptor;
import de.metas.ui.web.window.descriptor.DocumentLayoutSideListDescriptor;
import de.metas.ui.web.window.descriptor.DocumentQueryFilterDescriptor;
import de.metas.ui.web.window.model.Document;
import de.metas.ui.web.window.model.DocumentCollection;
import de.metas.ui.web.window.model.DocumentQuery;
import de.metas.ui.web.window.model.DocumentViewsRepository;
import de.metas.ui.web.window.model.IDocumentChangesCollector.ReasonSupplier;
import de.metas.ui.web.window.model.IDocumentView;
import de.metas.ui.web.window.model.IDocumentViewSelection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

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

@Api
@RestController
@RequestMapping(value = WindowRestController.ENDPOINT)
public class WindowRestController implements IWindowRestController
{
	public static final String ENDPOINT = WebConfig.ENDPOINT_ROOT + "/window";
	private static final String PARAM_WindowId = "type";
	private static final String PARAM_DocumentId = "id";
	private static final String PARAM_TabId = "tabid";
	private static final String PARAM_RowId = "rowId";
	private static final String PARAM_Field = "field";
	private static final String PARAM_FieldsList = "fields";
	private static final String PARAM_Advanced = "advanced";
	private static final String PARAM_Advanced_DefaultValue = "false";

	private static final String PARAM_ViewId = "viewId";
	private static final String PARAM_ViewDataType = "viewType";
	private static final String PARAM_FirstRow = "firstRow";
	private static final String PARAM_FirstRow_Description = "first row to fetch (starting from 0)";
	private static final String PARAM_PageLength = "pageLength";

	private static final Logger logger = LogManager.getLogger(WindowRestController.class);

	private static final ReasonSupplier REASON_Value_DirectSetFromCommitAPI = () -> "direct set from commit API";

	@Autowired
	private LoginService loginService;

	@Autowired
	@Lazy
	private UserSession userSession;

	@Autowired
	private DocumentCollection documentCollection;

	@Autowired
	private DocumentViewsRepository documentViewsRepo;

	private JSONFilteringOptions.Builder newJSONFilteringOptions()
	{
		final boolean debugShowColumnNamesForCaption = userSession.getPropertyAsBoolean(JSONFilteringOptions.SESSION_ATTR_ShowColumnNamesForCaption,
				JSONFilteringOptions.SESSION_ATTR_ShowColumnNamesForCaption_DefaulValue);

		return JSONFilteringOptions.builder()
				.setAD_Language(userSession.getAD_Language())
				.setDebugShowColumnNamesForCaption(debugShowColumnNamesForCaption);
	}

	@Override
	@RequestMapping(value = "/layout", method = RequestMethod.GET)
	public JSONDocumentLayout layout(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_TabId, required = false) final String detailId //
			, @RequestParam(name = PARAM_Advanced, required = false, defaultValue = PARAM_Advanced_DefaultValue) final boolean advanced //
	)
	{
		loginService.autologin();

		final DocumentLayoutDescriptor layout = documentCollection.getDocumentDescriptorFactory()
				.getDocumentDescriptor(adWindowId)
				.getLayout();

		final JSONFilteringOptions jsonFilteringOpts = newJSONFilteringOptions()
				.setShowAdvancedFields(advanced)
				.build();

		if (Strings.isNullOrEmpty(detailId))
		{
			return JSONDocumentLayout.ofHeaderLayout(layout, jsonFilteringOpts);
		}
		else
		{
			final DocumentLayoutDetailDescriptor detailLayout = layout.getDetail(detailId);
			return JSONDocumentLayout.ofDetailTab(adWindowId, detailLayout, jsonFilteringOpts);
		}
	}

	@Override
	@RequestMapping(value = "/data", method = RequestMethod.GET)
	public List<JSONDocument> data(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_DocumentId, required = true) final String idStr //
			, @RequestParam(name = PARAM_TabId, required = false) final String detailId //
			, @RequestParam(name = PARAM_RowId, required = false) final String rowIdStr //
			, @RequestParam(name = PARAM_FieldsList, required = false) @ApiParam("comma separated field names") final String fieldsListStr //
			, @RequestParam(name = PARAM_Advanced, required = false, defaultValue = PARAM_Advanced_DefaultValue) final boolean advanced //
	)
	{
		loginService.autologin();

		final DocumentPath documentPath = DocumentPath.builder()
				.setAD_Window_ID(adWindowId)
				.setDocumentId(idStr)
				.setDetailId(detailId)
				.setRowId(rowIdStr)
				.allowNullRowId()
				.build();

		//
		// Retrieve and return the documents
		final List<Document> documents = documentCollection.getDocuments(documentPath);
		return JSONDocument.ofDocumentsList(documents, newJSONFilteringOptions()
				.setShowAdvancedFields(advanced)
				.setDataFieldsList(fieldsListStr)
				.build());
	}

	@Override
	@RequestMapping(value = "/commit", method = RequestMethod.PATCH)
	public List<JSONDocument> commit(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_DocumentId, required = true) final String idStr //
			, @RequestParam(name = PARAM_TabId, required = false) final String detailId //
			, @RequestParam(name = PARAM_RowId, required = false) final String rowIdStr //
			, @RequestParam(name = PARAM_Advanced, required = false, defaultValue = PARAM_Advanced_DefaultValue) final boolean advanced //
			, @RequestBody final List<JSONDocumentChangedEvent> events)
	{
		loginService.autologin();

		final DocumentPath documentPath = DocumentPath.builder()
				.setAD_Window_ID(adWindowId)
				.setDocumentId(idStr)
				.allowNewDocumentId()
				.setDetailId(detailId)
				.setRowId(rowIdStr)
				.allowNewRowId()
				.build();

		final JSONFilteringOptions jsonFilteringOpts = newJSONFilteringOptions()
				.setShowAdvancedFields(advanced)
				.build();

		return Execution.callInNewExecution("window.commit", () -> commit0(documentPath, events, jsonFilteringOpts));
	}

	private List<JSONDocument> commit0(final DocumentPath documentPath, final List<JSONDocumentChangedEvent> events, final JSONFilteringOptions jsonFilteringOpts)
	{
		//
		// Fetch the document in writing mode
		final Document document = documentCollection.getOrCreateDocumentForWriting(documentPath);

		//
		// Apply changes
		for (final JSONDocumentChangedEvent event : events)
		{
			if (JSONDocumentChangedEvent.JSONOperation.replace == event.getOperation())
			{
				document.processValueChange(event.getPath(), event.getValue(), REASON_Value_DirectSetFromCommitAPI);
			}
			else
			{
				throw new IllegalArgumentException("Unknown operation: " + event);
			}
		}

		// Push back the changed document
		documentCollection.commit(document);

		//
		// Make sure all events were collected for the case when we just created the new document
		// FIXME: this is a workaround and in case we find out all events were collected, we just need to remove this.
		if (documentPath.isNewDocument())
		{
			logger.debug("Checking if we collected all events for the new document");
			final boolean somethingCollected = Execution.getCurrentDocumentChangesCollector().collectFrom(document, REASON_Value_DirectSetFromCommitAPI);
			if (somethingCollected)
			{
				logger.warn("We would expect all events to be auto-magically collected but it seems that not all of them were collected!", new Exception("StackTrace"));
			}
		}

		//
		// Return the changes
		return JSONDocument.ofEvents(Execution.getCurrentDocumentChangesCollector(), jsonFilteringOpts);
	}

	@Override
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public List<JSONDocument> delete(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_DocumentId, required = true) final String idStr //
			, @RequestParam(name = PARAM_TabId, required = false) final String detailId //
			, @RequestParam(name = PARAM_RowId, required = false) @ApiParam("comma separated rowIds") final String rowIdsListStr //
	)
	{
		loginService.autologin();

		final DocumentPath documentPath = DocumentPath.builder()
				.setAD_Window_ID(adWindowId)
				.setDocumentId(idStr)
				.setDetailId(detailId)
				.setRowIdsList(rowIdsListStr)
				.build();

		final JSONFilteringOptions jsonFilteringOptions = newJSONFilteringOptions()
				.setShowAdvancedFields(false)
				.build();

		return Execution.callInNewExecution("window.delete", () -> {
			documentCollection.delete(documentPath);
			return JSONDocument.ofEvents(Execution.getCurrentDocumentChangesCollector(), jsonFilteringOptions);
		});
	}

	@Override
	@RequestMapping(value = "/typeahead", method = RequestMethod.GET)
	public JSONLookupValuesList typeahead(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_DocumentId, required = true) final String idStr //
			, @RequestParam(name = PARAM_TabId, required = false) final String detailId //
			, @RequestParam(name = PARAM_RowId, required = false) final String rowIdStr //
			, @RequestParam(name = PARAM_Field, required = true) final String fieldName //
			, @RequestParam(name = "query", required = true) final String query //
	)
	{
		loginService.autologin();

		final DocumentPath documentPath = DocumentPath.builder()
				.setAD_Window_ID(adWindowId)
				.setDocumentId(idStr)
				.setDetailId(detailId)
				.setRowId(rowIdStr)
				.build();

		final Document document = documentCollection.getDocument(documentPath);
		final LookupValuesList lookupValues = document.getFieldLookupValuesForQuery(fieldName, query);
		return JSONLookupValuesList.ofLookupValuesList(lookupValues);
	}

	@Override
	@RequestMapping(value = "/dropdown", method = RequestMethod.GET)
	public JSONLookupValuesList dropdown(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_DocumentId, required = true, defaultValue = DocumentId.NEW_ID_STRING) final String idStr //
			, @RequestParam(name = PARAM_TabId, required = false) final String detailId //
			, @RequestParam(name = PARAM_RowId, required = false) final String rowIdStr //
			, @RequestParam(name = PARAM_Field, required = true) final String fieldName //
	)
	{
		loginService.autologin();

		final DocumentPath documentPath = DocumentPath.builder()
				.setAD_Window_ID(adWindowId)
				.setDocumentId(idStr)
				.setDetailId(detailId)
				.setRowId(rowIdStr)
				.build();

		final Document document = documentCollection.getDocument(documentPath);
		final LookupValuesList lookupValues = document.getFieldLookupValues(fieldName);

		return JSONLookupValuesList.ofLookupValuesList(lookupValues);
	}

	@Override
	@RequestMapping(value = "/viewLayout", method = RequestMethod.GET)
	public JSONDocumentLayoutTab viewLayout(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_ViewDataType, required = true) final JSONViewDataType viewDataType //

	)
	{
		loginService.autologin();

		final DocumentLayoutDescriptor layout = documentCollection.getDocumentDescriptorFactory()
				.getDocumentDescriptor(adWindowId)
				.getLayout();

		final JSONFilteringOptions jsonOpts = newJSONFilteringOptions()
				.build();

		switch (viewDataType)
		{
			case grid:
				final DocumentLayoutDetailDescriptor gridLayout = layout.getGridView();
				return JSONDocumentLayoutTab.of(adWindowId, gridLayout, jsonOpts);
			case list:
				final DocumentLayoutSideListDescriptor sideListLayout = layout.getSideList();
				final List<DocumentQueryFilterDescriptor> filters = layout.getFilters();
				return JSONDocumentLayoutTab.ofSideListLayout(adWindowId, sideListLayout, filters, newJSONFilteringOptions().build());
			default:
				throw new IllegalArgumentException("Invalid " + PARAM_ViewDataType + "=" + viewDataType);
		}
	}

	@Override
	@RequestMapping(value = "/view", method = RequestMethod.PUT)
	public JSONDocumentViewResult createView(
			@RequestParam(name = PARAM_WindowId, required = true) final int adWindowId //
			, @RequestParam(name = PARAM_ViewDataType, required = true) final JSONViewDataType viewDataType //
			, @RequestParam(name = PARAM_FirstRow, required = false, defaultValue = "0") @ApiParam(PARAM_FirstRow_Description) final int firstRow //
			, @RequestParam(name = PARAM_PageLength, required = false, defaultValue = "0") final int pageLength //
			, @RequestBody final List<JSONDocumentQueryFilter> jsonFilters //
	)
	{
		loginService.autologin();

		final DocumentEntityDescriptor entityDescriptor = documentCollection
				.getDocumentDescriptorFactory()
				.getDocumentDescriptor(adWindowId)
				.getEntityDescriptor();

		final Characteristic requiredFieldCharacteristic = viewDataType.getRequiredFieldCharacteristic();
		final List<DocumentFieldDescriptor> fields = entityDescriptor.getFieldsWithCharacteristic(requiredFieldCharacteristic);
		if (fields.isEmpty())
		{
			throw new IllegalStateException("No fields were found for " + PARAM_ViewDataType + ": " + viewDataType + "(required field characteristic: " + requiredFieldCharacteristic + ")");
		}

		final DocumentQuery query = DocumentQuery.builder(entityDescriptor)
				.setViewFields(fields)
				.addFilters(JSONDocumentQueryFilter.unwrapList(jsonFilters))
				.build();

		final IDocumentViewSelection view = documentViewsRepo.createView(query);

		//
		// Fetch result if requested
		if (pageLength > 0)
		{
			final List<IDocumentView> result = view.getPage(firstRow, pageLength);
			return JSONDocumentViewResult.of(view, firstRow, pageLength, result);
		}
		else
		{
			return JSONDocumentViewResult.of(view);
		}
	}

	@Override
	@RequestMapping(value = "/view/{" + PARAM_ViewId + "}", method = RequestMethod.DELETE)
	public void deleteView(
			@PathVariable(PARAM_ViewId) final String viewId//
	)
	{
		loginService.autologin();

		documentViewsRepo.deleteView(viewId);
	}

	@Override
	@RequestMapping(value = "/view/{" + PARAM_ViewId + "}", method = RequestMethod.GET)
	public JSONDocumentViewResult browseView(
			@PathVariable(PARAM_ViewId) final String viewId//
			, @RequestParam(name = PARAM_FirstRow, required = true) @ApiParam(PARAM_FirstRow_Description) final int firstRow //
			, @RequestParam(name = PARAM_PageLength, required = true) final int pageLength //
	)
	{
		loginService.autologin();

		final IDocumentViewSelection view = documentViewsRepo.getView(viewId);
		final List<IDocumentView> page = view.getPage(firstRow, pageLength);
		return JSONDocumentViewResult.of(view, firstRow, pageLength, page);
	}
}
