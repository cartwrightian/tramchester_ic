package com.tramchester.graph.graphbuild;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class BatchTransactionStrategy implements TransactionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BatchTransactionStrategy.class);

    private final GraphDatabase graphDatabase;
    private final int batchSize;
    private final IdFor<Agency> agencyId;
    private final AtomicInteger tripCount;

    private MutableGraphTransaction transaction;

    public BatchTransactionStrategy(GraphDatabase graphDatabase, int batchSize, IdFor<Agency> agencyId) {
        this.graphDatabase = graphDatabase;
        this.batchSize = batchSize;
        this.agencyId = agencyId;
        tripCount = new AtomicInteger(0);
        transaction = null;
    }

    @Override
    public void routeBegin(Route route) {
        // no-op
    }

    @Override
    public void routeDone() {
        // no-op
    }

    @Override
    public void tripBegin(final Trip trip) {
        if (transaction==null) {
            transaction = graphDatabase.beginTxMutable();
            tripCount.set(0);
        }
    }

    @Override
    public void tripDone() {
        final int current = tripCount.getAndIncrement();
        if (current>batchSize) {
            logger.info("Agency " + agencyId + " commit after " + current + " trips ");
            transaction.commit();
            transaction = null;
        }
    }

    @Override
    public MutableGraphTransaction currentTxn() {
        return transaction;
    }

    public void close() {
        if (transaction!=null) {
            final int current = tripCount.get();
            logger.info("Agency " + agencyId + " commit after " + current + " trips ");
            transaction.commit();
            transaction = null;
        }
    }
}
