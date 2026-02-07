package com.blablatwo.seat;

import com.blablatwo.domain.ResponseEnricher;
import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SeatResponseEnricher {

    private final SeatExternalMetaRepository metaRepository;
    private final ResponseEnricher responseEnricher;

    public SeatResponseEnricher(SeatExternalMetaRepository metaRepository,
                                 ResponseEnricher responseEnricher) {
        this.metaRepository = metaRepository;
        this.responseEnricher = responseEnricher;
    }

    public List<SeatResponseDto> enrich(List<Seat> seats, List<SeatResponseDto> dtos) {
        return responseEnricher.enrich(seats, dtos,
                s -> s.getPassenger().getId(),
                this::fetchAllMeta,
                this::assembleDto);
    }

    public SeatResponseDto enrich(Seat seat, SeatResponseDto dto) {
        return responseEnricher.enrich(seat, dto,
                s -> s.getPassenger().getId(),
                this::fetchSingleMeta,
                this::assembleDto);
    }

    private Map<Long, SeatExternalMeta> fetchAllMeta(Set<Long> seatIds) {
        return metaRepository.findAllByIdIn(seatIds)
                .stream()
                .collect(Collectors.toMap(SeatExternalMeta::getSeatId, Function.identity()));
    }

    private Optional<SeatExternalMeta> fetchSingleMeta(Long seatId) {
        return metaRepository.findById(seatId);
    }

    private SeatResponseDto assembleDto(SeatResponseDto dto, UserCardDto userCard,
                                         List<ContactMethodDto> contactMethods) {
        return dto.toBuilder()
                .passenger(userCard)
                .contactMethods(contactMethods)
                .build();
    }
}
