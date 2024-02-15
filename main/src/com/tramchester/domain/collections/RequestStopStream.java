package com.tramchester.domain.collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

public class RequestStopStream<T> implements Running {
    private static final Logger logger = LoggerFactory.getLogger(RequestStopStream.class);

    private Stream<T> theStream;
    private final AtomicBoolean running;

    public RequestStopStream() {
        running = new AtomicBoolean(true);
    }

    public RequestStopStream(final AtomicBoolean running) {
        this.running = running;
    }

    public RequestStopStream<T> setStream(final Stream<T> stream) {
        if (this.theStream != null) {
            String message = "stream already set";
            logger.error(message);
            throw new RuntimeException(message);
        }
        this.theStream = stream;
        return this;
    }

    public <R> RequestStopStream<R> map(final Function<T, R> mapper) {
        final RequestStopStream<R> result = new RequestStopStream<>(this.running);
        return result.setStream(theStream.map(mapper));
    }

    public Stream<T> getStream() {
        return theStream;
    }

    public synchronized void stop() {
        logger.warn("Stop was requested");
        running.set(false);
    }

    public synchronized boolean isRunning() {
        return running.get();
    }

}
