package de.metas.ui.web.handlingunits.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Services;
import org.compiere.util.Env;

import de.metas.handlingunits.inout.IHUInOutBL;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.process.IProcessPrecondition;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.handlingunits.process.HUEditorProcessTemplate.HUEditorRowFilter.Select;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Return the selected HUs back to vendor.
 *
 * @author metas-dev <dev@metasfresh.com>
 * @task initial task https://github.com/metasfresh/metasfresh-webui-api/issues/396
 */
public class WEBUI_M_HU_ReturnToVendor extends HUEditorProcessTemplate implements IProcessPrecondition
{
	private List<I_M_HU> husToReturn = null;

	@Override
	protected ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		final Set<Integer> huIds = getSelectedHUIds(Select.ONLY_TOPLEVEL);
		if (huIds.isEmpty())
		{
			return ProcessPreconditionsResolution.reject(msgBL.getTranslatableMsgText(WEBUI_M_HU_Messages.MSG_WEBUI_ONLY_TOP_LEVEL_HU));
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt() throws Exception
	{
		husToReturn = getSelectedHUs(Select.ONLY_TOPLEVEL);
		if (husToReturn.isEmpty())
		{
			throw new AdempiereException("@NoSelection@");
		}

		final Timestamp movementDate = Env.getDate(getCtx());
		Services.get(IHUInOutBL.class).createVendorReturnInOutForHUs(husToReturn, movementDate);
		return MSG_OK;
	}

	@Override
	protected void postProcess(final boolean success)
	{
		if (husToReturn != null && !husToReturn.isEmpty())
		{
			getView().removeHUsAndInvalidate(husToReturn);
		}
	}

}
