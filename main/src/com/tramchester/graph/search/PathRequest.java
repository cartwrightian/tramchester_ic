package com.tramchester.graph.search;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;

import java.time.Duration;
import java.util.EnumSet;

public class PathRequest {
    private final GraphNode startNode;
    private final TramDate queryDate;
    private final TramTime queryTime;
    private final int numChanges;
    private final EnumSet<TransportMode> requestedModes;
    private final EnumSet<TransportMode> destinationModes;
    private final Duration maxInitialWait;

    private final ServiceHeuristics serviceHeuristics;

    public PathRequest(JourneyRequest journeyRequest, GraphNode startNode, int numChanges, ServiceHeuristics serviceHeuristics,
                       Duration maxInitialWait, EnumSet<TransportMode> desintationModes) {
        this(startNode, journeyRequest.getDate(), journeyRequest.getOriginalTime(), numChanges, serviceHeuristics,
                journeyRequest.getRequestedModes(),
                maxInitialWait, desintationModes);
    }

    public PathRequest(GraphNode startNode, TramDate queryDate, TramTime queryTime, int numChanges,
                       ServiceHeuristics serviceHeuristics, EnumSet<TransportMode> requestedModes,
                       Duration maxInitialWait, EnumSet<TransportMode> destinationModes) {
        this.startNode = startNode;
        this.queryDate = queryDate;
        this.queryTime = queryTime;
        this.numChanges = numChanges;
        this.serviceHeuristics = serviceHeuristics;
        this.requestedModes = requestedModes;
        this.maxInitialWait = maxInitialWait;
        this.destinationModes = destinationModes;
    }

    public ServiceHeuristics getServiceHeuristics() {
        return serviceHeuristics;
    }

    public TramTime getActualQueryTime() {
        return queryTime;
    }

    public int getNumChanges() {
        return numChanges;
    }

    @Override
    public String toString() {
        return "PathRequest{" +
                "startNode=" + startNode +
                ", queryTime=" + queryTime +
                ", numChanges=" + numChanges +
                ", serviceHeuristics=" + serviceHeuristics +
                ", queryDate=" + queryDate +
                ", requestedModes=" + requestedModes +
                ", maxInitialWait=" + maxInitialWait +
                '}';
    }

    public TramDate getQueryDate() {
        return queryDate;
    }

    public EnumSet<TransportMode> getRequestedModes() {
        return requestedModes;
    }

    public Duration getMaxInitialWait() {
        return maxInitialWait;
    }

    public GraphNode getStartNode() {
        return startNode;
    }

//        public BranchOrderingPolicy getSelector() {
//            return selector;
//        }

    public EnumSet<TransportMode> getDesintationModes() {
        return destinationModes;
    }
}
