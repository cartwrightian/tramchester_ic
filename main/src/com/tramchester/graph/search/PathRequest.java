package com.tramchester.graph.search;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;

public class PathRequest {
    private final JourneyRequest journeyRequest;
    private final GraphNode startNode;
    private final TramTime actualQueryTime;
    private final int actualNumChanges;
    private final ImmutableEnumSet<TransportMode> destinationModes;
    private final TramDuration maxInitialWait;

    private final ServiceHeuristics serviceHeuristics;

    public PathRequest(JourneyRequest journeyRequest, GraphNode startNode, int actualNumChanges, ServiceHeuristics serviceHeuristics,
                       TramDuration maxInitialWait, ImmutableEnumSet<TransportMode> desintationModes) {
        this(journeyRequest, startNode, journeyRequest.getOriginalTime(), actualNumChanges, serviceHeuristics,
                maxInitialWait, desintationModes);
    }

    // query time here can range over the series of times
    public PathRequest(JourneyRequest journeyRequest, GraphNode startNode, TramTime actualQueryTime, int actualNumChanges,
                       ServiceHeuristics serviceHeuristics,
                       TramDuration maxInitialWait, ImmutableEnumSet<TransportMode> destinationModes) {
        this.journeyRequest = journeyRequest;
        this.startNode = startNode;
        this.actualQueryTime = actualQueryTime;
        this.actualNumChanges = actualNumChanges;
        this.serviceHeuristics = serviceHeuristics;
        this.maxInitialWait = maxInitialWait;
        this.destinationModes = destinationModes;
    }

    public ServiceHeuristics getServiceHeuristics() {
        return serviceHeuristics;
    }

    public TramTime getActualQueryTime() {
        // not always the same as original query time from JourneyRequest
        return actualQueryTime;
    }

    public int getActualNumChanges() {
        return actualNumChanges;
    }

    @Override
    public String toString() {
        return "PathRequest{" +
                "startNode=" + startNode.getId() +
                ", journeyRequest=" + journeyRequest +
                ", actualQueryTime=" + actualQueryTime +
                ", actualNumChanges=" + actualNumChanges +
                ", serviceHeuristics=" + serviceHeuristics +
                ", maxInitialWait=" + maxInitialWait +
                '}';
    }

    public TramDate getQueryDate() {
        return journeyRequest.getDate();
    }

    public ImmutableEnumSet<TransportMode> getRequestedModes() {
        return journeyRequest.getRequestedModes();
    }

    public TramDuration getMaxInitialWait() {
        return maxInitialWait;
    }

    public GraphNode getStartNode() {
        return startNode;
    }

    public ImmutableEnumSet<TransportMode> getDesintationModes() {
        return destinationModes;
    }

    public long getMaxNumberJourneys() {
        return journeyRequest.getMaxNumberOfJourneys();
    }
}
