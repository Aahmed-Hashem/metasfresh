-- 2019-03-06T17:09:32.562
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
INSERT INTO AD_Process_Para (AD_Client_ID,AD_Element_ID,AD_Org_ID,AD_Process_ID,AD_Process_Para_ID,AD_Reference_ID,ColumnName,Created,CreatedBy,DefaultValue,EntityType,FieldLength,IsActive,IsAutocomplete,IsCentrallyMaintained,IsEncrypted,IsMandatory,IsRange,Name,SeqNo,Updated,UpdatedBy) VALUES (0,576158,0,540836,541378,20,'isManualImport',TO_TIMESTAMP('2019-03-06 17:09:32','YYYY-MM-DD HH24:MI:SS'),100,'N','D',0,'Y','N','Y','N','Y','N','isManualImport',10,TO_TIMESTAMP('2019-03-06 17:09:32','YYYY-MM-DD HH24:MI:SS'),100)
;

-- 2019-03-06T17:09:32.578
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
INSERT INTO AD_Process_Para_Trl (AD_Language,AD_Process_Para_ID, Description,Help,Name, IsTranslated,AD_Client_ID,AD_Org_ID,Created,Createdby,Updated,UpdatedBy) SELECT l.AD_Language, t.AD_Process_Para_ID, t.Description,t.Help,t.Name, 'N',t.AD_Client_ID,t.AD_Org_ID,t.Created,t.Createdby,t.Updated,t.UpdatedBy FROM AD_Language l, AD_Process_Para t WHERE l.IsActive='Y'AND (l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N') AND t.AD_Process_Para_ID=541378 AND NOT EXISTS (SELECT 1 FROM AD_Process_Para_Trl tt WHERE tt.AD_Language=l.AD_Language AND tt.AD_Process_Para_ID=t.AD_Process_Para_ID)
;

-- 2019-03-07T11:49:36.605
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
UPDATE AD_Process_Para SET DefaultValue='N', IsMandatory='Y',Updated=TO_TIMESTAMP('2019-03-07 11:49:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_Para_ID=541377
;

-- 2019-03-07T11:49:37.640
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
UPDATE AD_Process_Para SET AD_Element_ID=576164, ColumnName='IsInsertOnly', Description='Dieser Prozess fügt neue Einträge hinzu. Schon vorhandene Einträge werden nicht aktualisiert.', Name='Nur Hinzufügen',Updated=TO_TIMESTAMP('2019-03-07 11:49:37','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_Para_ID=541378
;

