package de.metas.ui.web.order.sales.purchasePlanning.view;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.compiere.model.I_C_OrderLine;
import org.compiere.util.TimeUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import de.metas.order.OrderLineId;
import de.metas.printing.esb.base.util.Check;
import de.metas.purchasecandidate.PurchaseCandidate;
import de.metas.purchasecandidate.PurchaseCandidateRepository;
import lombok.Builder;
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

class PurchaseRowsSaver
{
	private final PurchaseCandidateRepository purchaseCandidatesRepo;

	private final List<PurchaseRow> groupingRows;

	@Builder
	private PurchaseRowsSaver(
			@NonNull final PurchaseCandidateRepository purchaseCandidatesRepo,
			@NonNull final List<PurchaseRow> grouppingRows)
	{
		this.purchaseCandidatesRepo = purchaseCandidatesRepo;

		this.groupingRows = grouppingRows;
	}

	public List<PurchaseCandidate> save()
	{
		final Set<OrderLineId> salesOrderLineIds = groupingRows.stream()
				.map(PurchaseRow::getPurchaseDemandId)
				.filter(id -> id != null)
				.filter(id -> I_C_OrderLine.Table_Name.equals(id.getTableName()))
				.map(PurchaseDemandId::getRecordId)
				.map(OrderLineId::ofRepoId)
				.collect(ImmutableSet.toImmutableSet());

		final Map<Integer, PurchaseCandidate> existingPurchaseCandidatesById = purchaseCandidatesRepo
				.streamAllBySalesOrderLineIds(salesOrderLineIds)
				.collect(ImmutableMap
						.toImmutableMap(
								PurchaseCandidate::getPurchaseCandidateId,
								Function.identity()));

		final List<PurchaseCandidate> purchaseCandidatesToSave = groupingRows.stream()
				.flatMap(grouppingRow -> grouppingRow.getIncludedRows().stream()) // purchase candidate lines
				.map(row -> updatePurchaseCandidate(row, existingPurchaseCandidatesById))
				.collect(ImmutableList.toImmutableList());

		purchaseCandidatesRepo.saveAll(purchaseCandidatesToSave);

		//
		// Delete remaining candidates:
		final Set<Integer> purchaseCandidateIdsSaved = purchaseCandidatesToSave.stream()
				.map(PurchaseCandidate::getPurchaseCandidateId)
				.collect(ImmutableSet.toImmutableSet());
		final Set<Integer> purchaseCandidateIdsToDelete = existingPurchaseCandidatesById.keySet().stream()
				.filter(id -> !purchaseCandidateIdsSaved.contains(id))
				.collect(ImmutableSet.toImmutableSet());
		purchaseCandidatesRepo.deleteByIds(purchaseCandidateIdsToDelete);

		return purchaseCandidatesToSave;
	}

	private PurchaseCandidate updatePurchaseCandidate(
			@NonNull final PurchaseRow purchaseRow,
			@NonNull final Map<Integer, PurchaseCandidate> existingPurchaseCandidatesById)
	{
		Check.errorUnless(PurchaseRowType.LINE.equals(purchaseRow.getType()),
				"The given row's type needs to be {}, but is {}; purchaseRow={}", PurchaseRowType.LINE, purchaseRow.getType(), purchaseRow);

		final PurchaseCandidate purchaseCandidate = existingPurchaseCandidatesById.get(purchaseRow.getPurchaseCandidateId());
		Check.errorIf(purchaseCandidate == null,
				"Missing purchaseCandidate with C_PurchaseCandidate_ID={}; purchaseRow={}, existingPurchaseCandidatesById={}",
				purchaseRow.getPurchaseCandidateId(), purchaseRow, existingPurchaseCandidatesById);

		purchaseCandidate.setQtyToPurchase(purchaseRow.getQtyToPurchase());
		purchaseCandidate.setDateRequired(TimeUtil.asLocalDateTime(purchaseRow.getDatePromised()));

		Check.errorIf(
				purchaseCandidate.isProcessedOrLocked() && purchaseCandidate.hasChanges(),
				"The given purchaseRow has changes, but its purchaseCandidate is not editable; purchaseRow={}; purchaseCandidate={}",
				purchaseRow, purchaseCandidate);

		return purchaseCandidate;
	}
}
