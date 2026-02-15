package com.blablatwo.seat;

import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import com.blablatwo.exceptions.NoSuchSeatException;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationResolutionService;
import com.blablatwo.seat.dto.SeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.seat.dto.SeatSearchCriteriaDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
import com.blablatwo.user.exception.NoSuchUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatServiceImpl.class);

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;
    private final LocationResolutionService locationResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final SeatResponseEnricher seatResponseEnricher;
    private final CapabilityService capabilityService;

    @Value("${search.proximity.default-radius-km:50}")
    private double defaultRadiusKm;

    public SeatServiceImpl(SeatRepository seatRepository,
                            SeatMapper seatMapper,
                            LocationResolutionService locationResolutionService,
                            UserAccountRepository userAccountRepository,
                            SeatResponseEnricher seatResponseEnricher,
                            CapabilityService capabilityService) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.seatResponseEnricher = seatResponseEnricher;
        this.capabilityService = capabilityService;
    }

    @Override
    @Transactional
    public SeatResponseDto createForCurrentUser(SeatCreationDto dto, Long userId) {
        if (!capabilityService.isActive(userId)) {
            throw new IllegalStateException("User " + userId + " is not active");
        }

        UserAccount passenger = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));
        Location origin = locationResolutionService.resolve(dto.origin());
        Location destination = locationResolutionService.resolve(dto.destination());

        Seat seat = seatMapper.seatCreationDtoToEntity(dto);
        seat.setPassenger(passenger);
        seat.setOrigin(origin);
        seat.setDestination(destination);
        seat.setDepartureDate(dto.departureTime().toLocalDate());
        seat.setDepartureTime(dto.departureTime().toLocalTime());
        seat.setApproximate(dto.isApproximate());
        seat.setLastModified(Instant.now());

        Seat saved = seatRepository.save(seat);
        return seatResponseEnricher.enrich(saved, seatMapper.seatEntityToResponseDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeatResponseDto> getById(Long id) {
        return seatRepository.findById(id)
                .map(seat -> seatResponseEnricher.enrich(seat, seatMapper.seatEntityToResponseDto(seat)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatResponseDto> searchSeats(SeatSearchCriteriaDto criteria, Pageable pageable) {
        if (criteria.isProximityMode()) {
            return searchSeatsNearby(criteria, pageable);
        }
        return searchSeatsExact(criteria, pageable);
    }

    private Page<SeatResponseDto> searchSeatsExact(SeatSearchCriteriaDto criteria, Pageable pageable) {
        var departureFrom = TimePredicateHelper.calculateDepartureFrom(
                criteria.departureDate(), criteria.departureTimeFrom());

        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatSpecifications.originOsmIdEquals(criteria.originOsmId()))
                .and(SeatSpecifications.destinationOsmIdEquals(criteria.destinationOsmId()))
                .and(SeatSpecifications.countAtMost(criteria.availableSeatsInCar()))
                .and(SeatSpecifications.departsOnOrAfter(departureFrom.date(), departureFrom.time()));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(SeatSpecifications.departsOnOrBefore(
                    criteria.departureDateTo(), LocalTime.MAX));
        }

        Page<Seat> seatPage = seatRepository.findAll(spec, pageable);
        List<Seat> seats = seatPage.getContent();

        List<SeatResponseDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToResponseDto)
                .toList();
        List<SeatResponseDto> enriched = seatResponseEnricher.enrich(seats, dtos);
        return new PageImpl<>(enriched, pageable, seatPage.getTotalElements());
    }

    private Page<SeatResponseDto> searchSeatsNearby(SeatSearchCriteriaDto criteria, Pageable pageable) {
        var departureFrom = TimePredicateHelper.calculateDepartureFrom(
                criteria.departureDate(), criteria.departureTimeFrom());

        double radiusKm = criteria.radiusKm() != null ? criteria.radiusKm() : defaultRadiusKm;
        double radiusMeters = radiusKm * 1000;

        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatSpecifications.originWithinRadius(criteria.originLat(), criteria.originLon(), radiusMeters))
                .and(SeatSpecifications.destinationWithinRadius(criteria.destinationLat(), criteria.destinationLon(), radiusMeters))
                .and(SeatSpecifications.excludeOriginOsmId(criteria.excludeOriginOsmId()))
                .and(SeatSpecifications.excludeDestinationOsmId(criteria.excludeDestinationOsmId()))
                .and(SeatSpecifications.countAtMost(criteria.availableSeatsInCar()))
                .and(SeatSpecifications.departsOnOrAfter(departureFrom.date(), departureFrom.time()))
                .and(SeatSpecifications.orderByCombinedDistance(
                        criteria.originLat(), criteria.originLon(),
                        criteria.destinationLat(), criteria.destinationLon()));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(SeatSpecifications.departsOnOrBefore(
                    criteria.departureDateTo(), LocalTime.MAX));
        }

        Page<Seat> seatPage = seatRepository.findAll(spec, pageable);
        List<Seat> seats = seatPage.getContent();

        LOGGER.info("Proximity seat search: radiusKm={}, found={}",
                radiusKm, seatPage.getTotalElements());

        List<SeatResponseDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToResponseDto)
                .toList();
        List<SeatResponseDto> enriched = seatResponseEnricher.enrich(seats, dtos);
        return new PageImpl<>(enriched, pageable, seatPage.getTotalElements());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (seatRepository.existsById(id)) {
            seatRepository.deleteById(id);
        } else {
            throw new NoSuchSeatException(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponseDto> getSeatsForPassenger(Long passengerId) {
        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        List<Seat> seats = seatRepository.findByPassengerId(passengerId);
        List<SeatResponseDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToResponseDto)
                .toList();
        return seatResponseEnricher.enrich(seats, dtos);
    }
}
