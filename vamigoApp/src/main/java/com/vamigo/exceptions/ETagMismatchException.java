package com.vamigo.exceptions;

public class ETagMismatchException extends RuntimeException {
    public ETagMismatchException() {
        super("ETag mismatch");
    }
}
