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

    public static final String RIDE_DESCRIPTION = "A scenic ride through the countryside.";

    public static final BigDecimal BIG_DECIMAL = BigDecimal.valueOf(99.99d);
    public static final Instant INSTANT = Instant.parse("2021-01-01T00:00:00Z");
    public static final String ETAG = INSTANT.toString();

    public static final String EMAIL = "example@example.com";
    public static final String TELEPHONE = "+48-123-456-789";
    public static final String CRISTIANO = "Cristiano Ronaldo";


    public static final String USERNAME = "user1";
    public static final String PASSWORD = "pwd";
    public static final int ENABLED = 1;

    public static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.now().plusDays(30);

    public static final String TRAVELER_USERNAME_JOHN_DOE = "johndoe";
    public static final String TRAVELER_PASSWORD = "password123";
    public static final String TRAVELER_EMAIL_JOHN_DOE = "johndoe@example.com";
    public static final String TRAVELER_PHONE_NUMBER = "+48-123-456-789";
    public static final String TRAVELER_NAME_JOHN_DOE = "John Doe";

    public static final String TRAVELER_USERNAME_JANE_DOE = "janedoe";
    public static final String TRAVELER_PASSWORD_SECURE = "securepass";
    public static final String TRAVELER_EMAIL_JANE_DOE = "janedoe@example.com";
    public static final String TRAVELER_NEW_EMAIL_JANE_DOE = "janesmith@example.com";
    public static final String TRAVELER_NAME_JANE_DOE = "Jane Doe";
    public static final String TRAVELER_NAME_JANE_SMITH = "Jane Smith";

    public static final String TRAVELER_USERNAME_TO_DELETE = "tobedeleted";
    public static final String TRAVELER_EMAIL_TO_DELETE = "delete@example.com";

    public static final String TRAVELER_USERNAME_NON_EXISTENT = "ghost";
    public static final String TRAVELER_PASSWORD_NON_EXISTENT = "nopassword";

    public static final String TRAVELER_USERNAME_USER1 = "user1";
    public static final String TRAVELER_PASSWORD_USER1 = "pass1";
    public static final String TRAVELER_EMAIL_USER1 = "user1@example.com";

    public static final String TRAVELER_USERNAME_USER2 = "user2";
    public static final String TRAVELER_PASSWORD_USER2 = "pass2";
    public static final String TRAVELER_EMAIL_USER2 = "user2@example.com";

    public static final String VEHICLE_MAKE_TESLA = "Tesla";
    public static final String VEHICLE_MAKE_BMW = "BMW";

    public static final String VEHICLE_MODEL_MODEL_S = "Model S";
    public static final String VEHICLE_MODEL_X5 = "X5";

    public static final Integer VEHICLE_PRODUCTION_YEAR_2021 = 2021;
    public static final Integer VEHICLE_PRODUCTION_YEAR_2020 = 2020;

    public static final String VEHICLE_COLOR_RED = "Red";
    public static final String VEHICLE_COLOR_BLACK = "Black";
    public static final String VEHICLE_COLOR_BLUE = "Blue";

    public static final String VEHICLE_LICENSE_PLATE_1 = "123-ABC";
    public static final String VEHICLE_LICENSE_PLATE_2 = "456-DEF";
    public static final String VEHICLE_LICENSE_PLATE_3 = "789-GHI";

    private Constants() {
    }
}
