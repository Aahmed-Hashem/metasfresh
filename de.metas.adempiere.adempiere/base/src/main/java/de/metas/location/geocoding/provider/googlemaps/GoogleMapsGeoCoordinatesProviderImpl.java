/*
 * #%L
 * de.metas.adempiere.adempiere.base
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

package de.metas.location.geocoding.provider.googlemaps;

import com.google.common.collect.ImmutableList;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import de.metas.cache.CCache;
import de.metas.location.geocoding.GeoCoordinatesProvider;
import de.metas.location.geocoding.GeoCoordinatesRequest;
import de.metas.location.geocoding.GeographicalCoordinates;
import de.metas.logging.LogManager;
import de.metas.util.GuavaCollectors;
import lombok.NonNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GoogleMapsGeoCoordinatesProviderImpl implements GeoCoordinatesProvider
{
	private static final Logger logger = LogManager.getLogger(GoogleMapsGeoCoordinatesProviderImpl.class);

	private final CCache<GeoCoordinatesRequest, ImmutableList<GeographicalCoordinates>> coordinatesCache;

	public GoogleMapsGeoCoordinatesProviderImpl(@Value("${de.metas.location.geocoding.provider.googlemaps.cacheCapacity:200}") final int cacheCapacity)
	{

		logger.info("cacheCapacity={}", cacheCapacity);
		coordinatesCache = CCache.<GeoCoordinatesRequest, ImmutableList<GeographicalCoordinates>>builder()
				.cacheMapType(CCache.CacheMapType.LRU)
				.initialCapacity(cacheCapacity)
				.build();
	}

	@Override
	public Optional<GeographicalCoordinates> findBestCoordinates(final GeoCoordinatesRequest request)
	{
		final List<GeographicalCoordinates> coords = findAllCoordinates(request);

		if (coords.isEmpty())
		{
			return Optional.empty();
		}
		return Optional.of(coords.get(0));
	}

	@NonNull
	private ImmutableList<GeographicalCoordinates> findAllCoordinates(final @NonNull GeoCoordinatesRequest request)
	{
		final ImmutableList<GeographicalCoordinates> response = coordinatesCache.get(request);
		if (response != null)
		{
			return response;
		}

		return coordinatesCache.getOrLoad(request, this::queryAllCoordinates);
	}

	@NonNull
	private ImmutableList<GeographicalCoordinates> queryAllCoordinates(@NonNull final GeoCoordinatesRequest request)
	{
		final String formattedAddress = String.format("%s, %s %s, %s", request.getAddress(), request.getPostal(), request.getCity(), request.getCountryCode2());

		final GeoApiContext context = GoogleMapsGeoApiContext.getInstance();
		final GeocodingResult[] results = GeocodingApi
				.geocode(context, formattedAddress)
				.awaitIgnoreError();

		//noinspection ConfusingArgumentToVarargsMethod
		logger.trace("Geocoding response from google: {}", results);

		return Arrays.stream(results)
				.map(GoogleMapsGeoCoordinatesProviderImpl::toGeographicalCoordinates)
				.collect(GuavaCollectors.toImmutableList());
	}

	private static GeographicalCoordinates toGeographicalCoordinates(@NonNull final GeocodingResult result)
	{
		final LatLng ll = result.geometry.location;
		return GeographicalCoordinates.builder()
				.latitude(BigDecimal.valueOf(ll.lat))
				.longitude(BigDecimal.valueOf(ll.lng))
				.build();
	}
}
