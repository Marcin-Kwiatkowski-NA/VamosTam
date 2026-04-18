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

    @Query("""
            SELECT COUNT(s) > 0 FROM SavedSearch s
            WHERE s.user.id = :userId
              AND s.originOsmId = :originOsmId
              AND s.destinationOsmId = :destOsmId
              AND s.departureDate = :departureDate
              AND s.searchType = :searchType
              AND s.active = true
            """)
    boolean existsActiveSearch(
            @Param("userId") Long userId,
            @Param("originOsmId") Long originOsmId,
            @Param("destOsmId") Long destOsmId,
            @Param("departureDate") LocalDate departureDate,
            @Param("searchType") SearchType searchType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SavedSearch s SET s.active = false " +
            "WHERE s.active = true AND s.departureDate < :today")
    int deactivateExpired(@Param("today") LocalDate today);

    /**
     * Finds saved searches matching a given route (exact OSM ID match or proximity within radius).
     * Excludes the creator's own searches. Returns a projection with per-side distances and the
     * exact-match flag so the matcher can record the winning stop-pair's offsets.
     */
    @Query(value = """
            SELECT ss.id AS savedSearchId,
              ROUND(ST_Distance(
                ST_SetSRID(ST_MakePoint(ss.origin_lon, ss.origin_lat), 4326)::geography,
                ST_SetSRID(ST_MakePoint(:originLon, :originLat), 4326)::geography
              ))::int AS originDistanceM,
              ROUND(ST_Distance(
                ST_SetSRID(ST_MakePoint(ss.destination_lon, ss.destination_lat), 4326)::geography,
                ST_SetSRID(ST_MakePoint(:destLon, :destLat), 4326)::geography
              ))::int AS destinationDistanceM,
              (ss.origin_osm_id = :originOsmId AND ss.destination_osm_id = :destOsmId) AS exactMatch
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
    List<SearchMatchProjection> findMatchingSearches(
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
