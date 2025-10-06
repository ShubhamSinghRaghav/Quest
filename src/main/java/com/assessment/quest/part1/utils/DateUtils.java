package com.assessment.quest.part1.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class DateUtils {

    public static long parseDateTimeToEpochMillis(String date, String time) {
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("M/d/yyyy h:mm a")
                    .toFormatter(Locale.ENGLISH);

            LocalDateTime localDateTime = LocalDateTime.parse(date + " " + time, formatter);
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("America/New_York"));
            Instant utcInstant = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toInstant();
            return utcInstant.toEpochMilli();

        } catch (Exception e) {
            return Instant.now().toEpochMilli();
        }
    }

}
