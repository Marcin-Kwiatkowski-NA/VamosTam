package com.vamigo.user.exception;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String slug) {
        super("Slug already taken: " + slug);
    }
}
