package com.vamigo.seat;

import com.vamigo.domain.Status;
import com.vamigo.exceptions.CannotCreateSeatException;
import com.vamigo.exceptions.NoSuchSeatException;
import com.vamigo.exceptions.NotSeatPassengerException;
import com.vamigo.location.Location;
import com.vamigo.location.LocationResolutionService;
import com.vamigo.match.DateWindow;
import com.vamigo.match.GeoPoint;
import com.vamigo.match.LocationMatchingService;
import com.vamigo.match.MatchFilters;
import com.vamigo.match.MatchProperties;
import com.vamigo.match.RadiusStrategy;
import com.vamigo.match.RouteQuery;
import com.vamigo.match.SeatMatch;
import com.vamigo.match.SeatMatchQuery;
import com.vamigo.seat.dto.SeatCreationDto;
import com.vamigo.seat.dto.SeatListDto;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.dto.SeatSearchCriteriaDto;
import com.vamigo.seat.event.SeatCreatedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.capability.CapabilityService;
import com.vamigo.user.exception.NoSuchUserException;
import com.vamigo.utils.PageableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
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
    private final LocationMatchingService locationMatchingService;
    private final MatchProperties matchProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public SeatServiceImpl(SeatRepository seatRepository,
                            SeatMapper seatMapper,
                            LocationResolutionService locationResolutionService,
                            UserAccountRepository userAccountRepository,
                            SeatResponseEnricher seatResponseEnricher,
                            CapabilityService capabilityService,
                            LocationMatchingService locationMatchingService,
                            MatchProperties matchProperties,
                            ApplicationEventPublisher eventPublisher,
                            Clock clock) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.seatResponseEnricher = seatResponseEnricher;
        this.capabilityService = capabilityService;
        this.locationMatchingService = locationMatchingService;
        this.matchProperties = matchProperties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SeatResponseDto createForCurrentUser(SeatCreationDto dto, Long userId) {
        if (!capabilityService.canCreateSeat(userId)) {
            throw new CannotCreateSeatException(userId);
        }

        UserAccount passenger = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));
        Location origin = locationResolutionService.resolve(dto.origin());
        Location destination = locationResolutionService.resolve(dto.destination());

        Seat seat = Seat.builder().build();
        seat.assignPassenger(passenger);
        seat.updateDetails(seatMapper.toDetails(dto, origin, destination));

        Seat saved = seatRepository.save(seat);
        SeatResponseDto response = seatResponseEnricher.enrich(saved, seatMapper.seatEntityToResponseDto(saved));
        eventPublisher.publishEvent(new SeatCreatedEvent(saved.getId(), userId, response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeatResponseDto> getById(Long id) {
        return seatRepository.findById(id)
                .map(seat -> seatResponseEnricher.enrich(seat, seatMapper.seatEntityToResponseDto(seat)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatListDto> searchSeats(SeatSearchCriteriaDto criteria, Pageable pageable) {
        if (criteria.isProximityMode()) {
            return searchSeatsNearby(criteria, pageable);
        }
        return searchSeatsExact(criteria, pageable);
    }

    private Page<SeatListDto> searchSeatsExact(SeatSearchCriteriaDto criteria, Pageable pageable) {
        Pageable stablePageable = PageableUtils.withStableSort(pageable);
        Instant effectiveEarliest = clampToNow(criteria.earliestDeparture(), Instant.now(clock));

        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatSpecifications.originOsmIdEquals(criteria.originOsmId()))
                .and(SeatSpecifications.destinationOsmIdEquals(criteria.destinationOsmId()))
                .and(SeatSpecifications.countAtMost(criteria.availableSeatsInCar()))
                .and(SeatSpecifications.departsOnOrAfter(effectiveEarliest));

        if (criteria.latestDeparture() != null) {
            spec = spec.and(SeatSpecifications.departsBefore(criteria.latestDeparture()));
        }

        Page<Seat> seatPage = seatRepository.findAll(spec, stablePageable);
        List<Seat> seats = seatPage.getContent();

        List<SeatListDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToSeatListDto)
                .toList();
        List<SeatListDto> enriched = seatResponseEnricher.enrichList(seats, dtos);
        return new PageImpl<>(enriched, stablePageable, seatPage.getTotalElements());
    }

    private Page<SeatListDto> searchSeatsNearby(SeatSearchCriteriaDto criteria, Pageable pageable) {
        Instant effectiveEarliest = clampToNow(criteria.earliestDeparture(), Instant.now(clock));
        RadiusStrategy radius = criteria.radiusKm() != null
                ? RadiusStrategy.fixedKm(criteria.radiusKm())
                : matchProperties.seatSmartRadius();

        var query = new SeatMatchQuery(
                new RouteQuery(
                        new GeoPoint(criteria.originLat(), criteria.originLon()),
                        new GeoPoint(criteria.destinationLat(), criteria.destinationLon()),
                        criteria.originOsmId(), criteria.destinationOsmId()),
                radius,
                DateWindow.of(effectiveEarliest, criteria.latestDeparture()),
                new MatchFilters(null, null, criteria.availableSeatsInCar())
        );

        Page<SeatMatch> matchPage = locationMatchingService.findSeats(query, pageable);
        List<Seat> seats = matchPage.getContent().stream().map(SeatMatch::seat).toList();

        LOGGER.info("Proximity seat search: found={}", matchPage.getTotalElements());

        List<SeatListDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToSeatListDto)
                .toList();
        List<SeatListDto> enriched = seatResponseEnricher.enrichList(seats, dtos);
        return new PageImpl<>(enriched, pageable, matchPage.getTotalElements());
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

        seat.updateDetails(seatMapper.toDetails(dto, origin, destination));

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

        seat.cancel();

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
    public List<SeatListDto> getSeatsForPassenger(Long passengerId) {
        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        List<Seat> seats = seatRepository.findByPassengerIdOrderByDepartureTimeAscIdAsc(passengerId);
        List<SeatListDto> dtos = seats.stream()
                .map(seatMapper::seatEntityToSeatListDto)
                .toList();
        return seatResponseEnricher.enrichList(seats, dtos);
    }

    private static Instant clampToNow(Instant earliest, Instant now) {
        return earliest == null || earliest.isBefore(now) ? now : earliest;
    }
}
