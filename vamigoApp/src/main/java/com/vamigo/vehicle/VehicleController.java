package com.vamigo.vehicle;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.vehicle.dto.VehiclePhotoConfirmRequest;
import com.vamigo.vehicle.dto.VehiclePhotoConfirmResponse;
import com.vamigo.vehicle.dto.VehiclePhotoPresignResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehiclePhotoService vehiclePhotoService;

    @GetMapping
    public ResponseEntity<List<VehicleResponseDto>> getMyVehicles(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(vehicleService.getMyVehicles(principal.userId()));
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDto> create(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody VehicleCreationDto dto) {
        VehicleResponseDto created = vehicleService.create(principal.userId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponseDto> update(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleCreationDto dto) {
        return ResponseEntity.ok(vehicleService.update(principal.userId(), vehicleId, dto));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long vehicleId) {
        vehicleService.delete(principal.userId(), vehicleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vehicleId}/photo/presign")
    public ResponseEntity<VehiclePhotoPresignResponse> getPhotoPresignUrl(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long vehicleId,
            @RequestParam String contentType) {
        var response = vehiclePhotoService.generatePresignedUrl(principal.userId(), vehicleId, contentType);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{vehicleId}/photo/confirm")
    public ResponseEntity<VehiclePhotoConfirmResponse> confirmPhoto(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehiclePhotoConfirmRequest request) {
        var response = vehiclePhotoService.confirmPhoto(principal.userId(), vehicleId, request.objectKey());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{vehicleId}/photo")
    public ResponseEntity<Void> removePhoto(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long vehicleId) {
        vehiclePhotoService.removePhoto(principal.userId(), vehicleId);
        return ResponseEntity.noContent().build();
    }
}
