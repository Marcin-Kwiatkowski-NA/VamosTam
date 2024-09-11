package mkpw.blablatwo.services;

import mkpw.blablatwo.entity.RideEntity;
import mkpw.blablatwo.repository.RideRepository;
import mkpw.blablatwo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RideServiceImpl implements RideService {
    private final RideRepository repository;
    private final UserRepository userRepo;

    public RideServiceImpl(RideRepository repository, UserRepository userRepo) {
        this.repository = repository;
        this.userRepo = userRepo;
    }

    @Override
    public Iterable<RideEntity> getRidesByDriverId(String driverId) {
        return repository.findByDriverId(UUID.fromString(driverId));
    }

    @Override
    public RideEntity getRideByRideId(String rideId) {

        return repository.findById(UUID.fromString(rideId)).orElseThrow();
    }

}
