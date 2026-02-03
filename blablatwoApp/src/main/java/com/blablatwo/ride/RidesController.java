package com.blablatwo.ride;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.utils.ControllersUtil.getUriFromId;

@RestController
public class RidesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RidesController.class);
    private final RideService rideService;

    @Autowired
    public RidesController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponseDto> getRideById(@PathVariable Long id) {
        Optional<RideResponseDto> rideResp = rideService.getById(id);
        return rideResp.map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchRideException(id));
    }

    @PostMapping("/rides")
    public ResponseEntity<RideResponseDto> createRide(@Valid @RequestBody RideCreationDto ride,
                                                      @AuthenticationPrincipal AppPrincipal principal) {
        var newRide = rideService.createForCurrentUser(ride, principal.userId());
        return ResponseEntity.created(getUriFromId(newRide.id()))
                .body(newRide);
    }

    @PutMapping("/rides/{id}")
    public ResponseEntity<RideResponseDto> updateRide(@Valid @RequestBody RideCreationDto rideDTO,
                                                      @PathVariable Long id) {


        var updatedRide = rideService.update(rideDTO, id);
        return ResponseEntity.ok()
                .location(getUriFromId(id))
                .body(updatedRide);
    }

    @DeleteMapping("/rides/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        rideService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rides")
    public ResponseEntity<Page<RideResponseDto>> getAllRides(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(rideService.getAllRides(pageable));
    }

    @GetMapping("/rides/search")
    public ResponseEntity<Page<RideResponseDto>> searchRides(
            @RequestParam(required = false) Long originPlaceId,
            @RequestParam(required = false) Long destinationPlaceId,
            @RequestParam(required = false, defaultValue = "pl") String lang,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureTimeFrom,
            @RequestParam(required = false, defaultValue = "1") Integer minSeats,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                originPlaceId, destinationPlaceId, lang,
                departureDate, departureDateTo, departureTimeFrom, minSeats
        );
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());

        return ResponseEntity.ok(rideService.searchRides(criteria, pageable));
    }

    @PostMapping("/rides/{rideId}/book")
    public ResponseEntity<RideResponseDto> bookRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal AppPrincipal principal) {
        RideResponseDto bookedRide = rideService.bookRide(rideId, principal.userId());
        return ResponseEntity.ok(bookedRide);
    }

    @DeleteMapping("/rides/{rideId}/book")
    public ResponseEntity<RideResponseDto> cancelBooking(
            @PathVariable Long rideId,
            @AuthenticationPrincipal AppPrincipal principal) {
        RideResponseDto updatedRide = rideService.cancelBooking(rideId, principal.userId());
        return ResponseEntity.ok(updatedRide);
    }

    @GetMapping("/travelers/{travelerId}/rides")
    public ResponseEntity<List<RideResponseDto>> getPassengerRides(@PathVariable Long travelerId) {
        return ResponseEntity.ok(rideService.getRidesForPassenger(travelerId));
    }
}
