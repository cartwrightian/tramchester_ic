package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;

import java.util.EnumSet;
import java.util.Objects;

public final class StateBuilderParameters {
    private final TramDate queryDate;
    private final int queryHour;
    private final LocationCollection destinationIds;
    private final NodeContentsRepository nodeContents;
    private final boolean depthFirst;
    private final boolean interchangesOnly;
    private final TransportRelationshipTypes[] currentModes;

    public StateBuilderParameters(TramDate queryDate, int queryHour,
                                  LocationCollection destinationIds,
                                  NodeContentsRepository nodeContents,
                                  TramchesterConfig config, EnumSet<TransportMode> requestedModes) {
        this.queryDate = queryDate;
        this.queryHour = queryHour;
        this.destinationIds = destinationIds;
        this.nodeContents = nodeContents;
        this.depthFirst = config.getDepthFirst();
        this.interchangesOnly = config.getChangeAtInterchangeOnly();
        this.currentModes =  TransportRelationshipTypes.forModes(requestedModes);
        ;
    }

    public TramDate queryDate() {
        return queryDate;
    }

    public int queryHour() {
        return queryHour;
    }

    public LocationCollection destinationIds() {
        return destinationIds;
    }

    public NodeContentsRepository nodeContents() {
        return nodeContents;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StateBuilderParameters) obj;
        return Objects.equals(this.queryDate, that.queryDate) &&
                this.queryHour == that.queryHour &&
                Objects.equals(this.destinationIds, that.destinationIds) &&
                Objects.equals(this.nodeContents, that.nodeContents) &&
                this.depthFirst == that.depthFirst &&
                this.interchangesOnly == that.interchangesOnly &&
                Objects.equals(this.currentModes, that.currentModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryDate, queryHour, destinationIds, nodeContents, depthFirst, interchangesOnly, currentModes);
    }

    @Override
    public String toString() {
        return "StateBuilderParameters[" +
                "queryDate=" + queryDate + ", " +
                "queryHour=" + queryHour + ", " +
                "destinationIds=" + destinationIds + ", " +
                "nodeContents=" + nodeContents + ", " +
                "depthFirst=" + depthFirst + ", " +
                "interchangesOnly=" + interchangesOnly + ", " +
                "currentModes=" + currentModes + ']';
    }


}
