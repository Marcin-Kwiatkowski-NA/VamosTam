package com.blablatwo.user.exception;

public class AvatarKeyMismatchException extends RuntimeException {
    public AvatarKeyMismatchException(String objectKey, Long userId) {
        super("Object key '%s' does not belong to user %d".formatted(objectKey, userId));
    }
}
