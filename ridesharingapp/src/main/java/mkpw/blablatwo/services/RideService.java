package mkpw.blablatwo.services;

import mkpw.blablatwo.entity.RideEntity;

public interface RideService {
    Iterable<RideEntity> getRidesByDriverId (String driverId);
    RideEntity getRideByRideId (String driverId);
}
