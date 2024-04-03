package com.tramchester.graph.search.diagnostics;

import java.util.Objects;

public class CachedHeuristicReason extends HeuristicReasonWithValidity {
    private final HeuristicsReason wasCached;

    public CachedHeuristicReason(HeuristicsReason wasCached, HowIGotHere howIGotHere) {
        super(getReasonCodeForCached(wasCached.getReasonCode()), howIGotHere, wasCached.isValid());
        this.wasCached = wasCached;
    }

    public CachedHeuristicReason(HeuristicsReason reason) {
        this(reason, reason.getHowIGotHere());
    }

    @Override
    public String textForGraph() {
        return "Cached( " + wasCached.textForGraph() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CachedHeuristicReason that = (CachedHeuristicReason) o;
        return Objects.equals(wasCached, that.wasCached);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), wasCached);
    }

    private static ReasonCode getReasonCodeForCached(final ReasonCode code) {
        return switch (code) {
            case NotAtHour -> ReasonCode.CachedNotAtHour;
            case DoesNotOperateOnTime -> ReasonCode.CachedDoesNotOperateOnTime;
            case TooManyRouteChangesRequired -> ReasonCode.CachedTooManyRouteChangesRequired;
            case RouteNotOnQueryDate -> ReasonCode.CachedRouteNotOnQueryDate;
            case NotOnQueryDate -> ReasonCode.CachedNotOnQueryDate;
            case TooManyInterchangesRequired -> ReasonCode.CachedTooManyInterchangesRequired;
            case AlreadySeenRouteStation -> ReasonCode.AlreadySeenRouteStation;
            default -> throw new RuntimeException("No cached reason code for " + code.name());
        };
    }

    public HeuristicsReason getContained() {
        return wasCached;
    }
}
