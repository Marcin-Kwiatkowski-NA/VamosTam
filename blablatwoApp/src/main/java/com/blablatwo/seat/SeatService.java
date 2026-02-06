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

    Page<SeatResponseDto> searchSeats(SeatSearchCriteriaDto criteria, Pageable pageable);

    void delete(Long id);

    List<SeatResponseDto> getSeatsForPassenger(Long passengerId);
}
