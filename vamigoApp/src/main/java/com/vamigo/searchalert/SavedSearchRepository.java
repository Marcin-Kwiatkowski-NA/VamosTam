package com.vamigo.searchalert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByUserIdAndActiveTrue(Long userId);

    long countByUserIdAndActiveTrueAndAutoCreatedFalse(Long userId);

    Optional<SavedSearch> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndOriginOsmIdAndDestinationOsmIdAndDepartureDateAndSearchTypeAndActiveTrue(
            Long userId, Long originOsmId, Long destinationOsmId, LocalDate departureDate, SearchType searchType);

    @Modifying
    @Query("UPDATE SavedSearch s SET s.active = false " +
            "WHERE s.active = true AND s.departureDate < :today")
    int deactivateExpired(@Param("today") LocalDate today);

    /**
     * Finds saved searches matching a given route (exact OSM ID match or proximity within radius).
     * Excludes the creator's own searches. Returns a projection with the exact_match flag.
     */
    @Query(value = """
            SELECT ss.*,
              CASE WHEN ss.origin_osm_id = :originOsmId AND ss.destination_osm_id = :destOsmId
                   THEN true ELSE false END as exact_match
            FROM saved_search ss
            WHERE ss.active = true
              AND ss.search_type = :#{#searchType.name()}
              AND ss.departure_date = :departureDate
              AND ss.user_id != :creatorUserId
              AND (
                (ss.origin_osm_id = :originOsmId AND ss.destination_osm_id = :destOsmId)
                OR
                (ST_DWithin(
                  ST_SetSRID(ST_MakePoint(ss.origin_lon, ss.origin_lat), 4326)::geography,
                  ST_SetSRID(ST_MakePoint(:originLon, :originLat), 4326)::geography, :radiusMeters
                ) AND ST_DWithin(
                  ST_SetSRID(ST_MakePoint(ss.destination_lon, ss.destination_lat), 4326)::geography,
                  ST_SetSRID(ST_MakePoint(:destLon, :destLat), 4326)::geography, :radiusMeters
                ))
              )
            """, nativeQuery = true)
    List<Object[]> findMatchingSearches(
            @Param("searchType") SearchType searchType,
            @Param("departureDate") LocalDate departureDate,
            @Param("creatorUserId") Long creatorUserId,
            @Param("originOsmId") Long originOsmId,
            @Param("destOsmId") Long destOsmId,
            @Param("originLat") double originLat,
            @Param("originLon") double originLon,
            @Param("destLat") double destLat,
            @Param("destLon") double destLon,
            @Param("radiusMeters") double radiusMeters);

    void deleteByUserIdAndOriginOsmIdAndDestinationOsmIdAndDepartureDateAndSearchTypeAndAutoCreatedTrue(
            Long userId, Long originOsmId, Long destinationOsmId, LocalDate departureDate, SearchType searchType);
}
