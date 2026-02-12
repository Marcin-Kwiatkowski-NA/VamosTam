package com.blablatwo.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatExternalMetaRepository extends JpaRepository<SeatExternalMeta, Long> {

    Optional<SeatExternalMeta> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<SeatExternalMeta> findAllByIdIn(Collection<Long> seatIds);
}
