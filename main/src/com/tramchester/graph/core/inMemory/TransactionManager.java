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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@LazySingleton
public class TransactionManager implements TransactionObserver {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    private final AtomicInteger transactionSequenceNumber;
    private final ProvidesNow providesNow;
    private final GraphCore graphCore;
    private final GraphIdFactory idFactory;
    private final Set<Integer> openTransactions;
    private final Set<Integer> committedTransactions;

    @Inject
    public TransactionManager(final ProvidesNow providesNow, final GraphCore graphCore, final GraphIdFactory idFactory) {
        this.providesNow = providesNow;
        this.graphCore = graphCore;
        this.idFactory = idFactory;
        openTransactions = new HashSet<>();
        committedTransactions = new HashSet<>();
        transactionSequenceNumber = new AtomicInteger(1);
    }

    @PreDestroy
    public void stop() {
        if (!openTransactions.isEmpty()) {
            openTransactions.stream().filter(id -> !committedTransactions.contains(id)).
                    forEach(id -> logger.error("Not closed or commited " + id));
        }
        logger.info("Stopped");
    }

    public synchronized MutableGraphTransaction createTransaction(final Duration timeout, boolean immutable) {
        final int index = transactionSequenceNumber.getAndIncrement();
        openTransactions.add(index);
        final Graph graph = wrapGraph(immutable);

        logger.info("create mutable for id " + index);
        return new GraphTransactionInMemory(index, this, graph, immutable);
    }

    public synchronized MutableGraphTransaction createTimedTransaction(Logger logger, String text, boolean immutable) {
        final int index = transactionSequenceNumber.getAndIncrement();
        openTransactions.add(index);
        final Graph graph = wrapGraph(immutable);

        logger.info("create timed for id " + index);
        return new TimedTransactionInMemory(index, this, graph, logger, text, immutable);
    }

    private Graph wrapGraph(boolean immutable) {
        if (immutable) {
            return new ImmutableGraph(graphCore);
        } else {
            //return graphCore;
            return new MutableTransactionGraph(graphCore, idFactory);
        }
    }

    @Override
    public synchronized void onClose(final GraphTransaction graphTransaction) {
        logger.info("close " + graphTransaction);
        openTransactions.remove(graphTransaction.getTransactionId());
    }

    @Override
    public synchronized void onCommit(final GraphTransaction graphTransaction) {
        logger.info("commit " + graphTransaction);

        if (!openTransactions.contains(graphTransaction.getTransactionId())) {
            throw new RuntimeException("Not open " + graphTransaction);
        }
        committedTransactions.add(graphTransaction.getTransactionId());
    }


}
