package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityRepository;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideMapper rideMapper;
    private final CityRepository cityRepository;
    private final CityMapper cityMapper;


    public RideServiceImpl(RideRepository rideRepository, RideMapper rideMapper, CityRepository cityRepository, CityMapper cityMapper) {
        this.rideRepository = rideRepository;
        this.rideMapper = rideMapper;
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RideResponseDto> getById(Long id) {
        return rideRepository.findById(id)
                .map(rideMapper::rideEntityToRideResponseDto);
    }

    @Override
    @Transactional
    public RideResponseDto create(RideCreationDto ride) {
        saveIfCityNotExists(ride);
        var newRideEntity = rideMapper.rideCreationDtoToEntity(ride);
        return rideMapper.rideEntityToRideResponseDto(
                rideRepository.save(newRideEntity));
    }

    private void saveIfCityNotExists(RideCreationDto ride) {
        if (cityRepository.findByOsmId(ride.origin().osmId())
                .isEmpty()) {
            City origin = cityMapper.cityDtoToEntity(ride.origin());
            cityRepository.save(origin);
        }
        if (cityRepository.findByOsmId(ride.destination().osmId())
                .isEmpty()) {
            City destination = cityMapper.cityDtoToEntity(ride.destination());
            cityRepository.save(destination);
        }
    }

    @Override
    @Transactional
    public RideResponseDto update(RideCreationDto ride, Long id) {
        var existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        rideMapper.update(existingRide, ride);
        return rideMapper.rideEntityToRideResponseDto(existingRide);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (rideRepository.existsById(id)) {
            rideRepository.deleteById(id);
        } else {
            throw new NoSuchRideException(id);
        }
    }
}
