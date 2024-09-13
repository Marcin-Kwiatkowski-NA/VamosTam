package mkpw.blablatwo.controllers;

import mkpw.blablatwo.api.RideApi;
import mkpw.blablatwo.model.Ride;
import mkpw.blablatwo.hateoas.RideRepresentationModelAssembler;
import mkpw.blablatwo.services.ride.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
//@RequestMapping(value = "/rides")
public class RidesController implements RideApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(RidesController.class);
    private final RideService service;
    private final RideRepresentationModelAssembler assembler;

    public RidesController(RideService service, RideRepresentationModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @Override
//    @GetMapping(path = "/rides/{rideId}")
    public ResponseEntity<Ride> getRideById (String rideId) {
        LOGGER.info("Request for rideId: {}",  rideId);
        return ResponseEntity.ok(assembler.toModel(service.getRideById(rideId)));
    }
}
