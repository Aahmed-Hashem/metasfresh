package de.metas.ui.web.picking.process;

import org.adempiere.exceptions.AdempiereException;
import org.springframework.beans.factory.annotation.Autowired;

import de.metas.handlingunits.picking.PickingCandidateCommand;
import de.metas.process.IProcessPrecondition;
import de.metas.ui.web.handlingunits.HUEditorRow;
import de.metas.ui.web.picking.pickingslot.PickingSlotRow;
import de.metas.ui.web.picking.pickingslot.PickingSlotView;

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
 * Assigns an existing HU to a picking slot.
 * 
 * This process is called from the HU selection dialog that is opened by {@link WEBUI_Picking_OpenHUsToPick}.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class WEBUI_Picking_PickSelectedHU
		extends WEBUI_Picking_Select_M_HU_Base
		implements IProcessPrecondition
{
	@Autowired
	private PickingCandidateCommand pickingCandidateCommand;

	@Override
	protected String doIt() throws Exception
	{
		retrieveEligibleHUEditorRows().forEach(
				huEditorRow -> {

					pickHuRow(huEditorRow);
				});

		invalidateViewsAndPrepareReturn();
		return MSG_OK;
	}

	void pickHuRow(final HUEditorRow huRow)
	{
		final int huId = huRow.getM_HU_ID();
		if (!huRow.isTopLevel())
		{
			// TODO: extract as top level
			throw new AdempiereException("Not a top level HU");
		}

		final PickingSlotView pickingSlotsView = getPickingSlotViewOrNull();
		final PickingSlotRow pickingSlotRow = getPickingSlotRow();
		final int pickingSlotId = pickingSlotRow.getPickingSlotId();
		final int shipmentScheduleId = pickingSlotsView.getCurrentShipmentScheduleId();

		pickingCandidateCommand.addHUToPickingSlot(huId, pickingSlotId, shipmentScheduleId);
	}
}
