package com.blablatwo.report.exception;

public class AlreadyReportedException extends RuntimeException {

    public AlreadyReportedException(String targetType, Long targetId) {
        super("Already reported %s with id %d".formatted(targetType, targetId));
    }
}
