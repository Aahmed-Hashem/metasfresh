package de.metas.material.dispo.service.event.handler.pporder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import de.metas.Profiles;
import de.metas.document.engine.DocStatus;
import de.metas.material.dispo.commons.candidate.Candidate;
import de.metas.material.dispo.commons.candidate.businesscase.ProductionDetail;
import de.metas.material.dispo.commons.repository.CandidateRepositoryRetrieval;
import de.metas.material.dispo.service.candidatechange.CandidateChangeService;
import de.metas.material.event.MaterialEventHandler;
import de.metas.material.event.pporder.PPOrderChangedEvent;
import de.metas.material.event.pporder.PPOrderChangedEvent.ChangedPPOrderLineDescriptor;
import de.metas.util.Check;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-material-dispo-service
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

@Service
@Profile(Profiles.PROFILE_MaterialDispo)
public class PPOrderChangedHandler implements MaterialEventHandler<PPOrderChangedEvent>
{
	private final CandidateRepositoryRetrieval candidateRepositoryRetrieval;
	private final CandidateChangeService candidateChangeService;

	public PPOrderChangedHandler(
			@NonNull final CandidateRepositoryRetrieval candidateRepositoryRetrieval,
			@NonNull final CandidateChangeService candidateChangeService)
	{
		this.candidateChangeService = candidateChangeService;
		this.candidateRepositoryRetrieval = candidateRepositoryRetrieval;

	}

	@Override
	public Collection<Class<? extends PPOrderChangedEvent>> getHandeledEventType()
	{
		return ImmutableList.of(PPOrderChangedEvent.class);
	}

	@Override
	public void handleEvent(@NonNull final PPOrderChangedEvent ppOrderChangedEvent)
	{
		final List<Candidate> candidatesToUpdate = candidateRepositoryRetrieval.retrieveCandidatesForPPOrderId(ppOrderChangedEvent.getPpOrderId());
		Check.errorIf(candidatesToUpdate.isEmpty(), "No Candidates found for PP_Order_ID={}", ppOrderChangedEvent.getPpOrderId());

		final List<Candidate> updatedCandidatesToPersist = new ArrayList<>();

		updatedCandidatesToPersist.addAll(
				processPPOrderChange(
						candidatesToUpdate,
						ppOrderChangedEvent));

		updatedCandidatesToPersist.addAll(
				processPPOrderLinesChanges(
						candidatesToUpdate,
						ppOrderChangedEvent.getNewDocStatus(),
						ppOrderChangedEvent.getPpOrderLineChanges()));

		// TODO: handle delete and creation of new lines

		updatedCandidatesToPersist.forEach(candidate -> candidateChangeService.onCandidateNewOrChange(candidate));
	}

	private static List<Candidate> processPPOrderChange(
			@NonNull final List<Candidate> candidatesToUpdate,
			@NonNull final PPOrderChangedEvent ppOrderChangedEvent)
	{
		final DocStatus newDocStatusFromEvent = ppOrderChangedEvent.getNewDocStatus();
		// final CandidateStatus newCandidateStatus = EventUtil.getCandidateStatus(newDocStatusFromEvent);
		final BigDecimal newPlannedQty = ppOrderChangedEvent.getNewQtyRequired();

		final List<Candidate> updatedCandidates = new ArrayList<>();
		for (final Candidate candidateToUpdate : candidatesToUpdate)
		{
			final ProductionDetail productionDetailToUpdate = ProductionDetail.cast(candidateToUpdate.getBusinessCaseDetail());
			if (productionDetailToUpdate.getPpOrderLineId() > 0)
			{
				continue; // this is a line's candidate; deal with it in the other method
			}

			final ProductionDetail updatedProductionDetail = productionDetailToUpdate.toBuilder()
					.ppOrderDocStatus(newDocStatusFromEvent)
					.qty(newPlannedQty)
					.build();

			final BigDecimal newCandidateQty = newPlannedQty.max(candidateToUpdate.computeActualQty());

			final Candidate updatedCandidate = candidateToUpdate.toBuilder()
					// .status(newCandidateStatus)
					.businessCaseDetail(updatedProductionDetail)
					.materialDescriptor(candidateToUpdate.getMaterialDescriptor().withQuantity(newCandidateQty))
					.build();
			updatedCandidates.add(updatedCandidate);
		}

		return updatedCandidates;
	}

	private static List<Candidate> processPPOrderLinesChanges(
			@NonNull final List<Candidate> candidatesToUpdate,
			@NonNull final DocStatus newDocStatusFromEvent,
			@NonNull final List<ChangedPPOrderLineDescriptor> ppOrderLineChanges)
	{
		// final CandidateStatus newCandidateStatus = EventUtil.getCandidateStatus(newDocStatusFromEvent);

		final ImmutableMap<Integer, ChangedPPOrderLineDescriptor> ppOrderLineChangesByPPOrderLineId = Maps.uniqueIndex(ppOrderLineChanges, ChangedPPOrderLineDescriptor::getOldPPOrderLineId);

		final List<Candidate> updatedCandidates = new ArrayList<>();
		for (final Candidate candidateToUpdate : candidatesToUpdate)
		{
			final ProductionDetail productionDetailToUpdate = ProductionDetail.cast(candidateToUpdate.getBusinessCaseDetail());
			if (productionDetailToUpdate.getPpOrderLineId() <= 0)
			{
				continue; // this is the header's candidate; deal with it in the other method
			}

			final ChangedPPOrderLineDescriptor changeDescriptor = ppOrderLineChangesByPPOrderLineId.get(productionDetailToUpdate.getPpOrderLineId());
			
			final Candidate updatedCandidate = processPPOrderLinesChanges(candidateToUpdate, newDocStatusFromEvent, changeDescriptor);
			updatedCandidates.add(updatedCandidate);
		}

		return updatedCandidates;
	}

	private static Candidate processPPOrderLinesChanges(
			@NonNull final Candidate candidateToUpdate,
			@NonNull final DocStatus newDocStatusFromEvent,
			@NonNull final ChangedPPOrderLineDescriptor ppOrderLineChange)
	{
		final ProductionDetail productionDetailToUpdate = ProductionDetail.cast(candidateToUpdate.getBusinessCaseDetail());
		if (productionDetailToUpdate.getPpOrderLineId() <= 0)
		{
			throw new AdempiereException("Invalid order BOM line candidate: " + candidateToUpdate);
		}

		final BigDecimal newPlannedQty = ppOrderLineChange.getNewQtyRequired();

		final ProductionDetail updatedProductionDetail = productionDetailToUpdate.toBuilder()
				.ppOrderDocStatus(newDocStatusFromEvent)
				.ppOrderLineId(ppOrderLineChange.getNewPPOrderLineId())
				.qty(newPlannedQty)
				.build();

		final BigDecimal newCandidateQty = newPlannedQty.max(candidateToUpdate.computeActualQty());

		final Candidate updatedCandidate = candidateToUpdate.toBuilder()
				// .status(newCandidateStatus)
				.businessCaseDetail(updatedProductionDetail)
				.materialDescriptor(candidateToUpdate.getMaterialDescriptor().withQuantity(newCandidateQty))
				.build();
		
		return updatedCandidate;
	}

}
