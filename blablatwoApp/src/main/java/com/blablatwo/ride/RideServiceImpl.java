package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityRepository;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.NoSuchTravelerException;
import com.blablatwo.exceptions.RideFullException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.TravelerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideMapper rideMapper;
    private final CityRepository cityRepository;
    private final CityMapper cityMapper;
    private final TravelerRepository travelerRepository;

    public RideServiceImpl(RideRepository rideRepository, RideMapper rideMapper,
                           CityRepository cityRepository, CityMapper cityMapper,
                           TravelerRepository travelerRepository) {
        this.rideRepository = rideRepository;
        this.rideMapper = rideMapper;
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
        this.travelerRepository = travelerRepository;
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
        City origin = getOrCreateCity(ride.origin());
        City destination = getOrCreateCity(ride.destination());
        var newRideEntity = rideMapper.rideCreationDtoToEntity(ride);
        newRideEntity.setOrigin(origin);
        newRideEntity.setDestination(destination);
        return rideMapper.rideEntityToRideResponseDto(
                rideRepository.save(newRideEntity));
    }

    private City getOrCreateCity(@Valid @NotNull CityDto city) {
        return cityRepository.findByOsmId(city.osmId())
                .orElseGet(() -> cityRepository.save(cityMapper.cityDtoToEntity(city))
        );
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

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponseDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable) {
        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(RideStatus.OPEN))
                .and(RideSpecifications.originNameContains(criteria.originCityName()))
                .and(RideSpecifications.destinationNameContains(criteria.destinationCityName()))
                .and(RideSpecifications.hasMinAvailableSeats(
                        criteria.minAvailableSeats() != null ? criteria.minAvailableSeats() : 1))
                .and(RideSpecifications.departureAfter(
                        criteria.departureDate() != null ?
                                criteria.departureDate().atStartOfDay() : LocalDateTime.now()));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(RideSpecifications.departureBefore(
                    criteria.departureDateTo().atTime(23, 59, 59)));
        }

        return rideRepository.findAll(spec, pageable)
                .map(rideMapper::rideEntityToRideResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponseDto> getAllRides(Pageable pageable) {
        return rideRepository.findAll(pageable)
                .map(rideMapper::rideEntityToRideResponseDto);
    }

    @Override
    @Transactional
    public RideResponseDto bookRide(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        Traveler passenger = travelerRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchTravelerException(passengerId));

        if (ride.getRideStatus() != RideStatus.OPEN) {
            throw new RideNotBookableException(rideId, ride.getRideStatus().name());
        }

        if (ride.getAvailableSeats() <= 0) {
            throw new RideFullException(rideId);
        }

        if (ride.getPassengers() != null &&
                ride.getPassengers().stream().anyMatch(p -> p.getId().equals(passengerId))) {
            throw new AlreadyBookedException(rideId, passengerId);
        }

        if (ride.getPassengers() == null) {
            ride.setPassengers(new ArrayList<>());
        }
        ride.getPassengers().add(passenger);
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        ride.setLastModified(Instant.now());

        if (ride.getAvailableSeats() == 0) {
            ride.setRideStatus(RideStatus.FULL);
        }

        return rideMapper.rideEntityToRideResponseDto(rideRepository.save(ride));
    }

    @Override
    @Transactional
    public RideResponseDto cancelBooking(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        Traveler passenger = travelerRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchTravelerException(passengerId));

        if (ride.getPassengers() == null ||
                ride.getPassengers().stream().noneMatch(p -> p.getId().equals(passengerId))) {
            throw new BookingNotFoundException(rideId, passengerId);
        }

        ride.getPassengers().removeIf(p -> p.getId().equals(passengerId));
        ride.setAvailableSeats(ride.getAvailableSeats() + 1);
        ride.setLastModified(Instant.now());

        if (ride.getRideStatus() == RideStatus.FULL) {
            ride.setRideStatus(RideStatus.OPEN);
        }

        return rideMapper.rideEntityToRideResponseDto(rideRepository.save(ride));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getRidesForPassenger(Long passengerId) {
        Traveler passenger = travelerRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchTravelerException(passengerId));

        return rideRepository.findByPassengersContaining(passenger)
                .stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
    }
}
