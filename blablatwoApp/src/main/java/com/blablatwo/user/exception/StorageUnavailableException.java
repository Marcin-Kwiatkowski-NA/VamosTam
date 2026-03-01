package com.blablatwo.user.exception;

public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(Throwable cause) {
        super("Storage service is unavailable", cause);
    }
}
