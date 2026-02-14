package com.blablatwo.seat.external;

import com.blablatwo.domain.ExternalImportSupport;
import com.blablatwo.domain.Status;
import com.blablatwo.ride.RideSource;
import com.blablatwo.seat.Seat;
import com.blablatwo.seat.SeatExternalMeta;
import com.blablatwo.seat.SeatExternalMetaRepository;
import com.blablatwo.seat.SeatMapper;
import com.blablatwo.seat.SeatRepository;
import com.blablatwo.seat.SeatResponseEnricher;
import com.blablatwo.seat.dto.ExternalSeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ExternalSeatServiceImpl(SeatRepository seatRepository,
                                    SeatExternalMetaRepository metaRepository,
                                    ExternalImportSupport importSupport,
                                    SeatMapper seatMapper,
                                    SeatResponseEnricher seatResponseEnricher) {
        this.seatRepository = seatRepository;
        this.metaRepository = metaRepository;
        this.importSupport = importSupport;
        this.seatMapper = seatMapper;
        this.seatResponseEnricher = seatResponseEnricher;
    }

    @Override
    @Transactional
    public SeatResponseDto createExternalSeat(ExternalSeatCreationDto dto) {
        importSupport.validateNotDuplicate(dto.externalId(), metaRepository::existsByExternalId);

        var locations = importSupport.resolveLocations(dto.originLocationName(), dto.destinationLocationName());
        UserAccount proxy = importSupport.resolveProxyUser();

        Seat seat = Seat.builder()
                .passenger(proxy)
                .origin(locations.origin())
                .destination(locations.destination())
                .departureDate(dto.departureDate())
                .departureTime(dto.departureTime())
                .isApproximate(dto.isApproximate())
                .source(RideSource.FACEBOOK)
                .count(dto.count())
                .priceWillingToPay(dto.priceWillingToPay())
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

        return seatResponseEnricher.enrich(saved, seatMapper.seatEntityToResponseDto(saved));
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
