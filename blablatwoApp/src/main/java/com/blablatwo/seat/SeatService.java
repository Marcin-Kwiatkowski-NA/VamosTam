package com.blablatwo.seat;

import com.blablatwo.seat.dto.SeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.seat.dto.SeatSearchCriteriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SeatService {

    SeatResponseDto createForCurrentUser(SeatCreationDto dto, Long userId);

    Optional<SeatResponseDto> getById(Long id);

    SeatResponseDto update(SeatCreationDto dto, Long id, Long userId);

    Page<SeatResponseDto> searchSeats(SeatSearchCriteriaDto criteria, Pageable pageable);

    void delete(Long id);

    SeatResponseDto cancelSeat(Long id, Long userId);

    List<SeatResponseDto> getSeatsForPassenger(Long passengerId);
}
