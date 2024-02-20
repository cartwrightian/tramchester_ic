package com.tramchester.domain.collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

public class RequestStopStream<T> implements Running {
    private static final Logger logger = LoggerFactory.getLogger(RequestStopStream.class);

    private Stream<T> theStream;
    private final AtomicBoolean running;
    private final CountDownLatch awaitStream;

    public RequestStopStream() {
        this(new AtomicBoolean(true));
    }

    public RequestStopStream(final AtomicBoolean running) {
        this.running = running;
        awaitStream = new CountDownLatch(1);
    }

    public synchronized RequestStopStream<T> setStream(final Stream<T> stream) {
        if (this.theStream != null) {
            String message = "stream already set";
            logger.error(message);
            throw new RuntimeException(message);
        }
        this.theStream = stream;
        awaitStream.countDown();
        return this;
    }

    public <R> RequestStopStream<R> map(final Function<T, R> mapper) {
        final RequestStopStream<R> result = new RequestStopStream<>(this.running);
        return result.setStream(theStream.map(mapper));
    }

    public synchronized Stream<T> getStream() throws InterruptedException {
        awaitStream.await();
        return theStream;
    }

    public synchronized void stop() {
        logger.warn("Stop was requested");
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

}
