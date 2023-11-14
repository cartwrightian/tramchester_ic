package com.tramchester.graph;

import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.metrics.Timing;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class TimedTransaction implements AutoCloseable {
    private final MutableGraphTransaction transaction;
    private final Logger logger;
    private final String name;
    private final Timing timing;
    private boolean commited;

    public TimedTransaction(GraphDatabase graphDatabase, Logger logger, String name) {
        this.transaction = graphDatabase.beginTx();
        this.logger = logger;
        this.name = name;
        timing = new Timing(logger, "transaction " + name);
        commited = false;
    }

    @Override
    public void close() {
        if (!commited) {
            logger.warn("transaction " + name + " was not committed");
        }
        transaction.close();
        timing.close();
    }

    public MutableGraphTransaction transaction() {
        return transaction;
    }

    public void commit() {
        commited = true;
        Instant start = Instant.now();
        transaction.commit();
        Instant finish = Instant.now();
        logger.info("TIMING: " + name + " COMMIT TOOK: " + Duration.between(start, finish).toMillis() +" ms");
    }
}
