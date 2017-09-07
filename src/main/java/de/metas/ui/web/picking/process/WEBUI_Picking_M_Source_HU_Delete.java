package de.metas.ui.web.picking.process;

import static de.metas.ui.web.picking.PickingConstants.MSG_WEBUI_PICKING_SELECT_SOURCE_HU;

import org.springframework.beans.factory.annotation.Autowired;

import de.metas.process.IProcessPrecondition;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.picking.PickingCandidateCommand;
import de.metas.ui.web.picking.PickingSlotRow;
import de.metas.ui.web.process.adprocess.ViewBasedProcessTemplate;

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

public class WEBUI_Picking_M_Source_HU_Delete
		extends ViewBasedProcessTemplate
		implements IProcessPrecondition
{
	@Autowired
	private PickingCandidateCommand pickingCandidateCommand;
	
	@Override
	protected ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		if (!getSelectedDocumentIds().isSingleDocumentId())
		{
			return ProcessPreconditionsResolution.rejectBecauseNotSingleSelection();
		}

		final PickingSlotRow pickingSlotRow = getSingleSelectedRow();
		if (!pickingSlotRow.isPickingSourceHURow())
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason(MSG_WEBUI_PICKING_SELECT_SOURCE_HU);
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected String doIt() throws Exception
	{
		final PickingSlotRow rowToProcess = getSingleSelectedRow();
		final int huId = rowToProcess.getHuId();

		pickingCandidateCommand.removeSourceHu(huId);

		invalidateView();
		invalidateParentView();

		return MSG_OK;
	}

	@Override
	protected PickingSlotRow getSingleSelectedRow()
	{
		return PickingSlotRow.cast(super.getSingleSelectedRow());
	}

}
