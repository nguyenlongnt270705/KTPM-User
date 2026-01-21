package com.example.demo.utils;

import org.apache.logging.log4j.util.Strings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {
    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String DATE_ONLY_FORMAT = "yyyy-MM-dd";

    public static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern(DATE_FORMAT);
    public static final DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT);
    public static final DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern(DATE_ONLY_FORMAT);

    public static String dateToString(Instant instant) {
        if (instant == null) {
            return Strings.EMPTY;
        }
        return sdf.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static Instant stringToDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        // Try parse format ISO 8601 (2025-01-01T00:00:00Z)
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateString, isoFormatter);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e1) {
            // Try parse date only format (2025-01-01)
            try {
                LocalDate localDate = LocalDate.parse(dateString, dateOnlyFormatter);
                return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException e2) {
                // Previous format (01-01-2025 00:00:00)
                try {
                    LocalDateTime ldt = LocalDateTime.parse(dateString, sdf);
                    return ldt.atZone(ZoneId.systemDefault()).toInstant();
                } catch (DateTimeParseException e3) {
                    // Throw exception with clear message
                    throw new DateTimeParseException(
                            "Cannot parse date string: " + dateString +
                                    ". Supported formats: 'yyyy-MM-dd'T'HH:mm:ss'Z'', 'yyyy-MM-dd', or 'dd-MM-yyyy HH:mm:ss'",
                            dateString, 0, e3
                    );
                }
            }
        }
    }

    public static String getCurrentDate() {
        return DateTimeFormatter.ofPattern("ddMMyyyy").format(Instant.now().atZone(ZoneId.systemDefault()));
    }

    public static Instant parseDateOnly(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
