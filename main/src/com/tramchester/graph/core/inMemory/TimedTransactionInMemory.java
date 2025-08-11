package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.core.TransactionObserver;
import com.tramchester.metrics.Timing;
import org.neo4j.graphdb.TransientTransactionFailureException;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class TimedTransactionInMemory extends GraphTransactionInMemory {
    private final Logger logger;
    private final String name;
    private final Timing timing;
    private boolean committed;

    public TimedTransactionInMemory(int id, TransactionObserver parent, Instant createdAt, Graph graph, Logger logger, String name) {
        super(id, parent, createdAt, graph);
        this.logger = logger;
        this.name = name;
        timing = new Timing(logger, name);
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
