DROP VIEW IF EXISTS Fact_Acct_Transactions_View;

CREATE VIEW Fact_Acct_Transactions_View AS

SELECT fact_acct_id,
       Created,
       CreatedBy,
       Updated,
       UpdatedBy,
       IsActive,
       AD_Client_ID,
       AD_Org_ID,
       documentNo,
       C_AcctSchema_ID,
       GL_Category_ID,
       C_Period_ID,
       PostingType,
       Account_ID,
       C_Currency_ID,
       AmtSourceDr,
       AmtSourceCr,
       AmtAcctDr,
       AmtAcctCr,
       M_Product_ID,
       AD_OrgTrx_ID,
       C_LocFrom_ID,
       C_LocTo_ID,
       C_SalesRegion_ID,
       C_Project_ID,
       User1_ID,
       User2_ID,
       GL_Budget_ID,
       C_Campaign_ID,
       C_BPartner_ID,
       C_Activity_ID,
       DateTrx,
       DateAcct,
       AD_Table_ID,
       Record_ID,
       C_Tax_ID,
       Description,
       M_Locator_ID,
       C_SubAcct_ID,
       UserElement1_ID,
       UserElement2_ID,
       CurrencyRate,
       DocStatus,
       acctbalance(account_id, AmtAcctDr, amtacctcr) as balance


FROM Fact_Acct fact;

