package com.tramchester.graph.facade;

import com.tramchester.config.GraphDBConfig;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    private final AtomicInteger openCount;
    private final Map<Integer,GraphTransaction> openTransactions;
    private final Map<Integer, StackTraceElement[]> diagnostics;
    private final Set<Integer> commited;

    public GraphTransactionFactory(GraphDatabaseService databaseService, GraphDBConfig graphDBConfig) {
        this.databaseService = databaseService;
        this.graphDBConfig = graphDBConfig;
        openTransactions = new HashMap<>();
        openCount = new AtomicInteger(0);
        diagnostics = new HashMap<>();
        commited = new HashSet<>();
    }

    public MutableGraphTransaction beginMutable(final Duration timeout) {
        // graph id factory scoped to transaction level to avoid memory usages issues
        final GraphIdFactory graphIdFactory = new GraphIdFactory(graphDBConfig);
        final Transaction graphDatabaseTxn = databaseService.beginTx(timeout.toSeconds(), TimeUnit.SECONDS);
        final int index = openCount.incrementAndGet();
        MutableGraphTransaction graphTransaction = new MutableGraphTransaction(graphDatabaseTxn, graphIdFactory, index,this);
        openTransactions.put(index, graphTransaction);
        diagnostics.put(index, Thread.currentThread().getStackTrace());
        return graphTransaction;
    }

    public ImmutableGraphTransaction begin(final Duration timeout) {
        final MutableGraphTransaction contained = beginMutable(timeout);
        return new ImmutableGraphTransaction(contained);
    }

    public void close() {
        final int total = openCount.get();
        if (total>0) {
            logger.info("Opened " + openCount.get() + " transactions");
            if (!openTransactions.isEmpty()) {
                logger.warn("close: Still " + openTransactions.size() + " Remaining open transactions: " + openTransactions);
                openTransactions.keySet().forEach(index -> {
                    logger.warn("Transaction " + index + " from " + logStack(diagnostics.get(index)));
                    final GraphTransaction graphTransaction = openTransactions.get(index);
                    logger.info("Closing " + graphTransaction);
                    graphTransaction.close();
                });
            }
        }
    }

    private String logStack(StackTraceElement[] stackTraceElements) {
        return Arrays.stream(stackTraceElements).map(line -> line + System.lineSeparator()).collect(Collectors.joining());
    }

    @Override
    public void onCommit(final GraphTransaction graphTransaction) {
        final int index = graphTransaction.getTransactionId();

        if (openTransactions.containsKey(index)) {
            openTransactions.remove(index);
            diagnostics.remove(index);
            commited.add(index);
        } else {
            logger.error("onCommit: Could not find for index: " + index + " " + graphTransaction);
        }
    }

    @Override
    public void onClose(final GraphTransaction graphTransaction) {
        final int index = graphTransaction.getTransactionId();

        if (commited.contains(index)) {
            return;
        }

        if (openTransactions.containsKey(index)) {
            openTransactions.remove(index);
            diagnostics.remove(index);
        } else {
            logger.error("onClose: Could not find for index: " + index + " " + graphTransaction);
        }

    }

}
