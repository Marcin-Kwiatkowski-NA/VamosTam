package com.mkpw.blaBlaTwo.services;

import com.mkpw.blaBlaTwo.entity.RideEntity;
import com.mkpw.blaBlaTwo.repository.RideRepository;
import com.mkpw.blaBlaTwo.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

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
    public Optional<RideEntity> getRideByRideId(String rideId) {

        return repository.findById(UUID.fromString(rideId));
    }

}
