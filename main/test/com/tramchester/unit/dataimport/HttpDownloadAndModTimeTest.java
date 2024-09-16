package com.tramchester.unit.dataimport;

import com.tramchester.dataimport.HttpDownloadAndModTime;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpDownloadAndModTimeTest {

    // TODO Better testing for HttpDownloadAndModTime

    // repro bug due to lack of locale in formatter
    @Test
    void shouldParseModTimeSeptember() {
        String text = "Mon, 02 Sep 2024 02:45:47 GMT";

        HttpDownloadAndModTime httpDownloadAndModTime = new HttpDownloadAndModTime();

        ZonedDateTime result = httpDownloadAndModTime.parseModTime(text);

        assertEquals(LocalDate.of(2024,9,2), result.toLocalDate());
        assertEquals(LocalTime.of(2,45,47), result.toLocalTime());

    }

    @Test
    void shouldSpikeRespectingTimezoneGMT() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HttpDownloadAndModTime.LAST_MOD_PATTERN, Locale.ENGLISH);
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("Mon, 02 Sep 2024 02:45:47 GMT", formatter);

        assertEquals(ZoneId.of("GMT"), zonedDateTime.getZone());

        ZonedDateTime adjusted = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        LocalDateTime localDateTime = adjusted.toLocalDateTime();

        assertEquals(LocalDate.of(2024,9,2), localDateTime.toLocalDate());
        assertEquals(LocalTime.of(2,45,47), localDateTime.toLocalTime());

    }

}
