package com.vamigo.user.exception;

public class AvatarNotUploadedException extends RuntimeException {
    public AvatarNotUploadedException(String objectKey) {
        super("Avatar object not found at key: %s. Upload before confirming.".formatted(objectKey));
    }
}
