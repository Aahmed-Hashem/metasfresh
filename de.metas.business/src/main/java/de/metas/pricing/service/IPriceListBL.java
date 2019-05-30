package de.metas.pricing.service;

import java.time.LocalDate;

import de.metas.location.CountryId;
import org.compiere.model.I_M_PriceList;
import org.compiere.model.I_M_PriceList_Version;

import de.metas.currency.CurrencyPrecision;
import de.metas.lang.SOTrx;
import de.metas.pricing.PriceListId;
import de.metas.pricing.PricingSystemId;
import de.metas.util.ISingletonService;

import javax.annotation.Nullable;

/**
 * @author RC
 */
public interface IPriceListBL extends ISingletonService
{
	CurrencyPrecision getPricePrecision(@Nullable PriceListId priceListId);

	default CurrencyPrecision getPricePrecision(final int priceListId)
	{
		return getPricePrecision(PriceListId.ofRepoIdOrNull(priceListId));
	}

	/**
	 * @return the current price list for vendor if any (for the giver pricing system), null otherwise
	 */
	I_M_PriceList getCurrentPricelistOrNull(
			PricingSystemId pricingSystemId,
			CountryId countryId,
			LocalDate date,
			SOTrx soTrx);

	default CurrencyPrecision getPrecisionForLineNetAmount(int priceListId)
	{
		return getPrecisionForLineNetAmount(PriceListId.ofRepoIdOrNull(priceListId));
	}

	CurrencyPrecision getPrecisionForLineNetAmount(PriceListId priceListId);

	/**
	 * Find the current version from a pricing system based on the given parameters.
	 *
	 * @param soTrx                 SO/PO or null
	 * @param processedPLVFiltering if not <code>null</code>, then only PLVs which have the give value in their <code>Processed</code> column are considered.
	 *                              task 09533: the user doesn't know about PLV's processed flag, so in most cases we can't filter by it
	 */
	@Nullable I_M_PriceList_Version getCurrentPriceListVersionOrNull(
			PricingSystemId pricingSystemId,
			CountryId countryId,
			LocalDate date,
			@Nullable SOTrx soTrx,
			Boolean processedPLVFiltering);
}
