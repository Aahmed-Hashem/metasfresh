/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.acct;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import org.slf4j.Logger;
import de.metas.logging.LogManager;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMatchPO;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.ProductCost;
import org.compiere.model.X_M_InOut;
import org.compiere.util.Env;

import de.metas.interfaces.I_C_OrderLine;

/**
 *  Post MatchPO Documents.
 *  <pre>
 *  Table:              C_MatchPO (473)
 *  Document Types:     MXP
 *  </pre>
 *  @author Jorg Janke
 *  @version  $Id: Doc_MatchPO.java,v 1.3 2006/07/30 00:53:33 jjanke Exp $
 */
public class Doc_MatchPO extends Doc
{
	/** Shall we create accounting facts (08555) */
	private static final String SYSCONFIG_NoFactRecords = "org.compiere.acct.Doc_MatchPO.NoFactAccts";
	private static final boolean DEFAULT_NoFactRecords = true;

	/**
	 *  Constructor
	 * 	@param ass accounting schemata
	 * 	@param rs record
	 * 	@param trxName trx
	 */
	public Doc_MatchPO (final IDocBuilder docBuilder)
	{
		super(docBuilder, DOCTYPE_MatMatchPO);
	}   //  Doc_MatchPO

	private int         m_C_OrderLine_ID = 0;
	private MOrderLine	m_oLine = null;
	//
	private int         m_M_InOutLine_ID = 0;
	private MInOutLine	m_ioLine = null;
//	private int         m_C_InvoiceLine_ID = 0;
	
	private ProductCost m_pc;
	private int			m_M_AttributeSetInstance_ID = 0;
	
	/** Shall we create accounting facts? (08555) */
	private boolean noFactRecords = false;

