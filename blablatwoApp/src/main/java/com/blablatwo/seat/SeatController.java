package com.blablatwo.seat;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.exceptions.NoSuchSeatException;
import com.blablatwo.seat.dto.SeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.seat.dto.SeatSearchCriteriaDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.blablatwo.utils.ControllersUtil.getUriFromId;

@RestController
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/seats")
    public ResponseEntity<SeatResponseDto> createSeat(@Valid @RequestBody SeatCreationDto dto,
                                                       @AuthenticationPrincipal AppPrincipal principal) {
        SeatResponseDto created = seatService.createForCurrentUser(dto, principal.userId());
        return ResponseEntity.created(getUriFromId(created.id()))
                .body(created);
    }

    @GetMapping("/seats/{id}")
    public ResponseEntity<SeatResponseDto> getSeatById(@PathVariable Long id) {
        return seatService.getById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchSeatException(id));
    }

    @GetMapping("/seats/search")
    public ResponseEntity<Page<SeatResponseDto>> searchSeats(
            @Valid @ModelAttribute SeatSearchCriteriaDto criteria,
            @PageableDefault(size = 10, sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(seatService.searchSeats(criteria, pageable));
    }

    @PutMapping("/seats/{id}")
    public ResponseEntity<SeatResponseDto> updateSeat(@Valid @RequestBody SeatCreationDto dto,
                                                       @PathVariable Long id,
                                                       @AuthenticationPrincipal AppPrincipal principal) {
        var updated = seatService.update(dto, id, principal.userId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/seats/{id}/cancel")
    public ResponseEntity<SeatResponseDto> cancelSeat(@PathVariable Long id,
                                                       @AuthenticationPrincipal AppPrincipal principal) {
        var cancelled = seatService.cancelSeat(id, principal.userId());
        return ResponseEntity.ok(cancelled);
    }

    @DeleteMapping("/seats/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long id) {
        seatService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/seats")
    public ResponseEntity<List<SeatResponseDto>> getMySeats(@AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(seatService.getSeatsForPassenger(principal.userId()));
    }
}
