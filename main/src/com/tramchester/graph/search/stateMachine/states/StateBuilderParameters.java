package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.search.stateMachine.TowardsDestination;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

public final class StateBuilderParameters {
    private final TramDate queryDate;
    private final int queryHour;
    private final TowardsDestination towardsDestination;
    private final boolean depthFirst;
    private final boolean interchangesOnly;
    private final TransportRelationshipTypes[] currentModes;

    public StateBuilderParameters(final TramDate queryDate, final TramTime queryTime,
                                  final TowardsDestination towardsDestination,
                                  final TramchesterConfig config, final EnumSet<TransportMode> requestedModes) {
        this.queryDate = queryDate;
        this.queryHour = queryTime.getHourOfDay();
        this.towardsDestination = towardsDestination;
        this.depthFirst = config.getDepthFirst();
        this.interchangesOnly = config.getChangeAtInterchangeOnly();
        this.currentModes =  TransportRelationshipTypes.forModes(requestedModes);
    }

    public TramDate queryDate() {
        return queryDate;
    }

    public int queryHour() {
        return queryHour;
    }

    public boolean depthFirst() {
        return depthFirst;
    }

    public boolean interchangesOnly() {
        return interchangesOnly;
    }

    public TransportRelationshipTypes[] currentModes() {
        return currentModes;
    }

    public TowardsDestination towardsDestination() {
        return towardsDestination;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StateBuilderParameters) obj;
        return Objects.equals(this.queryDate, that.queryDate) &&
                this.queryHour == that.queryHour &&
                this.depthFirst == that.depthFirst &&
                this.interchangesOnly == that.interchangesOnly &&
                Arrays.equals(this.currentModes, that.currentModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryDate, queryHour, depthFirst, interchangesOnly, Arrays.hashCode(currentModes));
    }

    @Override
    public String toString() {
        return "StateBuilderParameters[" +
                "queryDate=" + queryDate + ", " +
                "queryHour=" + queryHour + ", " +
                "depthFirst=" + depthFirst + ", " +
                "interchangesOnly=" + interchangesOnly + ", " +
                "currentModes=" + Arrays.toString(currentModes) + ']';
    }



}
