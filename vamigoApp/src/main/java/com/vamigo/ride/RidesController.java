package com.vamigo.ride;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.vamigo.utils.ControllersUtil.getUriFromId;
import static com.vamigo.utils.SortMappingUtil.translateSort;

@RestController
@PreAuthorize("hasRole('USER')")
public class RidesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RidesController.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "departureTime", "pricePerSeat", "totalSeats", "lastModified");

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
    @PreAuthorize("@rideSecurity.isDriver(principal, #id)")
    public ResponseEntity<RideResponseDto> updateRide(@Valid @RequestBody RideCreationDto rideDTO,
                                                      @PathVariable Long id,
                                                      @AuthenticationPrincipal AppPrincipal principal) {
        var updatedRide = rideService.update(rideDTO, id, principal.userId());
        return ResponseEntity.ok()
                .location(getUriFromId(id))
                .body(updatedRide);
    }

    @DeleteMapping("/rides/{id}")
    @PreAuthorize("@rideSecurity.isDriver(principal, #id)")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id,
                                            @AuthenticationPrincipal AppPrincipal principal) {
        rideService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rides/{id}/cancel")
    @PreAuthorize("@rideSecurity.isDriver(principal, #id)")
    public ResponseEntity<Void> cancelRide(@PathVariable Long id,
                                            @AuthenticationPrincipal AppPrincipal principal) {
        rideService.cancelRide(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rides/{id}/complete")
    @PreAuthorize("@rideSecurity.isDriver(principal, #id)")
    public ResponseEntity<RideResponseDto> completeRide(@PathVariable Long id,
                                                         @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(rideService.completeRide(id, principal.userId()));
    }

    @GetMapping("/rides")
    public ResponseEntity<Page<RideResponseDto>> getAllRides(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = resolveSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(rideService.getAllRides(pageable));
    }

    @GetMapping("/rides/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<RideResponseDto>> searchRides(
            @Valid @ModelAttribute RideSearchCriteriaDto criteria,
            @PageableDefault(size = 10, sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(rideService.searchRides(criteria, pageable));
    }

    @GetMapping("/me/rides")
    public ResponseEntity<List<RideResponseDto>> getMyRides(@AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(rideService.getRidesForDriver(principal.userId()));
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        try {
            return translateSort(sortBy, sortDir, ALLOWED_SORT_FIELDS);
        } catch (IllegalArgumentException e) {
            return translateSort("departureTime", sortDir, ALLOWED_SORT_FIELDS);
        }
    }
}
