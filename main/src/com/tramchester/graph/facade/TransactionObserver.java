package com.tramchester.graph.facade;

public interface TransactionObserver {
    void onClose(GraphTransaction graphTransaction);

    void onCommit(GraphTransaction graphTransaction);
}
