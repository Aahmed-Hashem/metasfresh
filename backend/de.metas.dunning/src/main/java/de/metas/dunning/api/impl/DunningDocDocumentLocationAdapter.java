package de.metas.dunning.api.impl;

/*
 * #%L
 * de.metas.dunning
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


import org.adempiere.util.Check;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_BPartner_Location;

import de.metas.document.model.IDocumentLocation;
import de.metas.dunning.model.I_C_DunningDoc;

public class DunningDocDocumentLocationAdapter implements IDocumentLocation
{
	private final I_C_DunningDoc delegate;

	public DunningDocDocumentLocationAdapter(I_C_DunningDoc delegate)
	{
		Check.assume(delegate != null, "delegate not null");
		this.delegate = delegate;
	}

	@Override
	public int getC_BPartner_ID()
	{
		return delegate.getC_BPartner_ID();
	}

	@Override
	public I_C_BPartner getC_BPartner()
	{
		return delegate.getC_BPartner();
	}

	@Override
	public int getC_BPartner_Location_ID()
	{
		return delegate.getC_BPartner_Location_ID();
	}

	@Override
	public I_C_BPartner_Location getC_BPartner_Location()
	{
		return delegate.getC_BPartner_Location();
	}

	@Override
	public int getAD_User_ID()
	{
		return delegate.getC_Dunning_Contact_ID();
	}

	@Override
	public I_AD_User getAD_User()
	{
		return delegate.getC_Dunning_Contact();
	}

	@Override
	public String getBPartnerAddress()
	{
		return delegate.getBPartnerAddress();
	}

	@Override
	public void setBPartnerAddress(String address)
	{
		delegate.setBPartnerAddress(address);

	}

}
