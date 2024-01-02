package com.tramchester.graph;

import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.metrics.Timing;
import org.neo4j.graphdb.TransientTransactionFailureException;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class TimedTransaction implements AutoCloseable {
    private final MutableGraphTransaction transaction;
    private final Logger logger;
    private final String name;
    private final Timing timing;
    private boolean committed;

    public TimedTransaction(GraphDatabase graphDatabase, Logger logger, String name) {
        this.transaction = graphDatabase.beginTxMutable();
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
        transaction.close();
        timing.close();
    }

    public MutableGraphTransaction transaction() {
        return transaction;
    }

    public void commit() {
        committed = true;
        Instant start = Instant.now();
        try {
            transaction.commit();
            Instant finish = Instant.now();
            logger.info("TIMING: " + name + " COMMIT TOOK: " + Duration.between(start, finish).toMillis() + " ms");
        }
        catch (TransientTransactionFailureException exception) {
            Instant finish = Instant.now();
            logger.error("TXN FAILED: " + name + " AFTER: " + Duration.between(start, finish).toMillis() + " ms", exception);
            throw new RuntimeException("Transaction " + name + " failed", exception);
        }
    }
}
