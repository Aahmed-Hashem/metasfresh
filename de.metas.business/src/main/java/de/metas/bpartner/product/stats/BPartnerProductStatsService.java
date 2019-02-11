package de.metas.bpartner.product.stats;

import java.time.ZonedDateTime;
import java.util.Set;

import org.compiere.util.TimeUtil;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import de.metas.bpartner.BPartnerId;
import de.metas.product.ProductId;
import lombok.NonNull;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2019 metas GmbH
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
public class BPartnerProductStatsService
{
	private final BPartnerProductStatsRepository statsRepo;

	public BPartnerProductStatsService(
			@NonNull final BPartnerProductStatsRepository statsRepo)
	{
		this.statsRepo = statsRepo;
	}

	public ImmutableMap<ProductId, BPartnerProductStats> getByPartnerAndProducts(
			@NonNull final BPartnerId bpartnerId,
			@NonNull final Set<ProductId> productIds)
	{
		return statsRepo.getByPartnerAndProducts(bpartnerId, productIds);
	}

	public void handleEvent(@NonNull final InOutChangedEvent event)
	{
		final BPartnerId bpartnerId = event.getBpartnerId();
		final Set<ProductId> productIds = event.getProductIds();

		if (event.isReversal())
		{
			statsRepo.refreshByPartnerAndProducts(bpartnerId, productIds);
		}
		else
		{
			final ZonedDateTime movementDate = TimeUtil.asZonedDateTime(event.getMovementDate());
			final boolean isShipment = event.getSoTrx().isSales();

			final ImmutableMap<ProductId, BPartnerProductStats> statsByProductId = statsRepo.getByPartnerAndProducts(bpartnerId, productIds);

			for (final ProductId productId : productIds)
			{
				BPartnerProductStats stats = statsByProductId.get(productId);
				if (stats == null)
				{
					stats = BPartnerProductStats.newInstance(bpartnerId, productId);
				}

				if (isShipment)
				{
					stats.updateLastShipmentDate(movementDate);
				}
				else
				{
					stats.updateLastReceiptDate(movementDate);
				}

				statsRepo.save(stats);
			}
		}

	}
}
