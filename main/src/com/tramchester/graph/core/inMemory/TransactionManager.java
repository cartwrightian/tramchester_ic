package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.core.TransactionObserver;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@LazySingleton
public class TransactionManager implements TransactionObserver {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    private final AtomicInteger transactionSequenceNumber;
    private final ProvidesNow providesNow;
    private final GraphCore graph;

    // TODO Open Transaction tracking/warning

    @Inject
    public TransactionManager(final ProvidesNow providesNow, final GraphCore graph) {
        this.providesNow = providesNow;
        this.graph = graph;
        transactionSequenceNumber = new AtomicInteger(1);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopped");
    }

    public synchronized MutableGraphTransaction createTransaction(final Duration timeout, boolean immutable) {
        // TODO implement timeout
        final int index = transactionSequenceNumber.getAndIncrement();
        logger.info("create mutable for id " + index);
        return new GraphTransactionInMemory(index, this, graph, immutable);
    }

    public synchronized MutableGraphTransaction createTimedTransaction(Logger logger, String text, boolean immutable) {
        final int index = transactionSequenceNumber.getAndIncrement();
        final Instant createdAt = providesNow.getInstant();
        logger.info("create timed for id " + index);
        return new TimedTransactionInMemory(index, this, graph, logger, text, immutable);
    }

    @Override
    public void onClose(final GraphTransaction graphTransaction) {
        logger.info("close " + graphTransaction.getTransactionId());
    }

    @Override
    public void onCommit(final GraphTransaction graphTransaction) {
        logger.info("commit " + graphTransaction.getTransactionId());
    }


}
