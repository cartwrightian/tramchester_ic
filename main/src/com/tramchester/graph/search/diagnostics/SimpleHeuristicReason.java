package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.facade.GraphNodeId;

import java.util.Objects;

public class SimpleHeuristicReason implements HeuristicsReason {

    private final ReasonCode code;
    private final HowIGotHere howIGotHere;

    protected SimpleHeuristicReason(ReasonCode code, HowIGotHere howIGotHere) {
        this.code = code;
        this.howIGotHere = howIGotHere;
    }

    @Override
    public boolean isValid() {
        return false;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleHeuristicReason that = (SimpleHeuristicReason) o;
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
