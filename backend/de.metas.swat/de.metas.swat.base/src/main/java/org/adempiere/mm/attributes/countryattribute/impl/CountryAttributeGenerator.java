package org.adempiere.mm.attributes.countryattribute.impl;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2015 metas GmbH
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


import java.util.Properties;

import org.adempiere.mm.attributes.api.IAttributeSet;
import org.adempiere.mm.attributes.countryattribute.ICountryAttributeDAO;
import org.adempiere.mm.attributes.spi.AbstractAttributeValueGenerator;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_Country;
import org.compiere.model.I_M_Attribute;
import org.compiere.model.I_M_AttributeValue;
import org.compiere.model.X_M_Attribute;
import org.compiere.util.Env;

public class CountryAttributeGenerator extends AbstractAttributeValueGenerator
{

	@Override
	public String getAttributeValueType()
	{
		return X_M_Attribute.ATTRIBUTEVALUETYPE_List;
	}

	/**
	 * @return <code>false</code>, because "country" is a List attribute.
	 */
	@Override
	public boolean canGenerateValue(Properties ctx, IAttributeSet attributeSet, I_M_Attribute attribute)
	{
		return false;
	}

	@Override
	public I_M_AttributeValue generateAttributeValue(Properties ctx, int tableId, int recordId, boolean isSOTrx, String trxName)
	{
		Check.assume(I_C_Country.Table_ID == tableId, "Wrong table.");
		final I_C_Country country = InterfaceWrapperHelper.create(ctx, recordId, I_C_Country.class, trxName);
		final I_M_AttributeValue countryValue = InterfaceWrapperHelper.create(ctx, I_M_AttributeValue.class, trxName);

		countryValue.setM_Attribute_ID(Services.get(ICountryAttributeDAO.class).retrieveCountryAttributeId(Env.getAD_Client_ID(ctx), Env.getAD_Org_ID(ctx)));
		countryValue.setName(country.getName());
		countryValue.setValue(country.getCountryCode());
		InterfaceWrapperHelper.save(countryValue);

		return countryValue;
	}
}
