/*
 * #%L
 * de-metas-common-shipping
 * %%
 * Copyright (C) 2020 metas GmbH
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

package de.metas.common.shipping.receiptcandidate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
public class JsonResponseReceiptCandidates
{
	String transactionKey;

	List<JsonResponseReceiptCandidate> items;

	boolean hasMoreItems;

	@Builder
	@JsonCreator
	private JsonResponseReceiptCandidates(
			@JsonProperty("transactionKey") @NonNull final String transactionKey,
			@JsonProperty("items") @Singular @NonNull final List<JsonResponseReceiptCandidate> items,
			@JsonProperty("hasMoreItems") @NonNull final Boolean hasMoreItems)
	{
		this.transactionKey = transactionKey;
		this.items = items;
		this.hasMoreItems = hasMoreItems;
	}
}
