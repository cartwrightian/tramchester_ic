package com.tramchester.graph.search.diagnostics;

import com.tramchester.graph.facade.GraphNodeId;

import java.util.Objects;

public abstract class HeuristicsReason {

    protected final HowIGotHere howIGotHere;
    protected final ReasonCode code;

    public HowIGotHere getHowIGotHere() {
        return howIGotHere;
    }

    protected HeuristicsReason(final ReasonCode code, final HowIGotHere path) {
        this.code = code;
        this.howIGotHere = path;
    }

    public String textForGraph() {
        return code.name();
    }

    public ReasonCode getReasonCode() {
        return code;
    }

    // DEFAULT
    public boolean isValid() {
        return false;
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeuristicsReason that = (HeuristicsReason) o;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    public GraphNodeId getNodeId() {
        return howIGotHere.getEndNodeId();
    }

    public boolean isCached() {
        return switch (code) {
            case CachedNotAtHour,CachedNotOnQueryDate,
                    CachedDoesNotOperateOnTime, CachedTooManyInterchangesRequired,
                    CachedTooManyRouteChangesRequired, CachedRouteNotOnQueryDate -> true;
            default -> false;
        };
    }
}
