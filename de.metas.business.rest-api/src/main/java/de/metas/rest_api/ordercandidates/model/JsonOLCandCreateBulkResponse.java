package de.metas.rest_api.ordercandidates.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import lombok.Value;


/*
 * #%L
 * de.metas.ordercandidate.rest-api
 * %%
 * Copyright (C) 2018 metas GmbH
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

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@Value
public class JsonOLCandCreateBulkResponse
{
	public static JsonOLCandCreateBulkResponse of(final List<JsonOLCand> olCands)
	{
		return new JsonOLCandCreateBulkResponse(olCands);
	}

	@JsonProperty("result")
	private final List<JsonOLCand> result;

	@JsonCreator
	private JsonOLCandCreateBulkResponse(@JsonProperty("result") final List<JsonOLCand> olCands)
	{
		this.result = ImmutableList.copyOf(olCands);
	}

	public JsonOLCand getSingleResult()
	{
		if (result.size() != 1)
		{
			throw new OrderCandidateRestApiException("Expected single result but we got: " + result);
		}
		return result.get(0);
	}
}
