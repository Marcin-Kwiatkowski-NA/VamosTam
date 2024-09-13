package mkpw.blablatwo.utils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ConstantsTest {
    private ConstantsTest() {
    }
    public static final String CITY_ID = "11111111-1111-1111-1111-111111111111";
    public static final String CITY_ID_2 = "11111111-1111-1111-1111-111111111111";
    public static final String RIDE_ID = "33333333-3333-3333-3333-333333333331";
    public static final String RIDE_ID_2 = "33333333-3333-3333-3333-333333333332";
    public static final String USER_ID = "22222222-2222-2222-2222-222222222221";
    public static final String USER_ID_2 = "22222222-2222-2222-2222-222222222222";
    public static final String CITY_STOP_ID = "44444444-4444-4444-4444-444444444441";
    public static final String CITY_STOP_ID_2 = "44444444-4444-4444-4444-444444444442";

    public static final BigDecimal BIG_DECIMAL = BigDecimal.valueOf(99.99d);
    public static final Timestamp TIMESTAMP = Timestamp.valueOf("2024-09-12 12:34:56");

    public static final String EMAIL = "example@example.com";
    public static final String TELEPHONE = "+48-123-456-789";
    public static final String CRISTIANO = "Cristiano Ronaldo";

    public static final OffsetDateTime DATE_TIME = OffsetDateTime.of(2024, 9, 12, 14, 30, 0, 0, ZoneOffset.of("+02:00"));


}
