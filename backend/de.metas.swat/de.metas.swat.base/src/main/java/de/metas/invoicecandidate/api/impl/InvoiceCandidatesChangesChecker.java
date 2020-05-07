package de.metas.invoicecandidate.api.impl;

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


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.ILoggable;
import org.adempiere.util.NullLoggable;
import org.adempiere.util.text.TokenizedStringBuilder;

import de.metas.invoicecandidate.api.IInvoiceCandidatesChangesChecker;
import de.metas.invoicecandidate.model.I_C_Invoice_Candidate;

public class InvoiceCandidatesChangesChecker implements IInvoiceCandidatesChangesChecker
{
	private ILoggable logger = NullLoggable.instance;
	private Map<Integer, InvoiceCandidateInfo> _infosBeforeChanges = null;
	private BigDecimal totalNetAmtToInvoiceChecksum = null;

	@Override
	public InvoiceCandidatesChangesChecker setLogger(final ILoggable logger)
	{
		Check.assumeNotNull(logger, "logger not null");
		this.logger = logger;
		return this;
	}

	@Override
	public InvoiceCandidatesChangesChecker setTotalNetAmtToInvoiceChecksum(BigDecimal totalNetAmtToInvoiceChecksum)
	{
		this.totalNetAmtToInvoiceChecksum = totalNetAmtToInvoiceChecksum;
		return this;
	}

	@Override
	public InvoiceCandidatesChangesChecker setBeforeChanges(final Iterable<I_C_Invoice_Candidate> candidates)
	{
		_infosBeforeChanges = createCheckInfo(candidates);
		return this;
	}

	private final Map<Integer, InvoiceCandidateInfo> getInfosBeforeChanges()
	{
		Check.assumeNotNull(_infosBeforeChanges, "infosBeforeChanges shall be set");
		return new HashMap<>(_infosBeforeChanges);
	}

	@Override
	public void assertNoChanges(final Iterable<I_C_Invoice_Candidate> candidates)
	{
		final Map<Integer, InvoiceCandidateInfo> infosAfterChanges = createCheckInfo(candidates);
		final Map<Integer, InvoiceCandidateInfo> infosBeforeChanges = getInfosBeforeChanges();

		boolean hasChanges = false;

		for (final InvoiceCandidateInfo infoAfterChange : infosAfterChanges.values())
		{
			final int invoiceCandidateId = infoAfterChange.getC_Invoice_Candidate_ID();
			final InvoiceCandidateInfo infoBeforeChange = infosBeforeChanges.remove(invoiceCandidateId);

			if (checkHasChanges(infoAfterChange, infoBeforeChange))
			{
				hasChanges = true;
			}
		}

		for (final InvoiceCandidateInfo infoBeforeChange : infosBeforeChanges.values())
		{
			final InvoiceCandidateInfo infoAfterChange = null;
			checkHasChanges(infoAfterChange, infoBeforeChange);
			hasChanges = true;
		}

		if (hasChanges)
		{
			throw new AdempiereException("@HasChanges@");
		}
	}

	/**
	 * Checks if there are changes between initial version or current version of an invoice candidate.
	 * 
	 * @param infoAfterChange
	 * @param infoBeforeChange
	 * @return true if changes found
	 */
	private boolean checkHasChanges(final InvoiceCandidateInfo infoAfterChange, final InvoiceCandidateInfo infoBeforeChange)
	{
		//
		// Case: Invoice candidate present in news list but missing in olds list
		// (shall not happen)
		if (infoBeforeChange == null)
		{
			logger.addLog("Missing(old): " + infoAfterChange);
			return true;
		}

		//
		// Case: Invoice candidate missing in news list but present in olds list
		// (shall not happen)
		if (infoAfterChange == null)
		{
			logger.addLog("Missing(new): " + infoBeforeChange);
			return true;
		}

		//
		// Case: we have the old version and the new version
		// => check if they are equal
		if (!infoBeforeChange.checkEquals(infoAfterChange, logger))
		{
			return true;
		}

		return false; // no changes
	}

