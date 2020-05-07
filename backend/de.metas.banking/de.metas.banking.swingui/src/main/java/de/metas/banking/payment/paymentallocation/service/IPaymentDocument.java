package de.metas.banking.payment.paymentallocation.service;

/*
 * #%L
 * de.metas.banking.swingui
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


import java.math.BigDecimal;

import org.adempiere.util.lang.ITableRecordReference;

public interface IPaymentDocument
{
	int getC_BPartner_ID();

	String getDocumentNo();

	boolean isCustomerDocument();

	boolean isVendorDocument();
	
	ITableRecordReference getReference();

	BigDecimal getAmountToAllocateInitial();

	BigDecimal getAmountToAllocate();
	
	int getC_Currency_ID();

	void addAllocatedAmt(BigDecimal allocatedAmtToAdd);

	/**
	 * @return true if everything that was requested to be allocated, was allocated
	 */
	boolean isFullyAllocated();

	BigDecimal calculateProjectedOverUnderAmt(final BigDecimal amountToAllocate);

	boolean canPay(IPayableDocument payable);
}
