package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.facade.GraphNodeId;

public interface HeuristicsReason {
    boolean isValid();
    String textForGraph();
    HowIGotHere getHowIGotHere();

    ReasonCode getReasonCode();

    GraphNodeId getNodeId();

}
