/**********************************************************************
 * This file is part of Adempiere ERP Bazaar                          * 
 * http://www.adempiere.org                                           * 
 *                                                                    * 
 * Copyright (C) Trifon Trifonov.                                     * 
 * Copyright (C) Contributors                                         * 
 *                                                                    * 
 * This program is free software; you can redistribute it and/or      * 
 * modify it under the terms of the GNU General Public License        * 
 * as published by the Free Software Foundation; either version 2     * 
 * of the License, or (at your option) any later version.             * 
 *                                                                    * 
 * This program is distributed in the hope that it will be useful,    * 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of     * 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the       * 
 * GNU General Public License for more details.                       * 
 *                                                                    * 
 * You should have received a copy of the GNU General Public License  * 
 * along with this program; if not, write to the Free Software        * 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,         * 
 * MA 02110-1301, USA.                                                * 
 *                                                                    * 
 * Contributors:                                                      * 
 *  - Trifon Trifonov (trifonnt@users.sourceforge.net)                *
 *                                                                    *
 * Sponsors:                                                          *
 *  - E-evolution (http://www.e-evolution.com/)                       *
 **********************************************************************/
package org.compiere.server;

/*
 * #%L
 * de.metas.adempiere.adempiere.serverRoot.base
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


import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.server.rpl.IImportProcessor;
import org.adempiere.server.rpl.IReplicationProcessor;
import org.adempiere.server.rpl.api.IIMPProcessorBL;
import org.adempiere.server.rpl.api.IIMPProcessorDAO;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.AdempiereProcessor;
import org.compiere.model.I_IMP_Processor;
import org.compiere.util.TimeUtil;

/**
 * 
 * @author Trifon N. Trifonov
 * 
 */
public class ReplicationProcessor extends AdempiereServer
		implements IReplicationProcessor
{

	/** Last Summary */
	private StringBuffer m_summary = new StringBuffer();

	/**
	 * Contains the config data, including a reference to the import processor type.
	 */
	private final I_IMP_Processor mImportProcessor;

	/**
	 * The actual import processor
	 */
	private IImportProcessor importProcessor;

	/**
	 * flag showing if process is working!
	 */
	private boolean importProcessorRunning = false;

	public ReplicationProcessor(AdempiereProcessor model, int initialNapSecs)
	{
		super(model, initialNapSecs);
		mImportProcessor = Services.get(IIMPProcessorBL.class).getIMP_Processor(model);
	}

	protected ReplicationProcessor(AdempiereProcessor model)
	{
		this(model, 10);
	}

	@Override
	protected void doWork()
	{
		//
		InterfaceWrapperHelper.refresh(mImportProcessor); // daysToKeepLog might have changed
		final int no = Services.get(IIMPProcessorDAO.class).deleteLogs(mImportProcessor);
		if(no > 0)
		{
			m_summary.append("Logs Records deleted=").append(no).append("; ");
		}
		if (isProcessRunning())
		{
			// process is already started successfully!
			return;
		}

		// process is not started!
		m_summary = new StringBuffer();
		final Properties ctx = InterfaceWrapperHelper.getCtx(mImportProcessor);
		final String trxName = InterfaceWrapperHelper.getTrxName(mImportProcessor);
		log.debug("trxName = " + trxName);
		log.debug("ImportProcessor = " + mImportProcessor);

		try
		{
			importProcessor = Services.get(IIMPProcessorBL.class).getIImportProcessor(mImportProcessor);
			importProcessor.start(ctx, this, trxName);
			Check.assume(isProcessRunning(), importProcessor + " has called setProcessRunning(true)");
		}
		catch (Exception e)
		{
			setProcessRunning(false);
			log(null, e);

			try
			{
				importProcessor.stop();
			}
			catch (Exception e1)
			{
				log(null, e1);
			}
		}
		
		//
		log(m_summary.toString(), null);
	}

	private void log(String summary, Throwable t)
	{
		if (summary == null && t != null)
		{
			summary = t.getMessage();
		}
		if (t != null)
		{
			log.error(summary, t);
		}
		
		final String reference = "#" + String.valueOf(p_runCount) + " - " + TimeUtil.formatElapsed(new Timestamp(p_startWork));
		String text = null;
		Services.get(IIMPProcessorBL.class).createLog(mImportProcessor, summary, text, reference, t);
	}

	@Override
	public String getServerInfo()
	{
		return "#" + p_runCount + " - Last=" + m_summary.toString();
	}

	/**
	 * @return the isProcessRunning
	 */
	@Override
	public boolean isProcessRunning()
	{
		return importProcessorRunning;
	}

	/**
	 * @param isProcessRunning the isProcessRunning to set
	 */
	@Override
	public void setProcessRunning(boolean isProcessRunning)
	{
		this.importProcessorRunning = isProcessRunning;
	}

	/**
	 * @return the mImportProcessor
	 */
	@Override
	public I_IMP_Processor getMImportProcessor()
	{
		return mImportProcessor;
	}
}
