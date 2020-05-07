package de.metas.dunning.invoice.api;

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


import java.util.Properties;

import org.adempiere.util.ILoggable;
import org.adempiere.util.ISingletonService;
import org.compiere.model.I_C_Invoice;

import de.metas.dunning.interfaces.I_C_Dunning;

public interface IInvoiceSourceBL extends ISingletonService
{
	
	/**
	 * This event is fired right after the invoice referenced by a given dunning-candidate has been written off. Note that only invoices can be written off, so users of this event can assume that
	 * the document referenced by a candidate is allways an invoice.
	 */
	String EVENT_AfterInvoiceWriteOff = IInvoiceSourceBL.class.getName() + "#AfterInvoiceWriteOff";
	
	enum DunningDocLineSourceEvent
	{
	}

	/**
	 * Sets the invoice's dunning grace if it has a dunning with "Automatic dunning grace."
	 * 
	 * NOTE: this method is not saving the invoice.
	 * 
	 * @param invoice
	 * @return true if invoice was updated
	 */
	boolean setDunningGraceIfAutomatic(I_C_Invoice invoice);

	/**
	 * Gets the propper dunning for the given invoice. <br>
	 * BP dunning has priority, after that BP Group dunning, lastly Org dunning.
	 * 
	 * @param invoice
	 * @return
	 */
	I_C_Dunning getDunningForInvoiceOrNull(I_C_Invoice invoice);

	/**
	 * Does a mass writeoff for unprocessed dunning candidates that belong to a writeoff dunning level
	 * 
	 * @param ctx
	 * @param writeOffDescription
	 * @param monitor Higher level logger.
	 * @return
	 */
	int writeOffDunningDocs(Properties ctx, String writeOffDescription, ILoggable monitor);
}
