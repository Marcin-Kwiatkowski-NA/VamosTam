package com.blablatwo.ride;

import com.blablatwo.traveler.Traveler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long>, JpaSpecificationExecutor<Ride> {

    List<Ride> findByPassengersContaining(Traveler passenger);

    List<Ride> findByDriverId(Long driverId);
}
