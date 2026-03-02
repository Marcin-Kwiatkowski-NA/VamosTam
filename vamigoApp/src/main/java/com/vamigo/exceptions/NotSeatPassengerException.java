package com.vamigo.exceptions;

public class NotSeatPassengerException extends RuntimeException {
    public NotSeatPassengerException(Long seatId, Long userId) {
        super("User " + userId + " is not the passenger of seat " + seatId);
    }
}