	/**
	 * Extract relevant informations from candidates.
	 * 
	 * Only those informations that were extracted are relevant for checking if an invoice candidate has changed
	 * 
	 * @return map of C_Invoice_Candidate_ID to "invoice candidate relevant infos POJO"
	 */
	private final Map<Integer, InvoiceCandidateInfo> createCheckInfo(final Iterable<I_C_Invoice_Candidate> candidates)
	{
		Check.assumeNotNull(candidates, "candidates not null");

		final ICNetAmtToInvoiceChecker totalNetAmtToInvoiceChecker = new ICNetAmtToInvoiceChecker()
				.setLoggable(logger)
				.setNetAmtToInvoiceExpected(totalNetAmtToInvoiceChecksum);

		final Map<Integer, InvoiceCandidateInfo> invoiceCandidateId2info = new HashMap<>();
		for (final I_C_Invoice_Candidate ic : candidates)
		{
			final InvoiceCandidateInfo icInfo = new InvoiceCandidateInfo(ic);
			invoiceCandidateId2info.put(icInfo.getC_Invoice_Candidate_ID(), icInfo);

			totalNetAmtToInvoiceChecker.add(ic);
		}

		totalNetAmtToInvoiceChecker.assertExpectedNetAmtToInvoiceIfSet();

		return invoiceCandidateId2info;
	}

	/**
	 * POJO containing the informations which are relevant for change tracking.
	 */
	private static final class InvoiceCandidateInfo
	{
		private final int invoiceCandidateId;
		private final BigDecimal netAmtToInvoice;
		private final int taxId;

		public InvoiceCandidateInfo(final I_C_Invoice_Candidate ic)
		{
			super();
			Check.assumeNotNull(ic, "ic not null");

			invoiceCandidateId = ic.getC_Invoice_Candidate_ID();
			netAmtToInvoice = ic.getNetAmtToInvoice();

			final int taxId = ic.getC_Tax_ID();
			this.taxId = taxId <= 0 ? -1 : taxId;
		}

		@Override
		public String toString()
		{
			return invoiceCandidateId
					+ ": "
					+ "@NetAmtToInvoice@: " + netAmtToInvoice
					+ ", @C_Tax_ID@: " + taxId
			//
			;
		}

		/**
		 * Compares this invoice candidate info (old version) object with given info (new version) and logs if there are any differences.
		 * 
		 * @param infoAfterChange
		 * @param logger
		 * @return <code>true</code> if the objects are equal.
		 */
		public boolean checkEquals(final InvoiceCandidateInfo infoAfterChange, final ILoggable logger)
		{
			final InvoiceCandidateInfo infoBeforeChange = this;

			Check.assume(infoAfterChange.getC_Invoice_Candidate_ID() == infoBeforeChange.getC_Invoice_Candidate_ID(),
					"Old info {} and New info {} shall share the same C_Invoice_Candidate_ID", infoBeforeChange, infoAfterChange);
			boolean hasChanges = false;
			final TokenizedStringBuilder changesInfo = new TokenizedStringBuilder(", ");
			if (infoAfterChange.getLineNetAmt().compareTo(infoBeforeChange.getLineNetAmt()) != 0)
			{
				changesInfo.append("@LineNetAmt@: " + infoBeforeChange.getLineNetAmt() + "->" + infoAfterChange.getLineNetAmt());
				hasChanges = true;
			}
			if (infoAfterChange.getC_Tax_ID() != infoBeforeChange.getC_Tax_ID())
			{
				changesInfo.append("@C_Tax_ID@: " + infoBeforeChange.getC_Tax_ID() + "->" + infoAfterChange.getC_Tax_ID());
				hasChanges = true;
			}

			if (hasChanges)
			{
				logger.addLog(infoAfterChange.getC_Invoice_Candidate_ID() + ": " + changesInfo);
			}

			return !hasChanges;
		}

		public int getC_Invoice_Candidate_ID()
		{
			return invoiceCandidateId;
		}

		public BigDecimal getLineNetAmt()
		{
			return netAmtToInvoice;
		}

		public int getC_Tax_ID()
		{
			return taxId;
		}
	}

}
