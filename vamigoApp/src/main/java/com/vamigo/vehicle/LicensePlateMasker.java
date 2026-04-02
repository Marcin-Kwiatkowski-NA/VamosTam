package com.vamigo.vehicle;

public final class LicensePlateMasker {

    private LicensePlateMasker() {}

    public static String mask(String plate) {
        if (plate == null || plate.length() <= 5) {
            return plate;
        }
        int visibleStart = 3;
        int visibleEnd = 2;
        int masked = plate.length() - visibleStart - visibleEnd;
        return plate.substring(0, visibleStart) + "*".repeat(masked) + plate.substring(plate.length() - visibleEnd);
    }
}