	/**
	 *  Load Specific Document Details
	 *  @return error message or null
	 */
	@Override
	protected String loadDocumentDetails ()
	{
		setC_Currency_ID (Doc.NO_CURRENCY);
		MMatchPO matchPO = (MMatchPO)getPO();
		setDateDoc(matchPO.getDateTrx());
		//
		m_M_AttributeSetInstance_ID = matchPO.getM_AttributeSetInstance_ID();
		setQty (matchPO.getQty());
		//
		m_C_OrderLine_ID = matchPO.getC_OrderLine_ID();
		m_oLine = new MOrderLine (getCtx(), m_C_OrderLine_ID, getTrxName());
		//
		m_M_InOutLine_ID = matchPO.getM_InOutLine_ID();
		m_ioLine = new MInOutLine (getCtx(), m_M_InOutLine_ID, null);	
		
//		m_C_InvoiceLine_ID = matchPO.getC_InvoiceLine_ID();
		
		//
		m_pc = new ProductCost (getCtx(), getM_Product_ID(), m_M_AttributeSetInstance_ID, getTrxName());
		m_pc.setQty(getQty());
		
		this.noFactRecords = Services.get(ISysConfigBL.class).getBooleanValue(SYSCONFIG_NoFactRecords, DEFAULT_NoFactRecords);
		
		return null;
	}   //  loadDocumentDetails

	
	/**************************************************************************
	 *  Get Source Currency Balance - subtracts line and tax amounts from total - no rounding
	 *  @return Zero - always balanced
	 */
	@Override
	public BigDecimal getBalance()
	{
		return Env.ZERO;
	}   //  getBalance

	
	/**
	 *  Create Facts (the accounting logic) for
	 *  MXP.
	 *  <pre>
	 *      Product PPV     <difference>
	 *      PPV_Offset                  <difference>
	 *  </pre>
	 *  @param as accounting schema
	 *  @return Fact
	 */
	@Override
	public ArrayList<Fact> createFacts (final MAcctSchema as)
	{
		final ArrayList<Fact> facts = new ArrayList<Fact>();
		
		// If configured to not create accounting facts for Match PO documents
		// then don't do it (08555)
		if (noFactRecords)
		{
			return facts;
		}
		
		//
		if (getM_Product_ID() == 0		//  Nothing to do if no Product
			|| getQty().signum() == 0
			|| m_M_InOutLine_ID == 0)	//  No posting if not matched to Shipment
		{
			log.debug("No Product/Qty - M_Product_ID=" + getM_Product_ID()
				+ ",Qty=" + getQty());
			return facts;
		}

		//  create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);
		setC_Currency_ID(as.getC_Currency_ID());
		boolean isInterOrg = isInterOrg(as);
		
		//	Purchase Order Line
		BigDecimal poCost = m_oLine.getPriceCost();
		if (poCost == null || poCost.signum() == 0)
			poCost = m_oLine.getPriceActual();
		
		MInOutLine receiptLine = new MInOutLine (getCtx(), m_M_InOutLine_ID, getTrxName());	
		MInOut inOut = receiptLine.getParent(); 
		boolean isReturnTrx = inOut.getMovementType().equals(X_M_InOut.MOVEMENTTYPE_VendorReturns);
		
		// calculate po cost
		poCost = poCost.multiply(getQty());			//	Delivered so far
		
		//	Different currency
		if (m_oLine.getC_Currency_ID() != as.getC_Currency_ID())
		{
			MOrder order = m_oLine.getParent();
			Timestamp dateAcct = order.getDateAcct();
			BigDecimal rate = currencyConversionBL.getRate(
				order.getC_Currency_ID(), as.getC_Currency_ID(),
				dateAcct, order.getC_ConversionType_ID(),
				m_oLine.getAD_Client_ID(), m_oLine.getAD_Org_ID());
			if (rate == null)
			{
				throw newPostingException()
						.setC_AcctSchema(as)
						.setFact(fact)
						.setDetailMessage("Purchase Order not convertible");
			}
			poCost = poCost.multiply(rate);
			if (poCost.scale() > as.getCostingPrecision())
				poCost = poCost.setScale(as.getCostingPrecision(), BigDecimal.ROUND_HALF_UP);
		}

		//	Calculate PPV for standard costing
		final MProduct product = MProduct.get(getCtx(), getM_Product_ID());
		final String costingMethod = product.getCostingMethod(as);
		//get standard cost and also make sure cost for other costing method is updated
		final BigDecimal costs = m_pc.getProductCosts(as, getAD_Org_ID(),  MAcctSchema.COSTINGMETHOD_StandardCosting, m_C_OrderLine_ID, false);	//	non-zero costs

		if (MAcctSchema.COSTINGMETHOD_StandardCosting.equals(costingMethod))
		{
			//	No Costs yet - no PPV
			if (ProductCost.isNoCosts(costs))
			{
				throw newPostingException()
						.setC_AcctSchema(as)
						.setFact(fact)
						.setDetailMessage("Resubmit - No Costs for " + product.getName());
			}
	
			//	Difference
			BigDecimal difference = poCost.subtract(costs);
			//	Nothing to post
			if (difference.signum() == 0)
			{
				log.debug("No Cost Difference for M_Product_ID=" + getM_Product_ID());
				return facts;
			}
	
			//  Product PPV
			FactLine cr = fact.createLine(null,
				m_pc.getAccount(ProductCost.ACCTTYPE_P_PPV, as),
				as.getC_Currency_ID(), isReturnTrx ? difference.negate() : difference);
			if (cr != null)
			{
				cr.setQty(isReturnTrx ? getQty().negate() : getQty());
				cr.setC_BPartner_ID(m_oLine.getC_BPartner_ID());
				cr.setC_Activity_ID(m_oLine.getC_Activity_ID());
				cr.setC_Campaign_ID(m_oLine.getC_Campaign_ID());
				cr.setC_Project_ID(m_oLine.getC_Project_ID());
				final I_C_OrderLine ol = InterfaceWrapperHelper.create(m_oLine, I_C_OrderLine.class);
				cr.setC_UOM_ID(ol.getPrice_UOM_ID());
				cr.setUser1_ID(m_oLine.getUser1_ID());
				cr.setUser2_ID(m_oLine.getUser2_ID());
			}
	
			//  PPV Offset
			FactLine dr = fact.createLine(null,
				getAccount(Doc.ACCTTYPE_PPVOffset, as),
				as.getC_Currency_ID(), isReturnTrx ? difference : difference.negate());
			if (dr != null)
			{
				dr.setQty(isReturnTrx ? getQty() : getQty().negate());
				dr.setC_BPartner_ID(m_oLine.getC_BPartner_ID());
				dr.setC_Activity_ID(m_oLine.getC_Activity_ID());
				dr.setC_Campaign_ID(m_oLine.getC_Campaign_ID());
				dr.setC_Project_ID(m_oLine.getC_Project_ID());
				final I_C_OrderLine ol = InterfaceWrapperHelper.create(m_oLine, I_C_OrderLine.class);
				dr.setC_UOM_ID(ol.getPrice_UOM_ID());
				dr.setUser1_ID(m_oLine.getUser1_ID());
				dr.setUser2_ID(m_oLine.getUser2_ID());
			}
			
			// Avoid usage of clearing accounts
			// If both accounts Purchase Price Variance and Purchase Price Variance Offset are equal
			// then remove the posting
			
			MAccount acct_db =  dr.getAccount(); // PPV
			MAccount acct_cr = cr.getAccount(); // PPV Offset
			
			if ((!as.isPostIfClearingEqual()) && acct_db.equals(acct_cr) && (!isInterOrg)) {
				
				BigDecimal debit = dr.getAmtSourceDr();
				BigDecimal credit = cr.getAmtSourceCr();
				
				if (debit.compareTo(credit) == 0) {
					fact.remove(dr);
					fact.remove(cr);
				}
			
			}
			// End Avoid usage of clearing accounts
			
			//
			facts.add(fact);
			return facts;
		}
		else
		{
			return facts;
		}
	}   //  createFact
	
	/** Verify if the posting involves two or more organizations
	@return true if there are more than one org involved on the posting
	 */
	private boolean isInterOrg(MAcctSchema as) {
		MAcctSchemaElement elementorg = as.getAcctSchemaElement(MAcctSchemaElement.ELEMENTTYPE_Organization);
		if (elementorg == null || !elementorg.isBalanced()) {
			// no org element or not need to be balanced
			return false;
		}

		// verify if org of receipt line is different from org of order line
		// ignoring invoice line org as not used in posting
		if (m_ioLine != null && m_oLine != null
				&& m_ioLine.getAD_Org_ID() != m_oLine.getAD_Org_ID())
			return true;
		
		return false;
	}
}   //  Doc_MatchPO
