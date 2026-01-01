package com.tramchester.graph.search;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;

import java.util.EnumSet;

public class PathRequest {
    private final GraphNode startNode;
    private final TramDate queryDate;
    private final TramTime queryTime;
    private final int numChanges;
    private final EnumSet<TransportMode> requestedModes;
    private final EnumSet<TransportMode> destinationModes;
    private final TramDuration maxInitialWait;
    private final long maxNumberJourneys;

    private final ServiceHeuristics serviceHeuristics;

    public PathRequest(JourneyRequest journeyRequest, GraphNode startNode, int numChanges, ServiceHeuristics serviceHeuristics,
                       TramDuration maxInitialWait, EnumSet<TransportMode> desintationModes) {
        this(startNode, journeyRequest.getDate(), journeyRequest.getOriginalTime(), numChanges, serviceHeuristics,
                journeyRequest.getRequestedModes(),
                maxInitialWait, desintationModes, journeyRequest.getMaxNumberOfJourneys());
    }

    // query time here can range over the series of times
    public PathRequest(GraphNode startNode, TramDate queryDate, TramTime queryTime, int numChanges,
                       ServiceHeuristics serviceHeuristics, EnumSet<TransportMode> requestedModes,
                       TramDuration maxInitialWait, EnumSet<TransportMode> destinationModes, long maxNumberJourneys) {
        this.startNode = startNode;
        this.queryDate = queryDate;
        this.queryTime = queryTime;
        this.numChanges = numChanges;
        this.serviceHeuristics = serviceHeuristics;
        this.requestedModes = requestedModes;
        this.maxInitialWait = maxInitialWait;
        this.destinationModes = destinationModes;
        this.maxNumberJourneys = maxNumberJourneys;
    }

    public ServiceHeuristics getServiceHeuristics() {
        return serviceHeuristics;
    }

    public TramTime getActualQueryTime() {
        // not always the same as original query time from JourneyRequest
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

    public TramDuration getMaxInitialWait() {
        return maxInitialWait;
    }

    public GraphNode getStartNode() {
        return startNode;
    }

    public EnumSet<TransportMode> getDesintationModes() {
        return destinationModes;
    }

    public long getMaxNumberJourneys() {
        return maxNumberJourneys;
    }
}
