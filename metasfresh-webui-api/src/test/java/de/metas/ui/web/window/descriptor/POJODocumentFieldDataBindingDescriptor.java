package de.metas.ui.web.window.descriptor;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import de.metas.ui.web.window.descriptor.LookupDescriptor.LookupScope;
import de.metas.ui.web.window.model.lookup.LookupDataSource;

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

public class POJODocumentFieldDataBindingDescriptor implements DocumentFieldDataBindingDescriptor
{
	public static final POJODocumentFieldDataBindingDescriptor of(final String columnName)
	{
		return new POJODocumentFieldDataBindingDescriptor(columnName);
	}

	private final String columnName;

	private POJODocumentFieldDataBindingDescriptor(final String columnName)
	{
		super();
		this.columnName = columnName;
	}

	@Override
	public String getColumnName()
	{
		return columnName;
	}

	@Override
	public LookupDescriptor getLookupDescriptor(final LookupScope scope)
	{
		return null;
	}

	@Override
	public LookupDataSource createLookupDataSource(final LookupScope scope)
	{
		return null;
	}

	@Override
	public Collection<String> getLookupValuesDependsOnFieldNames()
	{
		return ImmutableList.of();
	}
}