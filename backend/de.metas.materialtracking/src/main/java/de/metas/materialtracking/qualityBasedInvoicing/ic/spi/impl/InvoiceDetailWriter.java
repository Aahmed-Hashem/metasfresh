package de.metas.materialtracking.qualityBasedInvoicing.ic.spi.impl;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.pricing.api.IPricingResult;
import org.adempiere.util.Check;
import org.adempiere.util.Services;

import de.metas.invoicecandidate.model.I_C_Invoice_Candidate;
import de.metas.materialtracking.model.I_C_Invoice_Detail;
import de.metas.materialtracking.qualityBasedInvoicing.invoicing.IQualityInvoiceLine;
import de.metas.materialtracking.spi.IHandlingUnitsInfoFactory;

/**
 * Takes {@link IQualityInvoiceLine}s and converts them to {@link I_C_Invoice_Detail}s.
 *
 * @author tsa
 *
 */
public class InvoiceDetailWriter
{
	// services
	private final IHandlingUnitsInfoFactory handlingUnitsInfoFactory = Services.get(IHandlingUnitsInfoFactory.class);

	// Parameters
	private final IContextAware _context;
	private final I_C_Invoice_Candidate _invoiceCandidate;
	private Boolean _printBefore = null;
	private Boolean _printOverride = false;

	// State
	private int _seqNoNext = 10;
	private final List<I_C_Invoice_Detail> _createdLines = new ArrayList<>();

	public InvoiceDetailWriter(final I_C_Invoice_Candidate invoiceCandidate)
	{
		super();

		Check.assumeNotNull(invoiceCandidate, "invoiceCandidate not null");
		_invoiceCandidate = invoiceCandidate;
		_context = InterfaceWrapperHelper.getContextAware(invoiceCandidate);
	}

	private I_C_Invoice_Candidate getC_Invoice_Candidate()
	{
		return _invoiceCandidate;
	}

	private IContextAware getContext()
	{
		return _context;
	}

	public boolean isPrintBefore()
	{
		Check.assumeNotNull(_printBefore, "printBefore not null");
		return _printBefore;
	}

	/**
	 *
	 * @param printBefore
	 */
	public void setPrintBefore(final boolean printBefore)
	{
		_printBefore = printBefore;
	}

	public boolean isPrintOverride()
	{
		return _printOverride;
	}

	/**
	 * If set to <code>true</code>, then the following invocations of {@link #saveLines(Collection)} will be store their {@link org.adempiere.model.I_C_Invoice_Detail I_C_Invoice_Detail} records
	 * with {@link org.adempiere.model.I_C_Invoice_Detail#COLUMN_IsDetailOverridesLine IsDetailOverridesLine} to <code>true</code>.
	 * <p>
	 * Note: will also set {@link org.adempiere.model.I_C_Invoice_Detail#COLUMN_IsPrintBefore IsPrintBefore} to <code>true</code>, but that shouldn't matter.
	 *
	 * @param printOverride
	 */
	public void setPrintOverride(final boolean printOverride)
	{
		_printOverride = printOverride;
		_printBefore = true;
	}

	/**
	 *
	 * @param lines
	 */
	public void saveLines(final Collection<IQualityInvoiceLine> lines)
	{
		if (lines == null || lines.isEmpty())
		{
			return;
		}

		//
		// Iterate lines and save one by one
		for (final IQualityInvoiceLine line : lines)
		{
			saveLine(line);
		}
	}

	private void saveLine(final IQualityInvoiceLine line)
	{
		Check.assumeNotNull(line, "line not null");

		final I_C_Invoice_Candidate invoiceCandidate = getC_Invoice_Candidate();
		final int seqNo = _seqNoNext;

		// Pricing
		final IPricingResult pricingResult = line.getPrice();
		final int priceUOMId;
		final BigDecimal price;
		final BigDecimal discount;
		if (pricingResult != null)
		{
			priceUOMId = pricingResult.getPrice_UOM_ID();
			price = pricingResult.getPriceStd();
			discount = pricingResult.getDiscount();
		}
		else
		{
			priceUOMId = -1;
			price = null;
			discount = null;
		}

		final I_C_Invoice_Detail invoiceDetail = InterfaceWrapperHelper.newInstance(I_C_Invoice_Detail.class, getContext());
		invoiceDetail.setAD_Org_ID(invoiceCandidate.getAD_Org_ID());
		invoiceDetail.setC_Invoice_Candidate(invoiceCandidate);
		invoiceDetail.setSeqNo(seqNo);
		invoiceDetail.setIsActive(true);
		invoiceDetail.setIsDetailOverridesLine(isPrintOverride());
		invoiceDetail.setIsPrinted(isPrintOverride() ? true : line.isDisplayed()); // the override-details line is always printed
		invoiceDetail.setDescription(line.getDescription());
		invoiceDetail.setIsPrintBefore(isPrintBefore());
		invoiceDetail.setM_Product(line.getM_Product());
		invoiceDetail.setNote(line.getProductName());
		// invoiceDetail.setM_AttributeSetInstance(M_AttributeSetInstance);
		invoiceDetail.setC_UOM(line.getC_UOM());
		invoiceDetail.setDiscount(discount);
		invoiceDetail.setPrice_UOM_ID(priceUOMId);
		invoiceDetail.setPriceEntered(price);
		invoiceDetail.setPriceActual(price);
		invoiceDetail.setQty(line.getQty());
		invoiceDetail.setQtyEnteredInPriceUOM(line.getQty());
		invoiceDetail.setPercentage(line.getPercentage());

		invoiceDetail.setPP_Order(line.getPP_Order());

		// Set Handling Units specific infos
		handlingUnitsInfoFactory.updateInvoiceDetail(invoiceDetail, line.getHandlingUnitsInfo());

		//
		// Save detail line
		InterfaceWrapperHelper.save(invoiceDetail);
		I_C_Invoice_Detail.DYNATTR_C_Invoice_Detail_IQualityInvoiceLine.setValue(invoiceDetail, line);

		_createdLines.add(invoiceDetail);
		_seqNoNext += 10;
	}
}
