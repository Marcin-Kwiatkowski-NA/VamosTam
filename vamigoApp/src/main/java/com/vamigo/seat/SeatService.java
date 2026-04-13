package com.vamigo.seat;

import com.vamigo.seat.dto.SeatCreationDto;
import com.vamigo.seat.dto.SeatListDto;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.dto.SeatSearchCriteriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SeatService {

    SeatResponseDto createForCurrentUser(SeatCreationDto dto, Long userId);

    Optional<SeatResponseDto> getById(Long id);

    SeatResponseDto update(SeatCreationDto dto, Long id, Long userId);

    Page<SeatListDto> searchSeats(SeatSearchCriteriaDto criteria, Pageable pageable);

    void delete(Long id, Long userId);

    SeatResponseDto cancelSeat(Long id, Long userId);

    List<SeatListDto> getSeatsForPassenger(Long passengerId);
}
