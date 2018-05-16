package de.metas.ui.web.order.sales.pricingConditions.view;

import java.math.BigDecimal;

import org.adempiere.util.Check;
import org.adempiere.util.NumberUtils;

import de.metas.ui.web.window.datatypes.LookupValue;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2018 metas GmbH
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

@Value
public class Price
{
	public static Price none()
	{
		return NONE;
	}

	public static Price basePricingSystem(@NonNull final LookupValue pricingSystem, @NonNull final BigDecimal basePriceAddAmt)
	{
		return new Price(PriceType.BASE_PRICING_SYSTEM, /* priceValue */null, pricingSystem, basePriceAddAmt);
	}

	public static Price fixedPrice(@NonNull final BigDecimal priceValue)
	{
		return new Price(PriceType.FIXED_PRICE, priceValue, /* pricingSystem */null, /* basePriceAddAmt */null);
	}

	public static Price fixedZeroPrice()
	{
		return fixedPrice(BigDecimal.ZERO);
	}

	private static final Price NONE = new Price();

	PriceType priceType;

	LookupValue basePricingSystem;
	BigDecimal basePriceAddAmt;

	BigDecimal fixedPrice;

	private Price()
	{
		priceType = PriceType.NONE;
		fixedPrice = null;
		basePricingSystem = null;
		basePriceAddAmt = null;
	}

	private Price(
			@NonNull final PriceType priceType,
			final BigDecimal fixedPrice,
			final LookupValue basePricingSystem,
			final BigDecimal basePriceAddAmt)
	{
		this.priceType = priceType;

		if (priceType.isFixedPrice() && fixedPrice == null)
		{
			throw new NullPointerException("priceValue shall not be null when priceType=" + priceType);
		}
		this.fixedPrice = NumberUtils.stripTrailingDecimalZeros(fixedPrice);

		if (priceType.isBasePricingSystem() && basePricingSystem == null)
		{
			throw new NullPointerException("pricingSystem shall not be null when priceType=" + priceType);
		}
		this.basePricingSystem = basePricingSystem;
		this.basePriceAddAmt = NumberUtils.stripTrailingDecimalZeros(basePriceAddAmt);
	}

	public boolean isNoPrice()
	{
		return priceType == PriceType.NONE;
	}

	public int getPricingSystemId()
	{
		Check.assumeNotNull(basePricingSystem, "No pricing system for {}", this);
		return basePricingSystem.getIdAsInt();
	}
}
