package com.blablatwo.city.geocoding.dto;

import java.util.List;

/**
 * Response from the Photon-like (GeoNames-based) geocoding API.
 */
public record PhotonLikeResponse(
        List<PhotonLikeFeature> features
) {
}
