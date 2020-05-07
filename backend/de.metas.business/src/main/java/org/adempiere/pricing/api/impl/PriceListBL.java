package org.adempiere.pricing.api.impl;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.sql.Timestamp;
import java.util.Iterator;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.pricing.api.IPriceListBL;
import org.adempiere.pricing.api.IPriceListDAO;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_Country;
import org.compiere.model.I_M_PriceList;
import org.compiere.model.I_M_PriceList_Version;
import org.compiere.model.I_M_PricingSystem;

public class PriceListBL implements IPriceListBL
{
	@Override
	public I_M_PriceList getCurrentPricelistOrNull(final I_M_PricingSystem pricingSystem,
			final I_C_Country country,
			final Timestamp date,
			final boolean isSoTrx)
	{
		final I_M_PriceList_Version currentVersion = getCurrentPriceListVersionOrNull(pricingSystem, country, date, isSoTrx, null);

		if (currentVersion == null)
		{
			return null;
		}

		final I_M_PriceList currentPricelist = InterfaceWrapperHelper.create(currentVersion.getM_PriceList(), I_M_PriceList.class);
		return currentPricelist;
	}

	@Override
	public I_M_PriceList_Version getCurrentPriceListVersionOrNull(final I_M_PricingSystem pricingSystem,
			final I_C_Country country,
			final Timestamp date,
			final boolean isSoTrx,
			final Boolean processedPLVFiltering)
	{
		Check.assumeNotNull(date, "Param 'date' is not null; other params: country={}, isSoTrx={}, processedPLVFiltering={}", country, isSoTrx, processedPLVFiltering);

		if (country == null)
		{
			return null;
		}

		if (pricingSystem == null)
		{
			return null;
		}

		final IPriceListDAO priceListDAO = Services.get(IPriceListDAO.class);
		final Iterator<I_M_PriceList> pricelists = priceListDAO.retrievePriceLists(pricingSystem, country, isSoTrx);

		if (pricelists == null)
		{
			return null;
		}

		// This will be the most "fresh" pricelist (check the closest dateFrom)
		I_M_PriceList currentPricelist = null;

		Timestamp currentValidFrom = null;
		I_M_PriceList_Version lastPriceListVersion = null;

		if (pricelists.hasNext())
		{
			currentPricelist = pricelists.next();

			lastPriceListVersion = priceListDAO.retrievePriceListVersionOrNull(currentPricelist, date, processedPLVFiltering);

			if (lastPriceListVersion != null)
			{
				currentValidFrom = lastPriceListVersion.getValidFrom();
			}
		}

		while (pricelists.hasNext())
		{
			final I_M_PriceList priceListToCheck = pricelists.next();

			final I_M_PriceList_Version plvToCkeck = priceListDAO.retrievePriceListVersionOrNull(priceListToCheck, date, processedPLVFiltering);

			if (plvToCkeck == null)
			{
				// there may the case of no version fitting our requirements
				continue;
			}
			final Timestamp dateToCheck = plvToCkeck.getValidFrom();

			if (currentValidFrom.before(dateToCheck))
			{
				currentPricelist = priceListToCheck;
				currentValidFrom = dateToCheck;
				lastPriceListVersion = plvToCkeck;

			}
		}

		return lastPriceListVersion;
	}
}
