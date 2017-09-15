package de.metas.ui.web.picking.process;

import static de.metas.ui.web.picking.PickingConstants.MSG_WEBUI_PICKING_SELECT_ACTIVE_UNPICKED_UNSELECTED_HU;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.adempiere.util.Services;
import org.springframework.beans.factory.annotation.Autowired;

import de.metas.handlingunits.picking.IHUPickingSlotDAO;
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

public abstract class WEBUI_Picking_Select_M_HU_Base extends ViewBasedProcessTemplate
{
	@Autowired
	private IViewsRepository viewsRepo;

	@Override
	public final ProcessPreconditionsResolution checkPreconditionsApplicable()
	{
		final Optional<HUEditorRow> anyHU = retrieveEligibleHUEditorRows().findAny();
		if (anyHU.isPresent())
		{
			return ProcessPreconditionsResolution.accept();
		}

		final ITranslatableString reason = Services.get(IMsgBL.class).getTranslatableMsgText(MSG_WEBUI_PICKING_SELECT_ACTIVE_UNPICKED_UNSELECTED_HU);
		return ProcessPreconditionsResolution.reject(reason);
	}

	protected final Stream<HUEditorRow> retrieveEligibleHUEditorRows()
	{
		final List<HUEditorRow> huEditorRows = getView().getByIds(getSelectedDocumentIds());
		final IHUPickingSlotDAO huPickingSlotDAO = Services.get(IHUPickingSlotDAO.class);

		final Stream<HUEditorRow> stream = huEditorRows.stream()
				.filter(huRow -> huRow.isTopLevel())
				.filter(huRow -> huRow.isHUStatusActive())
				.filter(huRow -> !huPickingSlotDAO.isSourceHU(huRow.getM_HU_ID())) // may not yet be a source-HU
				.filter(huRow -> !huPickingSlotDAO.isHuIdPicked(huRow.getM_HU_ID()));

		return stream;
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

	protected PickingSlotRow getPickingSlotRow()
	{
		final HUEditorView huView = getView();
		final DocumentId pickingSlotRowId = huView.getParentRowId();

		final PickingSlotView pickingSlotView = getPickingSlotViewOrNull();
		return pickingSlotView.getById(pickingSlotRowId);
	}
	
	protected final void invalidateViewsAndPrepareReturn()
	{
		invalidateView(); // picking slots view
		invalidateParentView();  // picking view

		// After this process finished successfully go back to picking slots view
		getResult().setWebuiIncludedViewIdToOpen(getPickingSlotViewOrNull().getViewId().getViewId());
	}

}
