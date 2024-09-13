package mkpw.blablatwo.services.ride;

import mkpw.blablatwo.entity.RideEntity;

public interface RideService {
    Iterable<RideEntity> getRidesByDriverId (String driverId);
    RideEntity getRideById(String driverId);
}
