package de.metas.payment.esr.api.impl;

/*
 * #%L
 * de.metas.payment.esr
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
import org.compiere.util.Util;

import de.metas.payment.esr.api.IBPBankAccountBL;
import de.metas.payment.esr.model.I_C_BP_BankAccount;
import de.metas.payment.esr.model.I_C_Bank;

public class BPBankAccountBL implements IBPBankAccountBL
{
	@Override
	public String retrieveBankAccountNo(final I_C_BP_BankAccount bankAccount)
	{
		final I_C_Bank bank = bankAccount.getC_Bank();
		if (bank.isESR_PostBank())
		{
			return "000000";
		}
		else
		{
			return bankAccount.getAccountNo();
		}
	}

	@Override
	public String retrieveESRAccountNo(final I_C_BP_BankAccount bankAccount)
	{
		Check.assume(bankAccount.isEsrAccount(), bankAccount + " has IsEsrAccount=Y");

		final String renderedNo = bankAccount.getESR_RenderedAccountNo();
		Check.assume(!Check.isEmpty(renderedNo, true), bankAccount + " has a ESR_RenderedAccountNo");
		
		if (!renderedNo.contains("-"))
		{
			// task 07789: the rendered number is not "rendered" to start with. This happens e.g. if the number was parsed from an ESR payment string.
			return renderedNo;
		}

		final String[] renderenNoComponents = renderedNo.split("-");
		Check.assume(renderenNoComponents.length == 3, renderedNo + " contains three '-' separated parts");

		final StringBuilder sb = new StringBuilder();
		sb.append(renderenNoComponents[0]);
		sb.append(Util.lpadZero(renderenNoComponents[1], 6, "middle section of " + renderedNo));
		sb.append(renderenNoComponents[2]);

		return sb.toString();

	}
}
