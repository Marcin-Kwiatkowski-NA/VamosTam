package com.vamigo.seat.external;

import com.vamigo.domain.ExternalImportSupport;
import com.vamigo.domain.Status;
import com.vamigo.ride.RideSource;
import com.vamigo.seat.Seat;
import com.vamigo.seat.SeatExternalMeta;
import com.vamigo.seat.SeatExternalMetaRepository;
import com.vamigo.seat.SeatMapper;
import com.vamigo.seat.SeatRepository;
import com.vamigo.seat.SeatResponseEnricher;
import com.vamigo.seat.dto.ExternalSeatCreationDto;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.event.ExternalSeatCreatedEvent;
import com.vamigo.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ExternalSeatServiceImpl implements ExternalSeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalSeatServiceImpl.class);

    private final SeatRepository seatRepository;
    private final SeatExternalMetaRepository metaRepository;
    private final ExternalImportSupport importSupport;
    private final SeatMapper seatMapper;
    private final SeatResponseEnricher seatResponseEnricher;
    private final ApplicationEventPublisher eventPublisher;

    public ExternalSeatServiceImpl(SeatRepository seatRepository,
                                    SeatExternalMetaRepository metaRepository,
                                    ExternalImportSupport importSupport,
                                    SeatMapper seatMapper,
                                    SeatResponseEnricher seatResponseEnricher,
                                    ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.metaRepository = metaRepository;
        this.importSupport = importSupport;
        this.seatMapper = seatMapper;
        this.seatResponseEnricher = seatResponseEnricher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SeatResponseDto createExternalSeat(ExternalSeatCreationDto dto) {
        importSupport.validateNotDuplicate(dto.externalId(), metaRepository::existsByExternalId);
        importSupport.validateDepartureInFuture(dto.departureTime());

        var locations = importSupport.resolveLocations(dto.originLocationName(), dto.destinationLocationName());
        UserAccount proxy = importSupport.resolveProxyUser();

        Seat seat = Seat.builder()
                .passenger(proxy)
                .origin(locations.origin())
                .destination(locations.destination())
                .departureTime(dto.departureTime())
                .timePrecision(dto.timePrecision())
                .source(RideSource.FACEBOOK)
                .count(dto.count())
                .priceWillingToPay(dto.priceWillingToPay())
                .currency(dto.currency())
                .status(Status.ACTIVE)
                .description(dto.description())
                .lastModified(Instant.now())
                .build();

        Seat saved = seatRepository.save(seat);

        SeatExternalMeta meta = SeatExternalMeta.builder()
                .seat(saved)
                .sourceUrl(dto.sourceUrl())
                .externalId(dto.externalId())
                .rawContent(dto.rawContent())
                .phoneNumber(dto.phoneNumber())
                .authorName(dto.authorName())
                .build();

        metaRepository.save(meta);

        LOGGER.info("Created external seat with ID: {} from external source: {}",
                saved.getId(), dto.externalId());

        SeatResponseDto response = seatResponseEnricher.enrich(saved, seatMapper.seatEntityToResponseDto(saved));
        eventPublisher.publishEvent(new ExternalSeatCreatedEvent(response, dto.sourceUrl()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByExternalId(String externalId) {
        return metaRepository.existsByExternalId(externalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeatResponseDto> getByExternalId(String externalId) {
        return metaRepository.findByExternalId(externalId)
                .map(meta -> {
                    Seat seat = meta.getSeat();
                    return seatResponseEnricher.enrich(seat, seatMapper.seatEntityToResponseDto(seat));
                });
    }
}
