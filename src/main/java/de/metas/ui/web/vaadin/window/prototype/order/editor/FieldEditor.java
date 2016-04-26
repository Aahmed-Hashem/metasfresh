package de.metas.ui.web.vaadin.window.prototype.order.editor;

import java.util.Collection;

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Label;

import de.metas.ui.web.vaadin.window.prototype.order.PropertyDescriptor;

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

@SuppressWarnings("serial")
public abstract class FieldEditor<T> extends AbstractEditor
implements Field<T>
{
	static final String STYLE_Field = "mf-editor-field";
	private final AbstractField<T> valueField;
	private Label label;

	public FieldEditor(final PropertyDescriptor descriptor)
	{
		super(descriptor);
		addStyleName(STYLE_Field);

		valueField = createValueField();
		valueField.addStyleName(STYLE_ValueField);
		valueField.setCaption(descriptor.getPropertyName().toString());
		
		final Component content = valueField;
		content.setSizeFull();
		setCompositionRoot(content);

		valueField.addValueChangeListener(new Property.ValueChangeListener()
		{

			@Override
			public void valueChange(final Property.ValueChangeEvent event)
			{
				listener().valueChange(getPropertyName(), getValue());
			}
		});
	}

	protected abstract AbstractField<T> createValueField();
	
	@Override
	public void setValue(final Object value)
	{
		final T valueView = convertToView(value);
		valueField.setValue(valueView);
	}

	protected abstract T convertToView(final Object valueObj);

	@Override
	public T getValue()
	{
		final T value = valueField.getValue();
		return value;
	}

	@Override
	public void addChildEditor(de.metas.ui.web.vaadin.window.prototype.order.editor.Editor editor)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Label getLabel()
	{
		if(label == null)
		{
			label = new Label(getCaption());
		}
		return label;
	}
	
	@Override
	public void focus()
	{
		valueField.focus();
	}

	@Override
	public boolean isInvalidCommitted()
	{
		return valueField.isInvalidCommitted();
	}

	@Override
	public void setInvalidCommitted(boolean isCommitted)
	{
		valueField.setInvalidCommitted(isCommitted);
	}

	@Override
	public void commit() throws SourceException, InvalidValueException
	{
		valueField.commit();
	}

	@Override
	public void discard() throws SourceException
	{
		valueField.discard();
	}

	@Override
	public void setBuffered(boolean buffered)
	{
		valueField.setBuffered(buffered);
	}

	@Override
	public boolean isBuffered()
	{
		return valueField.isBuffered();
	}

	@Override
	public boolean isModified()
	{
		return valueField.isModified();
	}

	@Override
	public void addValidator(Validator validator)
	{
		valueField.addValidator(validator);
	}

	@Override
	public void removeValidator(Validator validator)
	{
		valueField.removeValidator(validator);
	}

	@Override
	public void removeAllValidators()
	{
		valueField.removeAllValidators();
	}

	@Override
	public Collection<Validator> getValidators()
	{
		return valueField.getValidators();
	}

	@Override
	public boolean isValid()
	{
		return valueField.isValid();
	}

	@Override
	public void validate() throws InvalidValueException
	{
		valueField.validate();
	}

	@Override
	public boolean isInvalidAllowed()
	{
		return valueField.isInvalidAllowed();
	}

	@Override
	public void setInvalidAllowed(boolean invalidValueAllowed) throws UnsupportedOperationException
	{
		valueField.setInvalidAllowed(true);
	}

	@Override
	public Class<? extends T> getType()
	{
		//TODO
		return null;
//		final Class<? extends IT> type = valueField.getType();
//		return type;
	}

	@Override
	public void addValueChangeListener(com.vaadin.data.Property.ValueChangeListener listener)
	{
		valueField.addValueChangeListener(listener);
	}

	@Override
	@Deprecated
	public void addListener(com.vaadin.data.Property.ValueChangeListener listener)
	{
		valueField.addListener(listener);
	}

	@Override
	public void removeValueChangeListener(com.vaadin.data.Property.ValueChangeListener listener)
	{
		valueField.removeValueChangeListener(listener);
	}

	@Override
	@Deprecated
	public void removeListener(com.vaadin.data.Property.ValueChangeListener listener)
	{
		valueField.removeListener(listener);
	}

	@Override
	public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
	{
		valueField.valueChange(event);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void setPropertyDataSource(Property newDataSource)
	{
		valueField.setPropertyDataSource(newDataSource);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Property getPropertyDataSource()
	{
		return valueField.getPropertyDataSource();
	}

	@Override
	public int getTabIndex()
	{
		return valueField.getTabIndex();
	}

	@Override
	public void setTabIndex(int tabIndex)
	{
		valueField.setTabIndex(tabIndex);
	}

	@Override
	public boolean isRequired()
	{
		return valueField.isRequired();
	}

	@Override
	public void setRequired(boolean required)
	{
		valueField.setRequired(required);
	}

	@Override
	public void setRequiredError(String requiredMessage)
	{
		valueField.setRequiredError(requiredMessage);
	}

	@Override
	public String getRequiredError()
	{
		return valueField.getRequiredError();
	}

	@Override
	public boolean isEmpty()
	{
		return valueField.isEmpty();
	}

	@Override
	public void clear()
	{
		valueField.clear();
	}
}
