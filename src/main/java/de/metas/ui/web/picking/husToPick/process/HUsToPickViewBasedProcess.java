package de.metas.ui.web.picking.husToPick.process;

import static de.metas.ui.web.handlingunits.WEBUI_HU_Constants.MSG_WEBUI_SELECT_ACTIVE_UNSELECTED_HU;

import java.util.Optional;
import java.util.stream.Stream;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Services;
import org.springframework.beans.factory.annotation.Autowired;

import de.metas.handlingunits.picking.IHUPickingSlotDAO;
import de.metas.handlingunits.picking.PickingCandidateService;
import de.metas.handlingunits.sourcehu.SourceHUsService;
import de.metas.i18n.IMsgBL;
import de.metas.i18n.ITranslatableString;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.handlingunits.HUEditorRow;
import de.metas.ui.web.handlingunits.HUEditorView;
import de.metas.ui.web.picking.pickingslot.PickingSlotRow;
import de.metas.ui.web.picking.pickingslot.PickingSlotView;
import de.metas.ui.web.process.adprocess.ViewBasedProcessTemplate;
import de.metas.ui.web.view.IView;
import de.metas.ui.web.view.IViewsRepository;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.window.datatypes.DocumentId;

/* package */abstract class HUsToPickViewBasedProcess extends ViewBasedProcessTemplate
{
	@Autowired
	private PickingCandidateService pickingCandidateService;
	@Autowired
	private IViewsRepository viewsRepo;
	private final SourceHUsService sourceHuService = SourceHUsService.get();
	private final IHUPickingSlotDAO huPickingSlotDAO = Services.get(IHUPickingSlotDAO.class);

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		final Optional<HUEditorRow> anyHU = retrieveEligibleHUEditorRows().findAny();
		if (anyHU.isPresent())
		{
			return ProcessPreconditionsResolution.accept();
		}

		final ITranslatableString reason = Services.get(IMsgBL.class).getTranslatableMsgText(MSG_WEBUI_SELECT_ACTIVE_UNSELECTED_HU);
		return ProcessPreconditionsResolution.reject(reason);
	}

	protected final Stream<HUEditorRow> retrieveEligibleHUEditorRows()
	{
		return getView()
				.streamByIds(getSelectedRowIds())
				.filter(this::isEligible);
	}

	protected final boolean isEligible(final HUEditorRow huRow)
	{
		if (!huRow.isTopLevel())
		{
			return false;
		}
		if (!huRow.isHUStatusActive())
		{
			return false;
		}

		// may not yet be a source-HU
		if (sourceHuService.isSourceHu(huRow.getM_HU_ID()))
		{
			return false;
		}

		if (huPickingSlotDAO.isHuIdPicked(huRow.getM_HU_ID()))
		{
			return false;
		}

		return true;
	}

	@Override
	protected final HUEditorView getView()
	{
		return HUEditorView.cast(super.getView());
	}

	@Override
	protected HUEditorRow getSingleSelectedRow()
	{
		return HUEditorRow.cast(super.getSingleSelectedRow());
	}

	protected PickingSlotView getPickingSlotViewOrNull()
	{
		final ViewId parentViewId = getView().getParentViewId();
		if (parentViewId == null)
		{
			return null;
		}
		final IView parentView = viewsRepo.getView(parentViewId);
		return PickingSlotView.cast(parentView);
	}

	protected PickingSlotView getPickingSlotView()
	{
		PickingSlotView pickingSlotsView = getPickingSlotViewOrNull();
		if (pickingSlotsView == null)
		{
			throw new AdempiereException("PickingSlots view is not available");
		}
		return pickingSlotsView;
	}

	protected PickingSlotRow getPickingSlotRow()
	{
		final HUEditorView huView = getView();
		final DocumentId pickingSlotRowId = huView.getParentRowId();

		final PickingSlotView pickingSlotView = getPickingSlotView();
		return pickingSlotView.getById(pickingSlotRowId);
	}

	protected final void invalidateViewsAndGoBackToPickingSlotsView()
	{
		invalidateView(); // picking slots view
		invalidateParentView();  // picking view

		// After this process finished successfully go back to picking slots view
		getResult().setWebuiIncludedViewIdToOpen(getPickingSlotView().getViewId().getViewId());
	}

	protected final void addHUIdToCurrentPickingSlot(final int huId)
	{
		final PickingSlotView pickingSlotsView = getPickingSlotView();
		final PickingSlotRow pickingSlotRow = getPickingSlotRow();
		final int pickingSlotId = pickingSlotRow.getPickingSlotId();
		final int shipmentScheduleId = pickingSlotsView.getCurrentShipmentScheduleId();

		pickingCandidateService.addHUToPickingSlot(huId, pickingSlotId, shipmentScheduleId);
	}
}
