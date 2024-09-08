package com.mkpw.blaBlaTwo.controllers;

import com.mkpw.blaBlaTwo.RideApi;
import com.mkpw.blaBlaTwo.hateoas.RideRepresentationModelAssembler;
import com.mkpw.blaBlaTwo.model.Ride;
import com.mkpw.blaBlaTwo.services.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RidesController implements RideApi {
    private static final Logger log = LoggerFactory.getLogger(RidesController.class);
    private final RideService service;
    private final RideRepresentationModelAssembler assembler;

    public RidesController(RideService service, RideRepresentationModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @Override
    public ResponseEntity<Ride> getRideById (String rideId) {
        log.info("Request for rideId: {}",  rideId);
        return ResponseEntity.ok(assembler.toModel(service.getRideByRideId(rideId)));
    }

}
