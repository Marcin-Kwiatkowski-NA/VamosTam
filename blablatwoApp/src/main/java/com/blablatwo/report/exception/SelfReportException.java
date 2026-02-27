package com.blablatwo.report.exception;

public class SelfReportException extends RuntimeException {

    public SelfReportException() {
        super("Cannot report your own content");
    }
}
