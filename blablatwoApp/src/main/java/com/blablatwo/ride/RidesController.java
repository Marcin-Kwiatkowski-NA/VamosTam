package com.blablatwo.ride;

import com.blablatwo.exceptions.ETagMismatchException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.DTO.RideResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<RideResponseDto> getRideById(@PathVariable long id){
        Optional<RideResponseDto> rideResp = rideService.getById(id);
        return rideResp.map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchRideException(id));
    }

    @PostMapping("/rides")
    public ResponseEntity<RideResponseDto> createRide(@Valid @RequestBody RideCreationDTO ride) {
        var newRide = rideService.create(ride);
        return ResponseEntity.created(getUriFromId(newRide.id()))
                .eTag(getEtag(newRide))
                .body(newRide);
    }

    @PutMapping("/rides/{id}")
    public ResponseEntity<RideResponseDto> updateRide(@Valid @RequestBody RideCreationDTO rideDTO,
                                               @PathVariable long id,
                                               @RequestHeader ("If-Match") String ifMatch) {

        if(! rideService.ifMatch(id, ifMatch)) {
            throw new ETagMismatchException();
        }

        var updatedRide = rideService.update(rideDTO, id);
        return ResponseEntity.ok()
                .location(getUriFromId(id))
                .eTag(getEtag(updatedRide))
                .body(updatedRide);
    }

    @DeleteMapping("/rides/{id}")
    public ResponseEntity<Void> deleteRide (@PathVariable long id) {
        rideService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static String getEtag(RideResponseDto newRide) {
        return String.valueOf(newRide.lastModified());
    }
}
