package mkpw.blablatwo.services.ride;

import mkpw.blablatwo.entity.RideEntity;
import mkpw.blablatwo.exeptions.runtime.NoSuchRideException;
import mkpw.blablatwo.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RideServiceImpl implements RideService {
    private final RideRepository repository;

    public RideServiceImpl(RideRepository repository) {
        this.repository = repository;
    }

    @Override
    public Iterable<RideEntity> getRidesByDriverId(String driverId) {
        return repository.findByDriverId(UUID.fromString(driverId));
    }

    @Override
    public RideEntity getRideById(String rideId) {

        return repository.findById(UUID.fromString(rideId))
                .orElseThrow(NoSuchRideException::new);
    }
}
