package com.blablatwo.messaging.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ride/seat context embedded in conversation responses.
 * Null when the linked offer has been deleted or the topicKey is malformed.
 */
public record RideContextDto(
    OfferKind offerKind,
    long offerId,
    String originName,
    String destinationName,
    LocalDate departureDate,
    LocalTime departureTime,
    OfferStatus offerStatus,
    ViewerRole viewerRole,
    boolean isCreator
) {}
