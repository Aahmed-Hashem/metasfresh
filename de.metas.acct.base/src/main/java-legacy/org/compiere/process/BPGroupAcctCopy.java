/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved. *
 * This program is free software; you can redistribute it and/or modify it *
 * under the terms version 2 of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. *
 * See the GNU General Public License for more details. *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
 * For the text or an alternative of this public license, you may reach us *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA *
 * or via info@compiere.org or http://www.compiere.org/license.html *
 *****************************************************************************/
package org.compiere.process;

import java.math.BigDecimal;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.util.DB;

import de.metas.acct.api.AcctSchemaId;
import de.metas.process.JavaProcess;
import de.metas.process.ProcessInfoParameter;

/**
 * Copy BP Group default Accounts
 * 
 * @author Jorg Janke
 * @version $Id: BPGroupAcctCopy.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class BPGroupAcctCopy extends JavaProcess
{
	/** BP Group */
	private int p_C_BP_Group_ID = 0;
	/** Acct Schema */
	private AcctSchemaId acctSchemaId;

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameters())
		{
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("C_BP_Group_ID"))
				p_C_BP_Group_ID = para.getParameterAsInt();
			else if (name.equals("C_AcctSchema_ID"))
				acctSchemaId = AcctSchemaId.ofRepoId(para.getParameterAsInt());
			else
				log.error("Unknown Parameter: " + name);
		}
	}	// prepare

	@Override
	protected String doIt()
	{
		if (acctSchemaId == null)
		{
			throw new FillMandatoryException("C_AcctSchema_ID");
		}
		
		//
		String sql = null;
		int updated = 0;
		int created = 0;
		int updatedTotal = 0;
		int createdTotal = 0;

		// Update existing Customers
		sql = DB.convertSqlToNative("UPDATE C_BP_Customer_Acct ca "
				+ "SET (C_Receivable_Acct,C_Receivable_Services_Acct,C_PrePayment_Acct)="
				+ " (SELECT C_Receivable_Acct,C_Receivable_Services_Acct,C_PrePayment_Acct "
				+ " FROM C_BP_Group_Acct"
				+ " WHERE C_BP_Group_ID=" + p_C_BP_Group_ID
				+ " AND C_AcctSchema_ID=" + acctSchemaId.getRepoId()
				+ "), Updated=now(), UpdatedBy=0 "
				+ "WHERE ca.C_AcctSchema_ID=" + acctSchemaId.getRepoId()
				+ " AND EXISTS (SELECT * FROM C_BPartner p "
				+ "WHERE p.C_BPartner_ID=ca.C_BPartner_ID"
				+ " AND p.C_BP_Group_ID=" + p_C_BP_Group_ID + ")");
		updated = DB.executeUpdate(sql, get_TrxName());
		addLog(0, null, new BigDecimal(updated), "@Updated@ @C_BPartner_ID@ @IsCustomer@");
		updatedTotal += updated;

		// Insert new Customer
		sql = "INSERT INTO C_BP_Customer_Acct "
				+ "(C_BPartner_ID, C_AcctSchema_ID,"
				+ " AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,"
				+ " C_Receivable_Acct, C_Receivable_Services_Acct, C_PrePayment_Acct) "
				+ "SELECT p.C_BPartner_ID, acct.C_AcctSchema_ID,"
				+ " p.AD_Client_ID, p.AD_Org_ID, 'Y', now(), 0, now(), 0,"
				+ " acct.C_Receivable_Acct, acct.C_Receivable_Services_Acct, acct.C_PrePayment_Acct "
				+ "FROM C_BPartner p"
				+ " INNER JOIN C_BP_Group_Acct acct ON (acct.C_BP_Group_ID=p.C_BP_Group_ID)"
				+ "WHERE acct.C_AcctSchema_ID=" + acctSchemaId.getRepoId()			// #
				+ " AND p.C_BP_Group_ID=" + p_C_BP_Group_ID
				+ " AND NOT EXISTS (SELECT * FROM C_BP_Customer_Acct ca "
				+ "WHERE ca.C_BPartner_ID=p.C_BPartner_ID"
				+ " AND ca.C_AcctSchema_ID=acct.C_AcctSchema_ID)";
		created = DB.executeUpdate(sql, get_TrxName());
		addLog(0, null, new BigDecimal(created), "@Created@ @C_BPartner_ID@ @IsCustomer@");
		createdTotal += created;

		// Update existing Vendors
		sql = DB.convertSqlToNative("UPDATE C_BP_Vendor_Acct va "
				+ "SET (V_Liability_Acct,V_Liability_Services_Acct,V_PrePayment_Acct)="
				+ " (SELECT V_Liability_Acct,V_Liability_Services_Acct,V_PrePayment_Acct "
				+ " FROM C_BP_Group_Acct"
				+ " WHERE C_BP_Group_ID=" + p_C_BP_Group_ID
				+ " AND C_AcctSchema_ID=" + acctSchemaId.getRepoId()
				+ "), Updated=now(), UpdatedBy=0 "
				+ "WHERE va.C_AcctSchema_ID=" + acctSchemaId.getRepoId()
				+ " AND EXISTS (SELECT * FROM C_BPartner p "
				+ "WHERE p.C_BPartner_ID=va.C_BPartner_ID"
				+ " AND p.C_BP_Group_ID=" + p_C_BP_Group_ID + ")");
		updated = DB.executeUpdate(sql, get_TrxName());
		addLog(0, null, new BigDecimal(updated), "@Updated@ @C_BPartner_ID@ @IsVendor@");
		updatedTotal += updated;

		// Insert new Vendors
		sql = "INSERT INTO C_BP_Vendor_Acct "
				+ "(C_BPartner_ID, C_AcctSchema_ID,"
				+ " AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,"
				+ " V_Liability_Acct, V_Liability_Services_Acct, V_PrePayment_Acct) "
				+ "SELECT p.C_BPartner_ID, acct.C_AcctSchema_ID,"
				+ " p.AD_Client_ID, p.AD_Org_ID, 'Y', now(), 0, now(), 0,"
				+ " acct.V_Liability_Acct, acct.V_Liability_Services_Acct, acct.V_PrePayment_Acct "
				+ "FROM C_BPartner p"
				+ " INNER JOIN C_BP_Group_Acct acct ON (acct.C_BP_Group_ID=p.C_BP_Group_ID)"
				+ "WHERE acct.C_AcctSchema_ID=" + acctSchemaId.getRepoId() // #
				+ " AND p.C_BP_Group_ID=" + p_C_BP_Group_ID
				+ " AND NOT EXISTS (SELECT * FROM C_BP_Vendor_Acct va "
				+ "WHERE va.C_BPartner_ID=p.C_BPartner_ID AND va.C_AcctSchema_ID=acct.C_AcctSchema_ID)";
		created = DB.executeUpdate(sql, get_TrxName());
		addLog(0, null, new BigDecimal(created), "@Created@ @C_BPartner_ID@ @IsVendor@");
		createdTotal += created;

		return "@Created@=" + createdTotal + ", @Updated@=" + updatedTotal;
	}	// doIt

}	// BPGroupAcctCopy
