package com.blablatwo.exceptions;

public class NoSuchSeatException extends RuntimeException {
    public NoSuchSeatException(Long seatId) {
        super("No seat found with id: " + seatId);
    }
}
