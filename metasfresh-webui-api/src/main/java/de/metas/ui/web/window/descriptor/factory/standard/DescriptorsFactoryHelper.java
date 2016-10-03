package de.metas.ui.web.window.descriptor.factory.standard;

import java.math.BigDecimal;
import java.util.Set;

import org.compiere.util.DisplayType;

import com.google.common.collect.ImmutableSet;

import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.DocumentLayoutElementFieldDescriptor;
import de.metas.ui.web.window.descriptor.LookupDescriptor;
import de.metas.ui.web.window.exceptions.DocumentLayoutBuildException;

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
 * Miscellaneous descriptors building helpers.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public final class DescriptorsFactoryHelper
{
	/** Column names where we shall use {@link DocumentFieldWidgetType#Switch} instead of {@link DocumentFieldWidgetType#YesNo} */
	private static final Set<String> COLUMNNAMES_Switch = ImmutableSet.of(WindowConstants.FIELDNAME_IsActive); // FIXME: hardcoded

	private DescriptorsFactoryHelper()
	{
		super();
	}

	public static Class<?> getValueClass(final int displayType, final LookupDescriptor lookupDescriptor)
	{
		if (lookupDescriptor != null)
		{
			return lookupDescriptor.getValueClass();
		}
		else
		{
			return DescriptorsFactoryHelper.getValueClass(displayType);
		}

	}

	private static Class<?> getValueClass(final int displayType)
	{
		if (displayType == DisplayType.List)
		{
			return StringLookupValue.class;
		}
		else if (displayType == DisplayType.Location)
		{
			return IntegerLookupValue.class;
		}
		else if (displayType == DisplayType.PAttribute)
		{
			return IntegerLookupValue.class;
		}
		else if (displayType == DisplayType.Table || displayType == DisplayType.Search)
		{
			throw new IllegalArgumentException("Table and Search references are not supported at this level"); // shall not happen
		}
		else if (DisplayType.isAnyLookup(displayType))
		{
			return IntegerLookupValue.class;
		}
		else if (displayType == DisplayType.ID)
		{
			return Integer.class;
		}
		//
		//
		else if (displayType == DisplayType.Date)
		{
			return java.util.Date.class;
		}
		else if (displayType == DisplayType.Time)
		{
			return java.util.Date.class;
		}
		else if (displayType == DisplayType.DateTime)
		{
			return java.util.Date.class;
		}
		//
		//
		else if (displayType == DisplayType.TextLong || displayType == DisplayType.Memo || displayType == DisplayType.Text)
		{
			return String.class;
		}
		else if (DisplayType.isText(displayType))
		{
			return String.class;
		}
		//
		//
		else if (DisplayType.Integer == displayType)
		{
			return Integer.class;
		}
		else if (displayType == DisplayType.Amount)
		{
			return BigDecimal.class;
		}
		else if (displayType == DisplayType.Number)
		{
			return BigDecimal.class;
		}
		else if (displayType == DisplayType.CostPrice)
		{
			return BigDecimal.class;
		}
		else if (displayType == DisplayType.Quantity)
		{
			return BigDecimal.class;
		}
		//
		//
		else if (displayType == DisplayType.YesNo)
		{
			return Boolean.class;
		}
		else if (displayType == DisplayType.Button)
		{
			return String.class;
		}
		else if (displayType == DisplayType.Image)
		{
			return Integer.class;
		}
		//
		//
		else
		{
			throw new DocumentLayoutBuildException("Unknown displayType=" + displayType);
		}
	}

	public static DocumentFieldWidgetType extractWidgetType(final String columnName, final int displayType)
	{
		if (displayType == DisplayType.List)
		{
			return DocumentFieldWidgetType.List;
		}
		else if (displayType == DisplayType.Location)
		{
			return DocumentFieldWidgetType.Address;
		}
		else if (displayType == DisplayType.PAttribute)
		{
			return DocumentFieldWidgetType.ProductAttributes;
		}
		else if (displayType == DisplayType.Table)
		{
			return DocumentFieldWidgetType.List;
		}
		else if (displayType == DisplayType.TableDir)
		{
			return DocumentFieldWidgetType.List;
		}
		else if (displayType == DisplayType.Search)
		{
			return DocumentFieldWidgetType.Lookup;
		}
		else if (DisplayType.isAnyLookup(displayType))
		{
			return DocumentFieldWidgetType.Lookup;
		}
		else if (displayType == DisplayType.ID)
		{
			return DocumentFieldWidgetType.Integer;
		}
		//
		//
		else if (displayType == DisplayType.Date)
		{
			return DocumentFieldWidgetType.Date;
		}
		else if (displayType == DisplayType.Time)
		{
			return DocumentFieldWidgetType.Time;
		}
		else if (displayType == DisplayType.DateTime)
		{
			return DocumentFieldWidgetType.DateTime;
		}
		//
		//
		else if (displayType == DisplayType.TextLong || displayType == DisplayType.Memo || displayType == DisplayType.Text)
		{
			return DocumentFieldWidgetType.LongText;
		}
		else if (DisplayType.isText(displayType))
		{
			return DocumentFieldWidgetType.Text;
		}
		//
		//
		else if (DisplayType.Integer == displayType)
		{
			return DocumentFieldWidgetType.Integer;
		}
		else if (displayType == DisplayType.Amount)
		{
			return DocumentFieldWidgetType.Amount;
		}
		else if (displayType == DisplayType.Number)
		{
			return DocumentFieldWidgetType.Number;
		}
		else if (displayType == DisplayType.CostPrice)
		{
			return DocumentFieldWidgetType.CostPrice;
		}
		else if (displayType == DisplayType.Quantity)
		{
			return DocumentFieldWidgetType.Quantity;
		}
		//
		//
		else if (displayType == DisplayType.YesNo)
		{
			if (COLUMNNAMES_Switch.contains(columnName))
			{
				return DocumentFieldWidgetType.Switch;
			}
			return DocumentFieldWidgetType.YesNo;
		}
		else if (displayType == DisplayType.Button)
		{
			return DocumentFieldWidgetType.Button;
		}
		else if (displayType == DisplayType.Image)
		{
			return DocumentFieldWidgetType.Image;
		}
		//
		//
		else
		{
			throw new DocumentLayoutBuildException("Unknown displayType=" + displayType + " of columnName=" + columnName);
		}
	}

	public static DocumentLayoutElementFieldDescriptor.LookupSource extractLookupSource(final int displayType, final int adReferenceValueId)
	{
		if (DisplayType.Search == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.lookup;
		}
		else if (DisplayType.List == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else if (DisplayType.TableDir == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else if (DisplayType.Table == displayType)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else if (DisplayType.isAnyLookup(displayType))
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.lookup;
		}
		else if (DisplayType.Button == displayType && adReferenceValueId > 0)
		{
			return DocumentLayoutElementFieldDescriptor.LookupSource.list;
		}
		else
		{
			return null;
		}
	}

}
