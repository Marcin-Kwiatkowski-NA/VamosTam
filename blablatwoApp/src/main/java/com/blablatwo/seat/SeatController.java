package com.blablatwo.seat;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.exceptions.NoSuchSeatException;
import com.blablatwo.seat.dto.SeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.seat.dto.SeatSearchCriteriaDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static com.blablatwo.utils.ControllersUtil.getUriFromId;
import static com.blablatwo.utils.SortMappingUtil.translateSort;

@RestController
public class SeatController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "departureTime", "count", "priceWillingToPay", "lastModified");

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
            @RequestParam(required = false) Long originPlaceId,
            @RequestParam(required = false) Long destinationPlaceId,
            @RequestParam(required = false, defaultValue = "pl") String lang,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureTimeFrom,
            @RequestParam(required = false) Integer availableSeatsInCar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort;
        try {
            sort = translateSort(sortBy, sortDir, ALLOWED_SORT_FIELDS);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        SeatSearchCriteriaDto criteria = new SeatSearchCriteriaDto(
                originPlaceId, destinationPlaceId, lang,
                departureDate, departureDateTo, departureTimeFrom, availableSeatsInCar
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(seatService.searchSeats(criteria, pageable));
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
