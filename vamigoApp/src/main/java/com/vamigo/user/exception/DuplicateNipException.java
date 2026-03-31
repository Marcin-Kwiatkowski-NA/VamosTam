package com.vamigo.user.exception;

public class DuplicateNipException extends RuntimeException {

    public DuplicateNipException(String nip) {
        super("Carrier with NIP already exists: " + nip);
    }
}
