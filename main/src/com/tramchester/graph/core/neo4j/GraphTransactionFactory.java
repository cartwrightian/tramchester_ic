package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.caches.SharedRelationshipCache;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.TransactionObserver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/***
 * NOTE: not under normal lifecycle control as is used during DB startup which happens before main graph DB is created
 * Do not call these directly when GraphDatabase object is available
 */
public class GraphTransactionFactory implements TransactionObserver {
    private static final Logger logger = LoggerFactory.getLogger(GraphTransactionFactory.class);

    private final GraphDatabaseService databaseService;
    private final SharedNodeCache nodeCache;
    private final SharedRelationshipCache relationshipCache;
    private final GraphReferenceMapper relationshipTypeFactory;
    private final State state;
    private final AtomicInteger transactionCount;
    private final boolean diagnostics;

    public GraphTransactionFactory(final GraphDatabaseService databaseService, SharedNodeCache nodeCache,
                                   SharedRelationshipCache relationshipCache, GraphReferenceMapper relationshipTypeFactory, final boolean diagnostics) {
        this.databaseService = databaseService;
        this.nodeCache = nodeCache;
        this.relationshipCache = relationshipCache;
        this.relationshipTypeFactory = relationshipTypeFactory;

        transactionCount = new AtomicInteger(0);
        state = new State();

        this.diagnostics = diagnostics;

        // neo4j docs state id's not guaranteed beyond transaction boundaries
        //graphIdFactory = new GraphIdFactory(diagnostics);
    }

    public void close() {
        final int total = transactionCount.get();

        if (logger.isDebugEnabled()) {
            if (total == 1) {
                logger.debug("Only one transaction");
                state.logDiagnostics(logger, Level.DEBUG);
            }
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

        //graphIdFactory.close();
        state.close();
    }

    public MutableGraphTransactionNeo4J beginMutable(final Duration timeout) {
        final Transaction graphDatabaseTxn = databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS);

        final int index = transactionCount.incrementAndGet();
        GraphIdFactory graphIdFactory = new GraphIdFactory(diagnostics);
        final MutableGraphTransactionNeo4J graphTransaction = new MutableGraphTransactionNeo4J(graphDatabaseTxn, graphIdFactory,
                relationshipTypeFactory, index,this, nodeCache, relationshipCache);

        state.put(graphTransaction, Thread.currentThread().getStackTrace());

        return graphTransaction;
    }

    public TimedTransaction beginTimedMutable(Logger logger, String text, Duration timeout) {
        final Transaction graphDatabaseTxn = databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS);

        final int index = transactionCount.incrementAndGet();
        final GraphIdFactory graphIdFactory = new GraphIdFactory(diagnostics);
        final TimedTransaction graphTransaction = new TimedTransaction(graphDatabaseTxn, graphIdFactory,
                index,this, logger, text, nodeCache, relationshipCache, relationshipTypeFactory);

        state.put(graphTransaction, Thread.currentThread().getStackTrace());

        return graphTransaction;
    }

    public ImmutableGraphTransactionNeo4J begin(final Duration timeout) {
        return new ImmutableGraphTransactionNeo4J(beginMutable(timeout));
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
        private final ConcurrentMap<Integer, GraphTransaction> openTransactions;
        private final ConcurrentMap<Integer, StackTraceElement[]> diagnostics;
        private final Set<Integer> commited;
        private final AtomicBoolean closed;

        private State() {
            openTransactions = new ConcurrentHashMap<>();
            diagnostics = new ConcurrentHashMap<>();
            commited = new HashSet<>();
            closed = new AtomicBoolean(false);
        }

        public void close() {
            guardNotClosed();
            diagnostics.clear();
            closed.set(true);
        }

        public synchronized void put(final MutableGraphTransactionNeo4J graphTransaction, final StackTraceElement[] stackTrace) {
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
            }
            else {
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
                logger.warn("Transaction " + index + " from " + stackAsString(diagnostics.get(index)));
                final GraphTransaction graphTransaction = openTransactions.get(index);
                if (graphTransaction==null) {
                    logger.error("Transaction for " + index + " is null");
                } else {
                    logger.info("Closing " + graphTransaction);
                    graphTransaction.close();
                }
            });
        }

        private String stackAsString(final StackTraceElement[] stackTraceElements) {
            return Arrays.stream(stackTraceElements).
                    map(line -> line + System.lineSeparator()).
                    collect(Collectors.joining());
        }

        private void guardNotClosed() {
            if (closed.get()) {
                throw new RuntimeException("Closed");
            }
        }

        public void logDiagnostics(final Logger logger, final Level level) {
            diagnostics.values().forEach(value -> logger.atLevel(level).log(stackAsString(value)));
        }
    }

}
