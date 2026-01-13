package com.blablatwo.city;

import java.util.Optional;

public interface PhotonService {

    /**
     * Search for a city by name using Photon geocoding API.
     * If the city exists in the database, returns it.
     * If not found in database but found in Photon, creates and returns it.
     *
     * @param cityName the name of the city to search for
     * @return Optional containing City (existing or newly created), empty if not found
     */
    Optional<City> resolveCity(String cityName);
}
