package de.metas.ui.web.window.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.util.GuavaCollectors;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

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

public final class DocumentLayoutElementLineDescriptor
{
	public static final Builder builder()
	{
		return new Builder();
	}

	private final List<DocumentLayoutElementDescriptor> elements;

	private DocumentLayoutElementLineDescriptor(final Builder builder)
	{
		super();
		elements = ImmutableList.copyOf(builder.buildElements());
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("elements", elements.isEmpty() ? null : elements)
				.toString();
	}

	public List<DocumentLayoutElementDescriptor> getElements()
	{
		return elements;
	}

	public boolean hasElements()
	{
		return !elements.isEmpty();
	}

	public static final class Builder
	{
		private final List<DocumentLayoutElementDescriptor.Builder> elementsBuilders = new ArrayList<>();

		private Builder()
		{
			super();
		}

		public DocumentLayoutElementLineDescriptor build()
		{
			return new DocumentLayoutElementLineDescriptor(this);
		}

		private List<DocumentLayoutElementDescriptor> buildElements()
		{
			return elementsBuilders
					.stream()
					.filter(elementBuilder -> !elementBuilder.isConsumed())
					.map(elementBuilder -> elementBuilder.build())
					.filter(element -> element.hasFields())
					.collect(GuavaCollectors.toImmutableList());
		}

		public Builder addElement(final DocumentLayoutElementDescriptor.Builder elementBuilder)
		{
			elementsBuilders.add(elementBuilder);
			return this;
		}
	}

}
