package com.blablatwo.ride;

import com.blablatwo.exceptions.ETagMismatchException;
import com.blablatwo.exceptions.MissingETagHeaderException;
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

    public RideServiceImpl(RideRepository rideRepository, RideMapper rideMapper) {
        this.rideRepository = rideRepository;
        this.rideMapper = rideMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RideResponseDto> getById(long id) {
        return rideRepository.findById(id)
                .map(rideMapper::rideEntityToRideResponseDto);
    }

    @Override
    @Transactional
    public RideResponseDto create(RideCreationDto ride) {
        var newRideEntity = rideMapper.rideCreationDtoToEntity(ride);
        return rideMapper.rideEntityToRideResponseDto(
                rideRepository.save(newRideEntity));
    }

    @Override
    @Transactional
    public RideResponseDto update(RideCreationDto ride, long id, String ifMatch) {
        var existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));
        var existingETag = String.valueOf(existingRide.getLastModified());

        eTagCheck(ifMatch, existingETag);

        rideMapper.update(existingRide, ride);
        rideRepository.save(existingRide);
        return rideMapper.rideEntityToRideResponseDto(existingRide);
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (rideRepository.existsById(id)) {
            rideRepository.deleteById(id);
        } else {
            throw new NoSuchRideException(id);
        }
    }

    void eTagCheck(String ifMatch, String existingETag) {
        if (ifMatch == null) {
            throw new MissingETagHeaderException();
        }

        if(! existingETag.equals(ifMatch)) {
            throw new ETagMismatchException();
        }
    }
}
