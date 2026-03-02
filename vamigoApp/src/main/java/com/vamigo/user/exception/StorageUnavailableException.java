package com.vamigo.user.exception;

public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(Throwable cause) {
        super("Storage service is unavailable", cause);
    }
}
