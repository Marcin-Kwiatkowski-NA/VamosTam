package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityResolutionService;
import com.blablatwo.domain.Segment;
import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import com.blablatwo.domain.TimeSlot;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.CannotBookException;
import com.blablatwo.exceptions.CannotCreateRideException;
import com.blablatwo.exceptions.ExternalRideNotBookableException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.RideFullException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
import com.blablatwo.user.exception.NoSuchUserException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideMapper rideMapper;
    private final CityMapper cityMapper;
    private final CityResolutionService cityResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final RideResponseEnricher rideResponseEnricher;
    private final CapabilityService capabilityService;

    public RideServiceImpl(RideRepository rideRepository, RideMapper rideMapper,
                           CityMapper cityMapper, CityResolutionService cityResolutionService,
                           UserAccountRepository userAccountRepository,
                           RideResponseEnricher rideResponseEnricher,
                           CapabilityService capabilityService) {
        this.rideRepository = rideRepository;
        this.rideMapper = rideMapper;
        this.cityMapper = cityMapper;
        this.cityResolutionService = cityResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.rideResponseEnricher = rideResponseEnricher;
        this.capabilityService = capabilityService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RideResponseDto> getById(Long id) {
        return rideRepository.findById(id)
                .map(ride -> rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride)));
    }

    @Override
    @Transactional
    public RideResponseDto createForCurrentUser(RideCreationDto ride, Long userId) {
        if (!capabilityService.canCreateRide(userId)) {
            throw new CannotCreateRideException(userId);
        }

        UserAccount driver = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));
        City origin = cityResolutionService.resolveCityByPlaceId(ride.originPlaceId(), "pl");
        City destination = cityResolutionService.resolveCityByPlaceId(ride.destinationPlaceId(), "pl");

        var newRideEntity = rideMapper.rideCreationDtoToEntity(ride);
        newRideEntity.setDriver(driver);
        newRideEntity.setSegment(new Segment(origin, destination));
        newRideEntity.setTimeSlot(TimeSlot.of(ride.departureTime(), ride.isApproximate()));
        newRideEntity.setLastModified(Instant.now());

        Ride savedRide = rideRepository.save(newRideEntity);
        return rideResponseEnricher.enrich(savedRide, rideMapper.rideEntityToRideResponseDto(savedRide));
    }

    @Override
    @Transactional
    public RideResponseDto update(RideCreationDto ride, Long id) {
        var existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        rideMapper.update(existingRide, ride);

        City origin = cityResolutionService.resolveCityByPlaceId(ride.originPlaceId(), "pl");
        City destination = cityResolutionService.resolveCityByPlaceId(ride.destinationPlaceId(), "pl");
        existingRide.setSegment(new Segment(origin, destination));
        existingRide.setTimeSlot(TimeSlot.of(ride.departureTime(), ride.isApproximate()));
        existingRide.setLastModified(Instant.now());

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
        var departureFrom = TimePredicateHelper.calculateDepartureFrom(
                criteria.departureDate(), criteria.departureTimeFrom());

        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(Status.ACTIVE))
                .and(RideSpecifications.originPlaceIdEquals(criteria.originPlaceId()))
                .and(RideSpecifications.destinationPlaceIdEquals(criteria.destinationPlaceId()))
                .and(RideSpecifications.hasMinAvailableSeats(criteria.minAvailableSeats()))
                .and(RideSpecifications.departsOnOrAfter(departureFrom.date(), departureFrom.time()));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(RideSpecifications.departsOnOrBefore(
                    criteria.departureDateTo(), LocalTime.MAX));
        }

        Page<Ride> ridePage = rideRepository.findAll(spec, pageable);
        List<Ride> rides = ridePage.getContent();

        String lang = criteria.lang() != null ? criteria.lang() : "pl";
        List<RideResponseDto> dtos = rides.stream()
                .map(ride -> mapRideToResponseDto(ride, lang))
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(rides, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
    }

    private RideResponseDto mapRideToResponseDto(Ride ride, String lang) {
        RideResponseDto dto = rideMapper.rideEntityToRideResponseDto(ride);
        return dto.toBuilder()
                .origin(cityMapper.cityEntityToCityDto(ride.getSegment().getOrigin(), lang))
                .destination(cityMapper.cityEntityToCityDto(ride.getSegment().getDestination(), lang))
                .build();
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

        if (ride.getSource() != RideSource.INTERNAL) {
            throw new ExternalRideNotBookableException(rideId);
        }

        if (!capabilityService.canBook(passengerId)) {
            throw new CannotBookException(passengerId);
        }

        if (rideRepository.existsPassenger(rideId, passengerId)) {
            throw new AlreadyBookedException(rideId, passengerId);
        }

        UserAccount passenger = userAccountRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchUserException(passengerId));

        if (ride.computeRideStatus() != RideStatus.OPEN) {
            throw new RideNotBookableException(rideId, ride.computeRideStatus().name());
        }

        if (ride.getAvailableSeats() <= 0) {
            throw new RideFullException(rideId);
        }

        if (ride.getPassengers() == null) {
            ride.setPassengers(new ArrayList<>());
        }
        ride.getPassengers().add(passenger);
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        ride.setLastModified(Instant.now());


        Ride savedRide = rideRepository.save(ride);
        return rideResponseEnricher.enrich(savedRide, rideMapper.rideEntityToRideResponseDto(savedRide));
    }

    @Override
    @Transactional
    public RideResponseDto cancelBooking(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        if (ride.getSource() != RideSource.INTERNAL) {
            throw new ExternalRideNotBookableException(rideId);
        }

        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        if (ride.getPassengers() == null ||
                ride.getPassengers().stream().noneMatch(p -> p.getId().equals(passengerId))) {
            throw new BookingNotFoundException(rideId, passengerId);
        }

        ride.getPassengers().removeIf(p -> p.getId().equals(passengerId));
        ride.setAvailableSeats(ride.getAvailableSeats() + 1);
        ride.setLastModified(Instant.now());

        Ride savedRide = rideRepository.save(ride);
        return rideResponseEnricher.enrich(savedRide, rideMapper.rideEntityToRideResponseDto(savedRide));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getRidesForPassenger(Long passengerId) {
        UserAccount passenger = userAccountRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchUserException(passengerId));

        List<Ride> rides = rideRepository.findByPassengersContaining(passenger);
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        return rideResponseEnricher.enrich(rides, dtos);
    }
}
