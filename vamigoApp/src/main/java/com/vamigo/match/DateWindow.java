package com.vamigo.match;

import java.time.Instant;

public record DateWindow(Instant earliest, Instant latest) {

    public static DateWindow of(Instant earliest, Instant latest) {
        return new DateWindow(earliest, latest);
    }

    public static DateWindow openEnded(Instant earliest) {
        return new DateWindow(earliest, null);
    }
}
