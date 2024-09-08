package com.mkpw.blaBlaTwo.services;

import com.mkpw.blaBlaTwo.entity.RideEntity;
import jakarta.validation.Valid;

import java.util.Optional;

public interface RideService {
    Iterable<RideEntity> getRidesByDriverId (String driverId);
    Optional<RideEntity> getRideByRideId (String driverId);
}
