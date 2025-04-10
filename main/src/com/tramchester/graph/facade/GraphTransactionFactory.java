package com.tramchester.graph.facade;

import com.tramchester.config.GraphDBConfig;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/***
 * NOTE: not under normal lifecycle control as is used during DB startup which happens before main graph DB is created
 * Do not call these directly when GraphDatabase object is available
 */
public class GraphTransactionFactory implements MutableGraphTransaction.TransactionObserver {
    private static final Logger logger = LoggerFactory.getLogger(GraphTransactionFactory.class);

    private final GraphDatabaseService databaseService;
    private final GraphDBConfig graphDBConfig;

    private final State state;
    private final AtomicInteger transactionCount;

    public GraphTransactionFactory(GraphDatabaseService databaseService, GraphDBConfig graphDBConfig) {
        this.databaseService = databaseService;
        this.graphDBConfig = graphDBConfig;

        transactionCount = new AtomicInteger(0);
        state = new State();
    }

    public MutableGraphTransaction beginMutable(final Duration timeout) {
        // graph id factory scoped to transaction level to avoid memory usages issues
        final GraphIdFactory graphIdFactory = new GraphIdFactory(graphDBConfig);
        final Transaction graphDatabaseTxn = databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS);

        final int index = transactionCount.incrementAndGet();
        final MutableGraphTransaction graphTransaction = new MutableGraphTransaction(graphDatabaseTxn, graphIdFactory,
                index,this);

        state.put(graphTransaction, Thread.currentThread().getStackTrace());

        return graphTransaction;
    }

    public ImmutableGraphTransaction begin(final Duration timeout) {
        return new ImmutableGraphTransaction(beginMutable(timeout));
    }

    public void close() {
        final int total = transactionCount.get();

        // TODO into debug
        if (total==1) {
            logger.info("Only one transaction for " + state.diagnostics);
        }
        if (total>0) {
            logger.info("Opened " + total + " transactions");
            if (state.hasOutstanding()) {
                logger.warn("close: Still " + state.outstanding() + " remaining open transactions");
                state.logOutstanding(logger);
            } else {
                logger.info("closed: open and commit/close balanced");
            }
        }
        state.close();
    }

    @Override
    public void onCommit(final GraphTransaction graphTransaction) {
        state.commitTransaction(graphTransaction);
    }

    @Override
    public void onClose(final GraphTransaction graphTransaction) {
        state.closeTransaction(graphTransaction);
    }

    private static class State {
        private final Map<Integer,GraphTransaction> openTransactions;
        private final Map<Integer, StackTraceElement[]> diagnostics;
        private final Set<Integer> commited;
        private AtomicBoolean closed;

        private State() {
            openTransactions = new HashMap<>();
            diagnostics = new HashMap<>();
            commited = new HashSet<>();
            closed = new AtomicBoolean(false);
        }

        public synchronized void put(final MutableGraphTransaction graphTransaction, final StackTraceElement[] stackTrace) {
            guardNotClosed();
            final int index = graphTransaction.getTransactionId();
            openTransactions.put(index, graphTransaction);
            diagnostics.put(index, stackTrace);
        }

        public synchronized void closeTransaction(final GraphTransaction graphTransaction) {
            guardNotClosed();
            final int index = graphTransaction.getTransactionId();
            if (commited.contains(index)) {
                return;
            }

            if (openTransactions.remove(index) == null) {
                logger.error("onClose: Could not find for index: " + index);
            } else {
                diagnostics.remove(index);
            }
        }

        public synchronized void commitTransaction(final GraphTransaction graphTransaction) {
            guardNotClosed();
            final int index = graphTransaction.getTransactionId();

            if (openTransactions.remove(index)==null) {
                logger.error("onCommit: Could not find for index: " + index);
            } else {
                diagnostics.remove(index);
                commited.add(index);
            }
        }

        public boolean hasOutstanding() {
            guardNotClosed();
            return !openTransactions.isEmpty();
        }

        public int outstanding() {
            guardNotClosed();
            return openTransactions.size();
        }

        public void logOutstanding(final Logger logger) {
            guardNotClosed();
            openTransactions.keySet().forEach(index -> {
                logger.warn("Transaction " + index + " from " + logStack(diagnostics.get(index)));
                final GraphTransaction graphTransaction = openTransactions.get(index);
                logger.info("Closing " + graphTransaction);
                graphTransaction.close();
            });
        }

        private String logStack(StackTraceElement[] stackTraceElements) {
            return Arrays.stream(stackTraceElements).map(line -> line + System.lineSeparator()).collect(Collectors.joining());
        }

        public void close() {
            guardNotClosed();
            closed.set(true);
        }

        private void guardNotClosed() {
            if (closed.get()) {
                throw new RuntimeException("Closed");
            }
        }
    }

}
