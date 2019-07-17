package de.metas.currency;

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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.Properties;

import org.adempiere.service.ClientId;
import org.compiere.model.I_C_Currency;
import org.compiere.util.TimeUtil;

import de.metas.currency.exceptions.NoCurrencyRateFoundException;
import de.metas.money.CurrencyConversionTypeId;
import de.metas.money.CurrencyId;
import de.metas.organization.OrgId;
import de.metas.util.ISingletonService;
import lombok.NonNull;

/**
 * Currency conversion services.
 *
 * @author tsa
 *
 */
public interface ICurrencyBL extends ISingletonService
{
	CurrencyConversionContext createCurrencyConversionContext(Date ConvDate, CurrencyConversionTypeId ConversionType_ID, int AD_Client_ID, int AD_Org_ID);

	default CurrencyConversionContext createCurrencyConversionContext(
			final LocalDate ConvDate,
			final CurrencyConversionTypeId ConversionType_ID,
			@NonNull final ClientId clientId,
			@NonNull final OrgId orgId)
	{
		return createCurrencyConversionContext(TimeUtil.asDate(ConvDate), ConversionType_ID, clientId.getRepoId(), orgId.getRepoId());
	}

	CurrencyConversionContext createCurrencyConversionContext(Date ConvDate, ConversionType conversionType, int AD_Client_ID, int AD_Org_ID);

	/**
	 * @return base currency of AD_Client and AD_Org which are set in context.
	 */
	I_C_Currency getBaseCurrency(Properties ctx);

	/**
	 * @return base currency of given client and org
	 */
	I_C_Currency getBaseCurrency(ClientId adClientId, OrgId adOrgId);

	/**
	 * @return base currency ID of given client and org
	 */
	CurrencyId getBaseCurrencyId(ClientId adClientId, OrgId adOrgId);

	/**
	 * Convert an amount to base Currency
	 *
	 * @param ctx context
	 * @param CurFrom_ID The C_Currency_ID FROM
	 * @param ConvDate conversion date - if null - use current date
	 * @param C_ConversionType_ID conversion rate type - if 0 - use Default
	 * @param Amt amount to be converted
	 * @param AD_Client_ID client
	 * @param AD_Org_ID organization
	 * @return converted amount
	 */
	BigDecimal convertBase(Properties ctx, BigDecimal Amt, int CurFrom_ID, Timestamp ConvDate, int C_ConversionType_ID, int AD_Client_ID, int AD_Org_ID);

	/**
	 * Convert an amount
	 *
	 * @param ctx context
	 * @param CurFrom_ID The C_Currency_ID FROM
	 * @param CurTo_ID The C_Currency_ID TO
	 * @param ConvDate conversion date - if null - use current date
	 * @param C_ConversionType_ID conversion rate type - if 0 - use Default
	 * @param Amt amount to be converted
	 * @param AD_Client_ID client
	 * @param AD_Org_ID organization
	 * @return converted amount or null if no rate
	 */
	BigDecimal convert(Properties ctx, BigDecimal Amt, int CurFrom_ID, int CurTo_ID, Timestamp ConvDate, int C_ConversionType_ID, int AD_Client_ID, int AD_Org_ID);

	/**
	 * Convert an amount with today's default rate
	 *
	 * @param ctx context
	 * @param CurFrom_ID The C_Currency_ID FROM
	 * @param CurTo_ID The C_Currency_ID TO
	 * @param Amt amount to be converted
	 * @param AD_Client_ID client
	 * @param AD_Org_ID organization
	 * @return converted amount
	 */
	BigDecimal convert(Properties ctx, BigDecimal Amt, int CurFrom_ID, int CurTo_ID, int AD_Client_ID, int AD_Org_ID);

	CurrencyConversionResult convert(CurrencyConversionContext conversionCtx, BigDecimal Amt, int CurFrom_ID, int CurTo_ID);

	/**
	 * Get Currency Conversion Rate
	 *
	 * @param CurFrom_ID The C_Currency_ID FROM
	 * @param CurTo_ID The C_Currency_ID TO
	 * @param ConvDate The Conversion date - if null - use current date
	 * @param ConversionType_ID Conversion rate type - if 0 - use Default
	 * @param AD_Client_ID client
	 * @param AD_Org_ID organization
	 * @return currency Rate or null
	 */
	BigDecimal getRate(int CurFrom_ID, int CurTo_ID, Timestamp ConvDate, int ConversionType_ID, int AD_Client_ID, int AD_Org_ID);

	BigDecimal getRate(CurrencyConversionContext conversionCtx, int CurFrom_ID, int CurTo_ID);

	/**
	 *
	 * @param conversionCtx
	 * @param currencyFromId
	 * @param currencyToId
	 * @return currency rate; never returns null
	 * @throws NoCurrencyRateFoundException
	 */
	CurrencyRate getCurrencyRate(CurrencyConversionContext conversionCtx, int currencyFromId, int currencyToId);
}
