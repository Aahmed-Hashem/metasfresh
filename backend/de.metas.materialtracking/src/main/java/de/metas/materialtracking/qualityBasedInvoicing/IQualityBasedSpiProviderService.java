package de.metas.materialtracking.qualityBasedInvoicing;

/*
 * #%L
 * de.metas.materialtracking
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


import org.adempiere.util.ISingletonService;

import de.metas.materialtracking.qualityBasedInvoicing.spi.IInvoicedSumProvider;
import de.metas.materialtracking.qualityBasedInvoicing.spi.IQualityBasedConfigProvider;
import de.metas.materialtracking.qualityBasedInvoicing.spi.IQualityInvoiceLineGroupsBuilderProvider;

public interface IQualityBasedSpiProviderService extends ISingletonService
{
	/**
	 *
	 * @return config provider; never return <code>null</code>
	 */
	IQualityBasedConfigProvider getQualityBasedConfigProvider();

	void setQualityBasedConfigProvider(IQualityBasedConfigProvider provider);

	/**
	 *
	 * @return invoice line group builder provider; never return <code>null</code>
	 */
	IQualityInvoiceLineGroupsBuilderProvider getQualityInvoiceLineGroupsBuilderProvider();

	void setQualityInvoiceLineGroupsBuilderProvider(IQualityInvoiceLineGroupsBuilderProvider qualityInvoiceLineGroupsBuilderProvider);

	/**
	 * Allows to register a provider that will for a given material tracking return the amount that was already invoiced so far.
	 * <p>
	 * Used to get the already paid downpayment amounts when it is time for the final settlement.
	 */
	void setInvoicedSumProvider(IInvoicedSumProvider provider);

	IInvoicedSumProvider getInvoicedSumProvider();

}
