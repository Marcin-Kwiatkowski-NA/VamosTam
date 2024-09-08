package com.mkpw.blaBlaTwo.services;

import com.mkpw.blaBlaTwo.entity.RideEntity;

public interface RideService {
    Iterable<RideEntity> getRidesByDriverId (String driverId);
    RideEntity getRideByRideId (String driverId);
}
