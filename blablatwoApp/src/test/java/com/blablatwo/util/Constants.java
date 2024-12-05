package com.blablatwo.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public class Constants {
    public static final int ONE = 1;
    public static final Long ID_100 = 100L;
    public static final Long ID_ONE = 1L;
    public static final Long NON_EXISTENT_ID = -2L;

    public static final String CITY_NAME_DESTINATION = "Destination";
    public static final String CITY_NAME_ORIGIN = "Origin";
    public static final String CITY_NAME_KRAKOW = "Kraków";
    public static final String CITY_NAME_WARSZAWA = "Warszawa";
    public static final String CITY_NAME_LODZ = "Łódź";
    public static final String CITY_NAME_POZNAN = "Poznań";

    public static final BigDecimal BIG_DECIMAL = BigDecimal.valueOf(99.99d);
    public static final Instant INSTANT = Instant.parse("2021-01-01T00:00:00Z");
    public static final String ETAG = INSTANT.toString();

    public static final String EMAIL = "example@example.com";
    public static final String TELEPHONE = "+48-123-456-789";
    public static final String CRISTIANO = "Cristiano Ronaldo";

    public static final String VEHICLE_MAKE = "Tesla";
    public static final String VEHICLE_MODEL = "Model S";
    public static final String VEHICLE_LICENSE_PLATE = "KR-1234";

    public static final String USERNAME = "user1";
    public static final String PASSWORD = "pwd";
    public static final int ENABLED = 1;



    public static final String PASSENGER_ONE_NAME = "Passenger One";
    public static final String PASSENGER_ONE_EMAIL = "passenger1@example.com";
    public static final String PASSENGER_ONE_TELEPHONE = "+48-111-222-333";

    public static final String PASSENGER_TWO_NAME = "Passenger Two";
    public static final String PASSENGER_TWO_EMAIL = "passenger2@example.com";
    public static final String PASSENGER_TWO_TELEPHONE = "+48-444-555-666";

    public static final String RIDE_STATUS_ACTIVE = "ACTIVE";

    public static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2025, 9, 12, 10, 30, 0);

    private Constants() {
    }
}
