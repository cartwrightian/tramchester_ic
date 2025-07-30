package com.tramchester.graph.core;

public interface TransactionObserver {
    void onClose(GraphTransaction graphTransaction);

    void onCommit(GraphTransaction graphTransaction);
}
