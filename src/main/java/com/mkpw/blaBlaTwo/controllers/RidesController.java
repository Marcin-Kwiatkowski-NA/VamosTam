package com.mkpw.blaBlaTwo.controllers;

import com.mkpw.blaBlaTwo.RidesApi;
import com.mkpw.blaBlaTwo.model.Ride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;


public class RidesController implements RidesApi {
    private static final Logger log = LoggerFactory.getLogger(RidesController.class);

    @Override
    public ResponseEntity<Ride> getRideById (String rideId) {
        log.info("Request for rideId: {}",  rideId);
        return ResponseEntity.ok(new Ride());
    }

    @Override
    public ResponseEntity<Void> createRide(Ride ride) throws Exception {
        return RidesApi.super.createRide(ride);

    }

}
