package com.blablatwo.seat;

import com.blablatwo.city.City;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityResolutionService;
import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import com.blablatwo.exceptions.NoSuchSeatException;
import com.blablatwo.seat.dto.SeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.seat.dto.SeatSearchCriteriaDto;
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
import java.util.List;
import java.util.Optional;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;
    private final CityMapper cityMapper;
    private final CityResolutionService cityResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final SeatResponseEnricher seatResponseEnricher;
    private final CapabilityService capabilityService;

    public SeatServiceImpl(SeatRepository seatRepository,
                            SeatMapper seatMapper,
                            CityMapper cityMapper,
                            CityResolutionService cityResolutionService,
                            UserAccountRepository userAccountRepository,
                            SeatResponseEnricher seatResponseEnricher,
                            CapabilityService capabilityService) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
        this.cityMapper = cityMapper;
        this.cityResolutionService = cityResolutionService;
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
        City origin = cityResolutionService.resolveCityByPlaceId(dto.originPlaceId(), "pl");
        City destination = cityResolutionService.resolveCityByPlaceId(dto.destinationPlaceId(), "pl");

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
        var departureFrom = TimePredicateHelper.calculateDepartureFrom(
                criteria.departureDate(), criteria.departureTimeFrom());

        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatSpecifications.originPlaceIdEquals(criteria.originPlaceId()))
                .and(SeatSpecifications.destinationPlaceIdEquals(criteria.destinationPlaceId()))
                .and(SeatSpecifications.countAtMost(criteria.availableSeatsInCar()))
                .and(SeatSpecifications.departsOnOrAfter(departureFrom.date(), departureFrom.time()));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(SeatSpecifications.departsOnOrBefore(
                    criteria.departureDateTo(), LocalTime.MAX));
        }

        Page<Seat> seatPage = seatRepository.findAll(spec, pageable);
        List<Seat> seats = seatPage.getContent();

        String lang = criteria.lang() != null ? criteria.lang() : "pl";
        List<SeatResponseDto> dtos = seats.stream()
                .map(seat -> mapSeatToResponseDto(seat, lang))
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

    private SeatResponseDto mapSeatToResponseDto(Seat seat, String lang) {
        SeatResponseDto dto = seatMapper.seatEntityToResponseDto(seat);
        return dto.toBuilder()
                .origin(cityMapper.cityEntityToCityDto(seat.getOrigin(), lang))
                .destination(cityMapper.cityEntityToCityDto(seat.getDestination(), lang))
                .build();
    }
}
