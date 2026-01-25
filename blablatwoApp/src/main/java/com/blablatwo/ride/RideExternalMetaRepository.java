package com.blablatwo.ride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideExternalMetaRepository extends JpaRepository<RideExternalMeta, Long> {

    Optional<RideExternalMeta> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    boolean existsByContentHash(String contentHash);

    List<RideExternalMeta> findAllByRideIdIn(Collection<Long> rideIds);
}
