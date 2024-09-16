package com.tramchester.cloud.data;

import com.tramchester.config.TramchesterConfig;

import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static java.lang.String.format;

public class S3Keys {
    //private static final Logger logger = LoggerFactory.getLogger(S3Keys.class);

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;
    private final TramchesterConfig config;

    @Inject
    public S3Keys(TramchesterConfig config) {
        this.config = config;
    }

    public String create(final LocalDateTime timeStamp) {
        String environment = config.getLiveDataConfig().getS3Prefix();

        // TODO this ought to be UTC, not local
        String date = timeStamp.toLocalDate().format(dateFormatter);
        String time = timeStamp.toLocalTime().format(timeFormatter);

        return format("%s/%s/%s", environment, date, time);
    }

    public String createPrefix(final LocalDate timeStamp) {
        String environment = config.getLiveDataConfig().getS3Prefix();

        // TODO this ought to be UTC, not local
        return format("%s/%s", environment, timeStamp.format(dateFormatter));
    }

    public LocalDateTime parse(final String key) throws S3KeyException {
        final String[] parts = key.split("/");

        if (parts.length!=3) {
            throw new S3KeyException("Cannot parse key: " + key);
        }

        try {
            final LocalDate date = LocalDate.parse(parts[1], dateFormatter);
            final LocalTime time = LocalTime.parse(parts[2], timeFormatter);
            return LocalDateTime.of(date, time);
        }
        catch (DateTimeParseException inner) {
            throw new S3KeyException("Could not parse " + key , inner);
        }
    }

    public static class S3KeyException extends Throwable {
        public S3KeyException(String message) {
            super(message);
        }
        public S3KeyException(String message, Exception exception) {
            super(message, exception);
        }
    }
}
