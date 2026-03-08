package com.vamigo.seat;

import com.vamigo.domain.Status;
import com.vamigo.exceptions.NoSuchSeatException;
import com.vamigo.exceptions.NotSeatPassengerException;
import com.vamigo.location.Location;
import com.vamigo.location.LocationResolutionService;
import com.vamigo.search.GeoUtils;
import com.vamigo.search.SearchProperties;
import com.vamigo.seat.dto.SeatCreationDto;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.dto.SeatSearchCriteriaDto;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.capability.CapabilityService;
import com.vamigo.user.exception.NoSuchUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@EnableConfigurationProperties(SearchProperties.class)
public class SeatServiceImpl implements SeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatServiceImpl.class);

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;
    private final LocationResolutionService locationResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final SeatResponseEnricher seatResponseEnricher;
    private final CapabilityService capabilityService;
    private final SearchProperties searchProperties;

    public SeatServiceImpl(SeatRepository seatRepository,
                            SeatMapper seatMapper,
                            LocationResolutionService locationResolutionService,
                            UserAccountRepository userAccountRepository,
                            SeatResponseEnricher seatResponseEnricher,
                            CapabilityService capabilityService,
                            SearchProperties searchProperties) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.seatResponseEnricher = seatResponseEnricher;
        this.capabilityService = capabilityService;
        this.searchProperties = searchProperties;
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
        seat.setDepartureTime(dto.departureTime());
        seat.setTimeApproximate(dto.isTimeApproximate());
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
        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatSpecifications.originOsmIdEquals(criteria.originOsmId()))
                .and(SeatSpecifications.destinationOsmIdEquals(criteria.destinationOsmId()))
                .and(SeatSpecifications.countAtMost(criteria.availableSeatsInCar()))
                .and(SeatSpecifications.departsOnOrAfter(criteria.earliestDeparture()));

        if (criteria.latestDeparture() != null) {
            spec = spec.and(SeatSpecifications.departsBefore(criteria.latestDeparture()));
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
        double radiusKm = resolveRadiusKm(criteria);
        double radiusMeters = radiusKm * 1000;

        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatSpecifications.originWithinRadius(criteria.originLat(), criteria.originLon(), radiusMeters))
                .and(SeatSpecifications.destinationWithinRadius(criteria.destinationLat(), criteria.destinationLon(), radiusMeters))
                .and(SeatSpecifications.countAtMost(criteria.availableSeatsInCar()))
                .and(SeatSpecifications.departsOnOrAfter(criteria.earliestDeparture()))
                .and(SeatSpecifications.orderByCombinedDistance(
                        criteria.originLat(), criteria.originLon(),
                        criteria.destinationLat(), criteria.destinationLon()));

        if (criteria.latestDeparture() != null) {
            spec = spec.and(SeatSpecifications.departsBefore(criteria.latestDeparture()));
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
    public SeatResponseDto update(SeatCreationDto dto, Long id, Long userId) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new NoSuchSeatException(id));

        if (!seat.getPassenger().getId().equals(userId)) {
            throw new NotSeatPassengerException(id, userId);
        }

        Location origin = locationResolutionService.resolve(dto.origin());
        Location destination = locationResolutionService.resolve(dto.destination());

        seat.setOrigin(origin);
        seat.setDestination(destination);
        seat.setDepartureTime(dto.departureTime());
        seat.setTimeApproximate(dto.isTimeApproximate());
        seat.setCount(dto.count());
        seat.setPriceWillingToPay(dto.priceWillingToPay());
        seat.setDescription(dto.description());
        seat.setLastModified(Instant.now());

        return seatResponseEnricher.enrich(seat, seatMapper.seatEntityToResponseDto(seat));
    }

    @Override
    @Transactional
    public SeatResponseDto cancelSeat(Long id, Long userId) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new NoSuchSeatException(id));

        if (!seat.getPassenger().getId().equals(userId)) {
            throw new NotSeatPassengerException(id, userId);
        }

        // Idempotent: if already cancelled, return current state
        if (seat.getStatus() == Status.CANCELLED) {
            return seatResponseEnricher.enrich(seat, seatMapper.seatEntityToResponseDto(seat));
        }

        seat.setStatus(Status.CANCELLED);
        seat.setLastModified(Instant.now());

        return seatResponseEnricher.enrich(seat, seatMapper.seatEntityToResponseDto(seat));
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new NoSuchSeatException(id));

        if (!seat.getPassenger().getId().equals(userId)) {
            throw new NotSeatPassengerException(id, userId);
        }

        seatRepository.delete(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponseDto> getSeatsForPassenger(Long passengerId) {
        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        List<Seat> seats = seatRepository.findByPassengerIdOrderByDepartureTimeAsc(passengerId);
        List<SeatResponseDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToResponseDto)
                .toList();
        return seatResponseEnricher.enrich(seats, dtos);
    }

    private double resolveRadiusKm(SeatSearchCriteriaDto criteria) {
        if (criteria.radiusKm() != null) {
            return criteria.radiusKm();
        }
        double distance = GeoUtils.haversineKm(
                criteria.originLat(), criteria.originLon(),
                criteria.destinationLat(), criteria.destinationLon());
        return Math.max(
                distance / searchProperties.proximity().radiusDivisor(),
                searchProperties.proximity().minRadiusKm());
    }
}
