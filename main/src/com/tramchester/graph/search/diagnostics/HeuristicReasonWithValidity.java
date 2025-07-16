package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.facade.neo4j.GraphNodeId;
import org.neo4j.graphdb.traversal.Evaluation;

import java.util.Objects;

public class HeuristicReasonWithValidity implements HeuristicsReason {

    private final ReasonCode code;
    private final HowIGotHere howIGotHere;
    private final boolean validity;

    protected HeuristicReasonWithValidity(ReasonCode code, HowIGotHere howIGotHere, boolean validity) {
        this.code = code;
        this.howIGotHere = howIGotHere;
        this.validity = validity;
    }

    @Override
    public boolean isValid() {
        return validity;
    }

    @Override
    public String textForGraph() {
        return code.name();
    }

    @Override
    public HowIGotHere getHowIGotHere() {
        return howIGotHere;
    }

    @Override
    public ReasonCode getReasonCode() {
        return code;
    }

    @Override
    public GraphNodeId getNodeId() {
        return howIGotHere.getEndNodeId();
    }

    @Override
    public Evaluation getEvaluationAction() {
        return code.getEvaluationAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeuristicReasonWithValidity that = (HeuristicReasonWithValidity) o;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "SimpleHeuristicReason{" +
                "code=" + code +
                ", howIGotHere=" + howIGotHere +
                '}';
    }
}
