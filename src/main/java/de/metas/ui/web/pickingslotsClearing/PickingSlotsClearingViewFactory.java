package de.metas.ui.web.pickingslotsClearing;

import java.util.List;
import java.util.Properties;

import javax.annotation.Nullable;

import org.adempiere.ad.window.api.IADWindowDAO;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;

import de.metas.picking.api.IPickingSlotDAO.PickingSlotQuery;
import de.metas.picking.api.IPickingSlotDAO.PickingSlotQuery.PickingSlotQueryBuilder;
import de.metas.process.IADProcessDAO;
import de.metas.process.RelatedProcessDescriptor;
import de.metas.ui.web.document.filter.DocumentFilterDescriptorsProvider;
import de.metas.ui.web.picking.pickingslot.PickingSlotRow;
import de.metas.ui.web.picking.pickingslot.PickingSlotViewFilters;
import de.metas.ui.web.picking.pickingslot.PickingSlotViewRepository;
import de.metas.ui.web.pickingslotsClearing.process.WEBUI_PickingSlotsClearingView_TakeOutHU;
import de.metas.ui.web.view.CreateViewRequest;
import de.metas.ui.web.view.IViewFactory;
import de.metas.ui.web.view.ViewFactory;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.ViewProfileId;
import de.metas.ui.web.view.descriptor.IncludedViewLayout;
import de.metas.ui.web.view.descriptor.ViewLayout;
import de.metas.ui.web.view.json.JSONViewDataType;
import de.metas.ui.web.window.datatypes.WindowId;

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
 * Browse Picking slots
 * 
 * @author metas-dev <dev@metasfresh.com>
 * @task https://github.com/metasfresh/metasfresh/issues/518
 */
@ViewFactory(windowId = PickingSlotsClearingViewFactory.WINDOW_ID_STRING)
public class PickingSlotsClearingViewFactory implements IViewFactory
{
	static final String WINDOW_ID_STRING = "540371"; // Picking Tray Clearing
	public static final WindowId WINDOW_ID = WindowId.fromJson(WINDOW_ID_STRING);

	@Autowired
	private PickingSlotViewRepository pickingSlotRepo;

	private CCache<Integer, DocumentFilterDescriptorsProvider> filterDescriptorsProviderCache = CCache.newCache("PickingSlotsClearingViewFactory.FilterDescriptorsProvider", 1, CCache.EXPIREMINUTES_Never);

	@Override
	public ViewLayout getViewLayout(final WindowId windowId, final JSONViewDataType viewDataType, @Nullable final ViewProfileId profileId)
	{
		// TODO: cache it

		return ViewLayout.builder()
				.setWindowId(WINDOW_ID)
				.setCaption(Services.get(IADWindowDAO.class).retrieveWindowName(WINDOW_ID.toInt()))
				.addElementsFromViewRowClass(PickingSlotRow.class, viewDataType)
				.setHasTreeSupport(true)
				.setFilters(getFilterDescriptorsProvider().getAll())
				.setIncludedViewLayout(IncludedViewLayout.builder()
						.openOnSelect(true)
						.blurWhenOpen(false)
						.build())
				.build();
	}

	private DocumentFilterDescriptorsProvider getFilterDescriptorsProvider()
	{
		return filterDescriptorsProviderCache.getOrLoad(0, () -> PickingSlotViewFilters.createFilterDescriptorsProvider());
	}

	@Override
	public PickingSlotsClearingView createView(final CreateViewRequest request)
	{
		request.assertNoParentViewOrRow();

		final CreateViewRequest requestEffective = request.unwrapFiltersAndCopy(getFilterDescriptorsProvider());

		final ViewId viewId = ViewId.random(PickingSlotsClearingViewFactory.WINDOW_ID);

		final PickingSlotQuery query = createPickingSlotQuery(requestEffective);

		return PickingSlotsClearingView.builder()
				.viewId(viewId)
				.rows(() -> pickingSlotRepo.retrievePickingSlotsRows(query))
				.additionalRelatedProcessDescriptors(createAdditionalRelatedProcessDescriptors())
				.filters(requestEffective.getFilters().getFilters())
				.build();
	}

	private static final PickingSlotQuery createPickingSlotQuery(final CreateViewRequest request)
	{
		final PickingSlotQueryBuilder queryBuilder = PickingSlotQuery.builder();

		String barcode = PickingSlotViewFilters.getPickingSlotBarcode(request.getFilters());
		if (!Check.isEmpty(barcode, true))
		{
			queryBuilder.barcode(barcode);
		}

		return queryBuilder.build();
	}

	private List<RelatedProcessDescriptor> createAdditionalRelatedProcessDescriptors()
	{
		// TODO: cache it

		final IADProcessDAO adProcessDAO = Services.get(IADProcessDAO.class);
		final Properties ctx = Env.getCtx();

		return ImmutableList.of(
				// allow to open the HU-editor for various picking related purposes
				RelatedProcessDescriptor.builder()
						.processId(adProcessDAO.retriveProcessIdByClass(ctx, WEBUI_PickingSlotsClearingView_TakeOutHU.class))
						.anyTable().anyWindow()
						.webuiQuickAction(true)
						.build());
	}

}
