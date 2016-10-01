package de.metas.ui.web.window.datatypes.json;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.adempiere.util.GuavaCollectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import de.metas.ui.web.window.descriptor.DocumentLayoutDetailDescriptor;
import de.metas.ui.web.window.descriptor.DocumentLayoutSideListDescriptor;
import de.metas.ui.web.window.descriptor.DocumentQueryFilterDescriptor;
import io.swagger.annotations.ApiModel;

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

@ApiModel("tab")
@SuppressWarnings("serial")
public final class JSONDocumentLayoutTab implements Serializable
{
	static List<JSONDocumentLayoutTab> ofList(final Collection<DocumentLayoutDetailDescriptor> details, final JSONFilteringOptions jsonOpts)
	{
		final Collection<DocumentQueryFilterDescriptor> filters = null;
		return details.stream()
				.map(detail -> of(detail, filters, jsonOpts))
				.filter(jsonDetail -> jsonDetail.hasElements())
				.collect(GuavaCollectors.toImmutableList());
	}

	public static JSONDocumentLayoutTab of(
			final DocumentLayoutDetailDescriptor detail //
			, final Collection<DocumentQueryFilterDescriptor> filters //
			, final JSONFilteringOptions jsonOpts //
	)
	{
		return new JSONDocumentLayoutTab(detail, filters, jsonOpts);
	}

	public static final JSONDocumentLayoutTab ofSideListLayout(
			final DocumentLayoutSideListDescriptor sideListLayout //
			, final Collection<DocumentQueryFilterDescriptor> filters //
			, final JSONFilteringOptions jsonOpts //
	)
	{
		return new JSONDocumentLayoutTab(sideListLayout, filters, jsonOpts);
	}

	/** i.e. AD_Window_ID */
	@JsonProperty("type")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String type;

	@JsonProperty("tabid")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String tabid;

	@JsonProperty("caption")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String caption;

	@JsonProperty("description")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String description;

	@JsonProperty("emptyResultText")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultText;

	@JsonProperty("emptyResultHint")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultHint;

	@JsonProperty("elements")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<JSONDocumentLayoutElement> elements;

	@JsonProperty("filters")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<JSONDocumentQueryFilterDescriptor> filters;

	private JSONDocumentLayoutTab(
			final DocumentLayoutDetailDescriptor detail //
			, final Collection<DocumentQueryFilterDescriptor> filters //
			, final JSONFilteringOptions jsonOpts //
	)
	{
		super();

		type = String.valueOf(detail.getAD_Window_ID());

		final String detailId = detail.getDetailId();
		tabid = detailId;

		final String adLanguage = jsonOpts.getAD_Language();
		caption = detail.getCaption(adLanguage);
		description = detail.getDescription(adLanguage);
		emptyResultText = detail.getEmptyResultText(adLanguage);
		emptyResultHint = detail.getEmptyResultHint(adLanguage);

		elements = JSONDocumentLayoutElement.ofList(detail.getElements(), jsonOpts);

		this.filters = JSONDocumentQueryFilterDescriptor.ofCollection(filters, jsonOpts);
	}

	private JSONDocumentLayoutTab(
			final DocumentLayoutSideListDescriptor sideListLayout //
			, final Collection<DocumentQueryFilterDescriptor> filters //
			, final JSONFilteringOptions jsonOpts //
	)
	{
		super();
		type = String.valueOf(sideListLayout.getAD_Window_ID());
		tabid = null; // n/a

		final String adLanguage = jsonOpts.getAD_Language();
		caption = null; // n/a
		description = null; // n/a
		emptyResultText = sideListLayout.getEmptyResultText(adLanguage);
		emptyResultHint = sideListLayout.getEmptyResultHint(adLanguage);

		elements = JSONDocumentLayoutElement.ofList(sideListLayout.getElements(), jsonOpts);

		this.filters = JSONDocumentQueryFilterDescriptor.ofCollection(filters, jsonOpts);
	}

	@JsonCreator
	private JSONDocumentLayoutTab(
			@JsonProperty("type") final String type //
			, @JsonProperty("tabid") final String tabid //
			, @JsonProperty("caption") final String caption //
			, @JsonProperty("description") final String description //
			, @JsonProperty("emptyResultText") final String emptyResultText //
			, @JsonProperty("emptyResultHint") final String emptyResultHint //
			, @JsonProperty("elements") final List<JSONDocumentLayoutElement> elements //
			, @JsonProperty("filters") final List<JSONDocumentQueryFilterDescriptor> filters //
	)
	{
		super();
		this.type = type;
		this.tabid = tabid;

		this.caption = caption;
		this.description = description;
		this.emptyResultText = emptyResultText;
		this.emptyResultHint = emptyResultHint;

		this.elements = elements == null ? ImmutableList.of() : ImmutableList.copyOf(elements);
		this.filters = filters == null ? ImmutableList.of() : ImmutableList.copyOf(filters);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("tabid", tabid)
				.add("caption", caption)
				.add("elements", elements.isEmpty() ? null : elements)
				.add("filters", filters.isEmpty() ? null : filters)
				.toString();
	}

	public String getTabid()
	{
		return tabid;
	}

	public String getCaption()
	{
		return caption;
	}

	public String getDescription()
	{
		return description;
	}

	public String getEmptyResultText()
	{
		return emptyResultText;
	}

	public String getEmptyResultHint()
	{
		return emptyResultHint;
	}

	public List<JSONDocumentLayoutElement> getElements()
	{
		return elements;
	}

	public boolean hasElements()
	{
		return !elements.isEmpty();
	}

	public List<JSONDocumentQueryFilterDescriptor> getFilters()
	{
		return filters;
	}
}
