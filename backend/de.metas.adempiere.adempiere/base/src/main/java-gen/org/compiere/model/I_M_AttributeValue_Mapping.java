/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;


/** Generated Interface for M_AttributeValue_Mapping
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public interface I_M_AttributeValue_Mapping 
{

    /** TableName=M_AttributeValue_Mapping */
    public static final String Table_Name = "M_AttributeValue_Mapping";

    /** AD_Table_ID=540631 */
//    public static final int Table_ID = org.compiere.model.MTable.getTable_ID(Table_Name);

//    org.compiere.util.KeyNamePair Model = new org.compiere.util.KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
//    java.math.BigDecimal accessLevel = java.math.BigDecimal.valueOf(3);

    /** Load Meta Data */

	/**
	 * Get Mandant.
	 * Mandant für diese Installation.
	 *
	 * <br>Type: TableDir
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getAD_Client_ID();

	public org.compiere.model.I_AD_Client getAD_Client() throws RuntimeException;

    /** Column definition for AD_Client_ID */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_Client> COLUMN_AD_Client_ID = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_Client>(I_M_AttributeValue_Mapping.class, "AD_Client_ID", org.compiere.model.I_AD_Client.class);
    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/**
	 * Set Sektion.
	 * Organisatorische Einheit des Mandanten
	 *
	 * <br>Type: TableDir
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public void setAD_Org_ID (int AD_Org_ID);

	/**
	 * Get Sektion.
	 * Organisatorische Einheit des Mandanten
	 *
	 * <br>Type: TableDir
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getAD_Org_ID();

	public org.compiere.model.I_AD_Org getAD_Org() throws RuntimeException;

	public void setAD_Org(org.compiere.model.I_AD_Org AD_Org);

    /** Column definition for AD_Org_ID */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_Org> COLUMN_AD_Org_ID = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_Org>(I_M_AttributeValue_Mapping.class, "AD_Org_ID", org.compiere.model.I_AD_Org.class);
    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/**
	 * Get Erstellt.
	 * Datum, an dem dieser Eintrag erstellt wurde
	 *
	 * <br>Type: DateTime
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public java.sql.Timestamp getCreated();

    /** Column definition for Created */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object> COLUMN_Created = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object>(I_M_AttributeValue_Mapping.class, "Created", null);
    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/**
	 * Get Erstellt durch.
	 * Nutzer, der diesen Eintrag erstellt hat
	 *
	 * <br>Type: Table
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getCreatedBy();

    /** Column definition for CreatedBy */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_User> COLUMN_CreatedBy = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_User>(I_M_AttributeValue_Mapping.class, "CreatedBy", org.compiere.model.I_AD_User.class);
    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/**
	 * Set Beschreibung.
	 *
	 * <br>Type: String
	 * <br>Mandatory: false
	 * <br>Virtual Column: false
	 */
	public void setDescription (java.lang.String Description);

	/**
	 * Get Beschreibung.
	 *
	 * <br>Type: String
	 * <br>Mandatory: false
	 * <br>Virtual Column: false
	 */
	public java.lang.String getDescription();

    /** Column definition for Description */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object> COLUMN_Description = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object>(I_M_AttributeValue_Mapping.class, "Description", null);
    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/**
	 * Set Aktiv.
	 * Der Eintrag ist im System aktiv
	 *
	 * <br>Type: YesNo
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public void setIsActive (boolean IsActive);

	/**
	 * Get Aktiv.
	 * Der Eintrag ist im System aktiv
	 *
	 * <br>Type: YesNo
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public boolean isActive();

    /** Column definition for IsActive */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object> COLUMN_IsActive = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object>(I_M_AttributeValue_Mapping.class, "IsActive", null);
    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/**
	 * Set Merkmals-Wert.
	 * Product Attribute Value
	 *
	 * <br>Type: TableDir
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public void setM_AttributeValue_ID (int M_AttributeValue_ID);

	/**
	 * Get Merkmals-Wert.
	 * Product Attribute Value
	 *
	 * <br>Type: TableDir
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getM_AttributeValue_ID();

	public org.compiere.model.I_M_AttributeValue getM_AttributeValue() throws RuntimeException;

	public void setM_AttributeValue(org.compiere.model.I_M_AttributeValue M_AttributeValue);

    /** Column definition for M_AttributeValue_ID */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_M_AttributeValue> COLUMN_M_AttributeValue_ID = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_M_AttributeValue>(I_M_AttributeValue_Mapping.class, "M_AttributeValue_ID", org.compiere.model.I_M_AttributeValue.class);
    /** Column name M_AttributeValue_ID */
    public static final String COLUMNNAME_M_AttributeValue_ID = "M_AttributeValue_ID";

	/**
	 * Set Attribut substitute.
	 *
	 * <br>Type: ID
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public void setM_AttributeValue_Mapping_ID (int M_AttributeValue_Mapping_ID);

	/**
	 * Get Attribut substitute.
	 *
	 * <br>Type: ID
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getM_AttributeValue_Mapping_ID();

    /** Column definition for M_AttributeValue_Mapping_ID */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object> COLUMN_M_AttributeValue_Mapping_ID = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object>(I_M_AttributeValue_Mapping.class, "M_AttributeValue_Mapping_ID", null);
    /** Column name M_AttributeValue_Mapping_ID */
    public static final String COLUMNNAME_M_AttributeValue_Mapping_ID = "M_AttributeValue_Mapping_ID";

	/**
	 * Set Merkmals-Wert Nach.
	 * Product Attribute Value To
	 *
	 * <br>Type: Table
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public void setM_AttributeValue_To_ID (int M_AttributeValue_To_ID);

	/**
	 * Get Merkmals-Wert Nach.
	 * Product Attribute Value To
	 *
	 * <br>Type: Table
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getM_AttributeValue_To_ID();

	public org.compiere.model.I_M_AttributeValue getM_AttributeValue_To() throws RuntimeException;

	public void setM_AttributeValue_To(org.compiere.model.I_M_AttributeValue M_AttributeValue_To);

    /** Column definition for M_AttributeValue_To_ID */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_M_AttributeValue> COLUMN_M_AttributeValue_To_ID = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_M_AttributeValue>(I_M_AttributeValue_Mapping.class, "M_AttributeValue_To_ID", org.compiere.model.I_M_AttributeValue.class);
    /** Column name M_AttributeValue_To_ID */
    public static final String COLUMNNAME_M_AttributeValue_To_ID = "M_AttributeValue_To_ID";

	/**
	 * Get Aktualisiert.
	 * Datum, an dem dieser Eintrag aktualisiert wurde
	 *
	 * <br>Type: DateTime
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public java.sql.Timestamp getUpdated();

    /** Column definition for Updated */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object> COLUMN_Updated = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, Object>(I_M_AttributeValue_Mapping.class, "Updated", null);
    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/**
	 * Get Aktualisiert durch.
	 * Nutzer, der diesen Eintrag aktualisiert hat
	 *
	 * <br>Type: Table
	 * <br>Mandatory: true
	 * <br>Virtual Column: false
	 */
	public int getUpdatedBy();

    /** Column definition for UpdatedBy */
    public static final org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_User> COLUMN_UpdatedBy = new org.adempiere.model.ModelColumn<I_M_AttributeValue_Mapping, org.compiere.model.I_AD_User>(I_M_AttributeValue_Mapping.class, "UpdatedBy", org.compiere.model.I_AD_User.class);
    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";
}
