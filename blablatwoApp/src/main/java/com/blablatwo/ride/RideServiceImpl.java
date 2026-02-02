package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityResolutionService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideMapper rideMapper;
    private final CityMapper cityMapper;
    private final CityResolutionService cityResolutionService;
    private final TravelerRepository travelerRepository;
    private final RideResponseEnricher rideResponseEnricher;

    public RideServiceImpl(RideRepository rideRepository, RideMapper rideMapper,
                           CityMapper cityMapper, CityResolutionService cityResolutionService,
                           TravelerRepository travelerRepository,
                           RideResponseEnricher rideResponseEnricher) {
        this.rideRepository = rideRepository;
        this.rideMapper = rideMapper;
        this.cityMapper = cityMapper;
        this.cityResolutionService = cityResolutionService;
        this.travelerRepository = travelerRepository;
        this.rideResponseEnricher = rideResponseEnricher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RideResponseDto> getById(Long id) {
        return rideRepository.findById(id)
                .map(ride -> rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride)));
    }

    @Override
    @Transactional
    public RideResponseDto create(RideCreationDto ride) {
        Traveler driver = travelerRepository.findById(ride.driverId())
                .orElseThrow(() -> new NoSuchTravelerException(ride.driverId()));
        City origin = cityResolutionService.resolveCityByPlaceId(ride.originPlaceId(), "pl");
        City destination = cityResolutionService.resolveCityByPlaceId(ride.destinationPlaceId(), "pl");
        var newRideEntity = rideMapper.rideCreationDtoToEntity(ride);
        newRideEntity.setDriver(driver);
        newRideEntity.setOrigin(origin);
        newRideEntity.setDestination(destination);
        Ride savedRide = rideRepository.save(newRideEntity);
        return rideResponseEnricher.enrich(savedRide, rideMapper.rideEntityToRideResponseDto(savedRide));
    }

    @Override
    @Transactional
    public RideResponseDto update(RideCreationDto ride, Long id) {
        var existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        rideMapper.update(existingRide, ride);

        // Resolve cities BEFORE mapping to DTO (enricher reads from entity)
        City origin = cityResolutionService.resolveCityByPlaceId(ride.originPlaceId(), "pl");
        City destination = cityResolutionService.resolveCityByPlaceId(ride.destinationPlaceId(), "pl");
        existingRide.setOrigin(origin);
        existingRide.setDestination(destination);

        return rideResponseEnricher.enrich(existingRide, rideMapper.rideEntityToRideResponseDto(existingRide));
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
                .and(RideSpecifications.originPlaceIdEquals(criteria.originPlaceId()))
                .and(RideSpecifications.destinationPlaceIdEquals(criteria.destinationPlaceId()))
                .and(RideSpecifications.hasMinAvailableSeats(criteria.minAvailableSeats()))
                .and(RideSpecifications.departureAfter(calculateDepartureFrom(criteria)));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(RideSpecifications.departureBefore(
                    criteria.departureDateTo().atTime(23, 59, 59)));
        }

        Page<Ride> ridePage = rideRepository.findAll(spec, pageable);
        List<Ride> rides = ridePage.getContent();

        // Use language from criteria for response localization
        String lang = criteria.lang() != null ? criteria.lang() : "pl";
        List<RideResponseDto> dtos = rides.stream()
                .map(ride -> mapRideToResponseDto(ride, lang))
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(rides, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
    }

    private RideResponseDto mapRideToResponseDto(Ride ride, String lang) {
        RideResponseDto dto = rideMapper.rideEntityToRideResponseDto(ride);
        // Re-map cities with correct language
        return dto.toBuilder()
                .origin(cityMapper.cityEntityToCityDto(ride.getOrigin(), lang))
                .destination(cityMapper.cityEntityToCityDto(ride.getDestination(), lang))
                .build();
    }

    private LocalDateTime calculateDepartureFrom(RideSearchCriteriaDto criteria) {
        if (criteria.departureDate() == null) {
            return LocalDateTime.now();
        }

        if (criteria.departureTimeFrom() != null) {
            return criteria.departureDate().atTime(criteria.departureTimeFrom());
        }

        if (criteria.departureDate().equals(LocalDate.now())) {
            return LocalDateTime.now();
        }

        return criteria.departureDate().atStartOfDay();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponseDto> getAllRides(Pageable pageable) {
        Page<Ride> ridePage = rideRepository.findAll(pageable);
        List<Ride> rides = ridePage.getContent();
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(rides, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
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

        Ride savedRide = rideRepository.save(ride);
        return rideResponseEnricher.enrich(savedRide, rideMapper.rideEntityToRideResponseDto(savedRide));
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

        Ride savedRide = rideRepository.save(ride);
        return rideResponseEnricher.enrich(savedRide, rideMapper.rideEntityToRideResponseDto(savedRide));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getRidesForPassenger(Long passengerId) {
        Traveler passenger = travelerRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchTravelerException(passengerId));

        List<Ride> rides = rideRepository.findByPassengersContaining(passenger);
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        return rideResponseEnricher.enrich(rides, dtos);
    }
}
