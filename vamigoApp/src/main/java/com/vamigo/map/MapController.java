package com.vamigo.map;

import com.vamigo.map.dto.RoutePreviewRequest;
import com.vamigo.map.dto.RoutePreviewResponse;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/map")
@EnableConfigurationProperties(MapProperties.class)
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/trips/ride/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RoutePreviewResponse> getRideRoutePreview(@PathVariable Long id) {
        return ResponseEntity.ok(mapService.getRoutePreviewForRide(id));
    }

    @GetMapping("/trips/seat/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RoutePreviewResponse> getSeatRoutePreview(@PathVariable Long id) {
        return ResponseEntity.ok(mapService.getRoutePreviewForSeat(id));
    }

    @PostMapping("/route-preview")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RoutePreviewResponse> getAdHocRoutePreview(
            @Valid @RequestBody RoutePreviewRequest request) {
        return ResponseEntity.ok(mapService.getRoutePreview(request));
    }
}
