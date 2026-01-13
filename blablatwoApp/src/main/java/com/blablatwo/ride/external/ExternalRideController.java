package com.blablatwo.ride.external;

import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rides/external")
public class ExternalRideController {

    private final ExternalRideService externalRideService;

    public ExternalRideController(ExternalRideService externalRideService) {
        this.externalRideService = externalRideService;
    }

    @PostMapping
    public ResponseEntity<RideResponseDto> createExternalRide(
            @Valid @RequestBody ExternalRideCreationDto dto) {
        RideResponseDto created = externalRideService.createExternalRide(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/check/{externalId}")
    public ResponseEntity<Boolean> checkExists(@PathVariable String externalId) {
        return ResponseEntity.ok(externalRideService.existsByExternalId(externalId));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<RideResponseDto> getByExternalId(@PathVariable String externalId) {
        return externalRideService.getByExternalId(externalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
