package com.blablatwo.seat.external;

import com.blablatwo.seat.dto.ExternalSeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
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
@RequestMapping("/seats/external")
public class ExternalSeatController {

    private final ExternalSeatService externalSeatService;

    public ExternalSeatController(ExternalSeatService externalSeatService) {
        this.externalSeatService = externalSeatService;
    }

    @PostMapping
    public ResponseEntity<SeatResponseDto> createExternalSeat(
            @Valid @RequestBody ExternalSeatCreationDto dto) {
        SeatResponseDto created = externalSeatService.createExternalSeat(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/check/{externalId}")
    public ResponseEntity<Boolean> checkExists(@PathVariable String externalId) {
        return ResponseEntity.ok(externalSeatService.existsByExternalId(externalId));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<SeatResponseDto> getByExternalId(@PathVariable String externalId) {
        return externalSeatService.getByExternalId(externalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
