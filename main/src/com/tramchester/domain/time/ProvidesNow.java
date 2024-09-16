package com.tramchester.domain.time;

import com.tramchester.domain.dates.TramDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;

public interface ProvidesNow {
    TramTime getNowHourMins();
    LocalDate getDate();
    LocalDateTime getDateTime();
    Instant getInstant();

    default TramDate getTramDate() {
        return TramDate.of(getDate());
    }

    default ZonedDateTime getZoneDateTimeUTC() {
        return ZonedDateTime.now(UTC);
    }
}
