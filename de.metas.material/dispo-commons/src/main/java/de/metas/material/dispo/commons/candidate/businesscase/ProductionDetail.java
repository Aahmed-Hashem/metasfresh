package de.metas.material.dispo.commons.candidate.businesscase;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import de.metas.material.dispo.commons.candidate.CandidateBusinessCase;
import de.metas.material.dispo.model.I_MD_Candidate_Prod_Detail;
import de.metas.util.Check;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-material-dispo
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
@Value
public class ProductionDetail implements BusinessCaseDetail
{
	public static ProductionDetail castOrNull(@Nullable final BusinessCaseDetail businessCaseDetail)
	{
		final boolean canBeCast = businessCaseDetail != null && businessCaseDetail instanceof ProductionDetail;
		return canBeCast ? cast(businessCaseDetail) : null;
	}

	public static ProductionDetail cast(@NonNull final BusinessCaseDetail businessCaseDetail)
	{
		return (ProductionDetail)businessCaseDetail;
	}

	public static ProductionDetail forProductionDetailRecord(
			@NonNull final I_MD_Candidate_Prod_Detail productionDetailRecord)
	{
		final ProductionDetail productionDetail = ProductionDetail.builder()
				.advised(Flag.of(productionDetailRecord.isAdvised()))
				.pickDirectlyIfFeasible(Flag.of(productionDetailRecord.isPickDirectlyIfFeasible()))
				.description(productionDetailRecord.getDescription())
				.plantId(productionDetailRecord.getPP_Plant_ID())
				.productBomLineId(productionDetailRecord.getPP_Product_BOMLine_ID())
				.productPlanningId(productionDetailRecord.getPP_Product_Planning_ID())
				.ppOrderId(productionDetailRecord.getPP_Order_ID())
				.ppOrderLineId(productionDetailRecord.getPP_Order_BOMLine_ID())
				.ppOrderDocStatus(productionDetailRecord.getPP_Order_DocStatus())
				.qty(productionDetailRecord.getPlannedQty())
				.build();

		return productionDetail;
	}

	int plantId;

	int productPlanningId;

	int productBomLineId;

	String description;

	int ppOrderId;

	String ppOrderDocStatus;

	int ppOrderLineId;

	Flag advised;

	Flag pickDirectlyIfFeasible;

	BigDecimal qty;

	@Builder(toBuilder = true)
	private ProductionDetail(
			final int plantId,
			final int productPlanningId,
			final int productBomLineId,
			final String description,
			final int ppOrderId,
			final String ppOrderDocStatus,
			final int ppOrderLineId,
			@NonNull final Flag advised,
			@NonNull final Flag pickDirectlyIfFeasible,
			@NonNull final BigDecimal qty)
	{
		this.advised = advised;
		this.pickDirectlyIfFeasible = pickDirectlyIfFeasible;

		final boolean detailIsAboutPPOrderHeader = productBomLineId <= 0;
		if (Flag.TRUE.equals(advised) && detailIsAboutPPOrderHeader)
		{
			// plantId needs to be available when using this productionDetail to request a ppOrder being created
			Check.errorIf(plantId <= 0, "Parameter plantId needs to be >= 0 for and advised PPOrder 'Header' productionDetail");
		}

		this.plantId = plantId;

		this.productPlanningId = productPlanningId;
		this.productBomLineId = productBomLineId;
		this.description = description;
		this.ppOrderId = ppOrderId;
		this.ppOrderDocStatus = ppOrderDocStatus;
		this.ppOrderLineId = ppOrderLineId;
		this.qty = qty;
	}

	@Override
	public CandidateBusinessCase getCandidateBusinessCase()
	{
		return CandidateBusinessCase.PRODUCTION;
	}
}
