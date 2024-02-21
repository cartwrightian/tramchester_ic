package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.facade.GraphNodeId;
import org.neo4j.graphdb.traversal.Evaluation;

public interface HeuristicsReason {
    boolean isValid();
    String textForGraph();
    HowIGotHere getHowIGotHere();

    ReasonCode getReasonCode();

    GraphNodeId getNodeId();

    Evaluation getEvaluationAction();
}
