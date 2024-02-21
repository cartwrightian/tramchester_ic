package com.tramchester.graph.search.diagnostics;

import java.util.Objects;

public class CachedHeuristicReason extends SimpleHeuristicReason {
    private final HeuristicsReason contained;

    public CachedHeuristicReason(HeuristicsReason contained, HowIGotHere howIGotHere) {
        super(getReasonCodeForCached(contained.getReasonCode()), howIGotHere);
        this.contained = contained;
    }

    private static ReasonCode getReasonCodeForCached(final ReasonCode code) {
        return switch (code) {
            case NotAtHour -> ReasonCode.CachedNotAtHour;
            case DoesNotOperateOnTime -> ReasonCode.CachedDoesNotOperateOnTime;
            case TooManyRouteChangesRequired -> ReasonCode.CachedTooManyRouteChangesRequired;
            case RouteNotOnQueryDate -> ReasonCode.CachedRouteNotOnQueryDate;
            case NotOnQueryDate -> ReasonCode.CachedNotOnQueryDate;
            case TooManyInterchangesRequired -> ReasonCode.CachedTooManyInterchangesRequired;
            default -> throw new RuntimeException("No cached reason code for " + code.name());
        };
    }

    @Override
    public String textForGraph() {
        return "Cached( " + contained.textForGraph() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CachedHeuristicReason that = (CachedHeuristicReason) o;
        return Objects.equals(contained, that.contained);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contained);
    }
}
