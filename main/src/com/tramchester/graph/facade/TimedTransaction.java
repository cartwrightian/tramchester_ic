package com.tramchester.graph.facade;

import com.tramchester.graph.caches.ImmutableNodeCache;
import com.tramchester.metrics.Timing;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransientTransactionFailureException;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class TimedTransaction extends MutableGraphTransaction implements AutoCloseable {
    private final Logger logger;
    private final String name;
    private final Timing timing;
    private boolean committed;

    TimedTransaction(final Transaction txn, final GraphIdFactory idFactory, final int transactionId, final TransactionObserver transactionObserver,
                     final Logger logger, final String name, ImmutableNodeCache nodeCache) {
        super(txn, idFactory, transactionId, transactionObserver, nodeCache);
        this.logger = logger;
        this.name = name;
        timing = new Timing(logger, "transaction " + name);
        committed = false;
    }

    @Override
    public void close() {
        if (!committed) {
            logger.warn("transaction " + name + " was not committed");
        }
        super.close();
        timing.close();
    }

    @Override
    public void commit() {
        committed = true;
        final Instant start = Instant.now();
        try {
            super.commit();
            final Instant finish = Instant.now();
            logger.info("TIMING: " + name + " COMMIT TOOK: " + Duration.between(start, finish).toMillis() + " ms");
        }
        catch (TransientTransactionFailureException exception) {
            final Instant finish = Instant.now();
            logger.error("TXN FAILED: " + name + " AFTER: " + Duration.between(start, finish).toMillis() + " ms", exception);
            throw new RuntimeException("Transaction " + name + " failed", exception);
        }
    }
}
