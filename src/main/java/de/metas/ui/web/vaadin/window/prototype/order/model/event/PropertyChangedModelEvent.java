package de.metas.ui.web.vaadin.window.prototype.order.model.event;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import de.metas.ui.web.vaadin.window.prototype.order.PropertyName;

/*
 * #%L
 * de.metas.ui.web.vaadin
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

public class PropertyChangedModelEvent extends ModelEvent
{
	public static final PropertyChangedModelEvent of(final Object model, final PropertyName propertyName, final Object value, final Object valueOld)
	{
		return new PropertyChangedModelEvent(model, propertyName, value, valueOld);
	}

	private final PropertyName propertyName;
	private final Object value;
	private final Object valueOld;

	private PropertyChangedModelEvent(final Object model, final PropertyName propertyName, final Object value, final Object valueOld)
	{
		super(model);
		this.propertyName = Preconditions.checkNotNull(propertyName, "propertyName not null");
		this.value = value;
		this.valueOld = valueOld;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("propertyName", propertyName)
				.add("value", value)
				.add("valueOld", valueOld)
				.toString();
	}

	public PropertyName getPropertyName()
	{
		return propertyName;
	}

	public Object getValue()
	{
		return value;
	}

	public Object getValueOld()
	{
		return valueOld;
	}
}
