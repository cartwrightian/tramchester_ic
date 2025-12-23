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

    private final AtomicInteger transactionId;
    private final ProvidesNow providesNow;
    private final Graph graph;

    @Inject
    public TransactionManager(final ProvidesNow providesNow, final Graph graph) {
        this.providesNow = providesNow;
        this.graph = graph;
        transactionId = new AtomicInteger(1);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopped");
    }

    public synchronized MutableGraphTransaction createTransaction(final Duration timeout) {
        // TODO implement timeout
        final int index = transactionId.getAndIncrement();
        final Instant createdAt = providesNow.getInstant();
        return new GraphTransactionInMemory(index, this, createdAt, graph);
    }

    public synchronized MutableGraphTransaction createTimedTransaction(Logger logger, String text) {
        final int index = transactionId.getAndIncrement();
        final Instant createdAt = providesNow.getInstant();
        return new TimedTransactionInMemory(index, this, createdAt, graph, logger, text);
    }

    @Override
    public void onClose(final GraphTransaction graphTransaction) {

    }

    @Override
    public void onCommit(final GraphTransaction graphTransaction) {

    }


}
