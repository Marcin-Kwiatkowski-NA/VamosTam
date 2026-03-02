package com.vamigo.user.exception;

public class InvalidAvatarContentTypeException extends RuntimeException {
    public InvalidAvatarContentTypeException(String contentType) {
        super("Invalid avatar content type: %s. Allowed: image/jpeg, image/png, image/webp".formatted(contentType));
    }
}
