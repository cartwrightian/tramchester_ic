package com.tramchester.metrics;

import java.time.Duration;
import java.time.Instant;

public class Timing implements AutoCloseable {
    private final String name;
    private final Instant start;
    private final org.slf4j.Logger logger;
    private long millis;

    public Timing(final org.slf4j.Logger logger, final String name) {
        this.name = name;
        this.logger = logger;
        this.start = Instant.now();
        this.millis = -1;

        logger.info("Start " + name);
    }

    @Override
    public void close() {
        Instant finish = Instant.now();
        millis = Duration.between(start, finish).toMillis();
        logger.info("TIMING: " + name + " TOOK: " + millis +" ms");
    }

    @Override
    public String toString() {
        return "Timing{" +
                "name='" + name + '\'' +
                ", start=" + start +
                ", millis=" + millis +
                '}';
    }
}
