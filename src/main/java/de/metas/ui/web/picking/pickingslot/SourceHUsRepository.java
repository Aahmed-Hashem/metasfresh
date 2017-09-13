package de.metas.ui.web.picking.pickingslot;

import static org.adempiere.model.InterfaceWrapperHelper.load;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.adempiere.util.Services;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import de.metas.handlingunits.IHUPickingSlotBL;
import de.metas.handlingunits.IHUPickingSlotBL.RetrieveActiveSourceHusQuery;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.trace.HUTraceEvent;
import de.metas.handlingunits.trace.HUTraceRepository;
import de.metas.handlingunits.trace.HUTraceSpecification;
import de.metas.handlingunits.trace.HUTraceType;
import de.metas.inoutcandidate.model.I_M_ShipmentSchedule;
import lombok.NonNull;

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
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Service
public class SourceHUsRepository
{
	private final HUTraceRepository huTraceRepository;

	private final PickingCandidateRepository pickingCandidateRepo;

	public SourceHUsRepository(
			@NonNull final HUTraceRepository huTraceRepository,
			@NonNull final PickingCandidateRepository pickingCandidateRepo)
	{
		this.huTraceRepository = huTraceRepository;
		this.pickingCandidateRepo = pickingCandidateRepo;
	}

	/**
	 * Uses HU-tracing to identify and return the (top-level) HUs that are the given {@code huIds}' source HUs.
	 * 
	 * @param huIds
	 * @return
	 */
	public Collection<I_M_HU> retrieveSourceHUsViaTracing(@NonNull final List<Integer> huIds)
	{
		final Set<Integer> vhuSourceIds = new HashSet<>();

		for (final int huId : huIds)
		{
			final HUTraceSpecification query = HUTraceSpecification.builder()
					.type(HUTraceType.TRANSFORM_LOAD)
					.topLevelHuId(huId)
					.build();

			final List<HUTraceEvent> traceEvents = huTraceRepository.query(query);
			for (final HUTraceEvent traceEvent : traceEvents)
			{
				vhuSourceIds.add(traceEvent.getVhuSourceId());
			}
		}

		//
		// don't use Services.get(IHandlingUnitsBL.class).getTopLevelHUs() because the sourceHUs might already be destroyed
		final Set<I_M_HU> topLevelSourceHus = new TreeSet<>(Comparator.comparing(I_M_HU::getM_HU_ID));
		for (int vhuSourceId : vhuSourceIds)
		{
			final HUTraceSpecification query = HUTraceSpecification.builder()
					.type(HUTraceType.TRANSFORM_LOAD)
					.vhuId(vhuSourceId)
					.build();
			final List<HUTraceEvent> traceEvents = huTraceRepository.query(query);
			for (final HUTraceEvent traceEvent : traceEvents)
			{
				final I_M_HU topLevelSourceHu = load(traceEvent.getTopLevelHuId(), I_M_HU.class);
				topLevelSourceHus.add(topLevelSourceHu);
			}
		}
		return topLevelSourceHus;
	}

	public Collection<I_M_HU> retrieveMatchingSourceHUs(final int huId)
	{
		final List<I_M_ShipmentSchedule> scheds = pickingCandidateRepo.retrieveShipmentSchedulesViaPickingCandidates(huId);
		final RetrieveActiveSourceHusQuery query = RetrieveActiveSourceHusQuery.fromShipmentSchedules(scheds);

		final IHUPickingSlotBL huPickingSlotBL = Services.get(IHUPickingSlotBL.class);
		final List<I_M_HU> sourceHUs = huPickingSlotBL.retrieveActiveSourceHUs(query);

		return sourceHUs;
	}

	public Collection<I_M_HU> retrieveSourceHUs(final int huId)
	{
		final LinkedHashMap<Integer, I_M_HU> map = new LinkedHashMap<>();

		addToMapIfMissing(map, retrieveSourceHUsViaTracing(ImmutableList.of(huId)));
		addToMapIfMissing(map, retrieveMatchingSourceHUs(huId));

		return map.values();
	}

	void addToMapIfMissing(
			@NonNull final LinkedHashMap<Integer, I_M_HU> map,
			@NonNull final Collection<I_M_HU> sourceHUs)
	{
		for (final I_M_HU sourceHU : sourceHUs)
		{
			if (map.containsKey(sourceHU.getM_HU_ID()))
			{
				continue;
			}
			map.put(sourceHU.getM_HU_ID(), sourceHU);
		}
	}

}
