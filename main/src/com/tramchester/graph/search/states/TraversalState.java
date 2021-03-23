package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.*;

import static java.lang.String.format;

public abstract class TraversalState implements ImmuatableTraversalState {

    protected final NodeContentsRepository nodeOperations;
    protected final TripRepository tripRepository;

    private final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    private final TraversalState parent;

    private final IdSet<Station> destinationStationIds;
    private final IdSet<Route> destinationRouteIds;

    protected final Set<Long> destinationNodeIds;
    protected final Builders builders;

    private TraversalState child;

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    // initial only
    protected TraversalState(TripRepository tripRepository, SortsPositions sortsPositions, NodeContentsRepository nodeOperations,
                             Set<Long> destinationNodeIds, Set<Station> destinationStations,
                             LatLong destinationLatLonHint, TramchesterConfig config) {
        this.tripRepository = tripRepository;
        this.nodeOperations = nodeOperations;
        this.destinationNodeIds = destinationNodeIds;
        this.destinationStationIds = destinationStations.stream().collect(IdSet.collector());
        this.destinationRouteIds = destinationStations.stream().map(Station::getRoutes).flatMap(Collection::stream).
                collect(IdSet.collector());

        this.costForLastEdge = 0;
        this.parentCost = 0;
        this.parent = null;
        this.outbounds = new ArrayList<>();

        this.builders = new Builders(sortsPositions, destinationLatLonHint, config);
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.nodeOperations = parent.nodeOperations;
        this.tripRepository = parent.tripRepository;
        this.destinationNodeIds = parent.destinationNodeIds;
        this.destinationStationIds = parent.destinationStationIds;
        this.destinationRouteIds = parent.destinationRouteIds;
        this.builders = parent.builders;

        this.parent = parent;
        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    protected abstract TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node,
                                                      JourneyState journeyState, int cost);

    protected TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                             JourneyState journeyState, int cost) {
        throw new RuntimeException(format("Multi label Not implemented at %s for %s labels were %s",
                this.toString(), journeyState, nodeLabels));
    }

    public TraversalState nextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                    JourneyState journeyState, int cost) {
        if (nodeLabels.size()==1) {
            GraphBuilder.Labels nodeLabel = nodeLabels.iterator().next();
            child = createNextState(nodeLabel, node, journeyState, cost);
        } else {
            child = createNextState(nodeLabels, node, journeyState, cost);
        }

        return child;
    }

    public void dispose() {
        if (child!=null) {
            child.dispose();
            child = null;
        }
    }

    public Iterable<Relationship> getOutbounds() {
        return outbounds;
    }

    // TODO Return iterable instead?
    protected static List<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        ArrayList<Relationship> results = new ArrayList<>();
        for (Relationship relationship: relationships) {
            if (relationship.getEndNode().getId() != nodeId) {
                results.add(relationship);
            }
        }
        return results;
    }

    @NotNull
    protected List<Relationship> getTowardsDestination(Iterable<Relationship> outgoing) {
        // towards final destination, just follow this one
        List<Relationship> towardsDestination = new ArrayList<>();
        outgoing.forEach(depart ->
        {
            if (destinationStationIds.contains(GraphProps.getStationIdFrom(depart))) {
                towardsDestination.add(depart);
            }
        });
        return towardsDestination;
    }

    public int getTotalCost() {
        return parentCost + getCurrentCost();
    }

    private int getCurrentCost() {
        return costForLastEdge;
    }

    @Override
    public String toString() {
        return "TraversalState{" +
                "costForLastEdge=" + costForLastEdge +
                ", parentCost=" + parentCost + System.lineSeparator() +
                ", parent=" + parent +
                '}';
    }

    public boolean hasDestinationRoute(IdFor<Route> routeId) {
        return destinationRouteIds.contains(routeId);
    }

    public static class Builders {

        protected final RouteStationStateOnTrip.Builder routeStation;
        protected final RouteStationStateEndTrip.Builder routeStationEndTrip;
        protected final RouteStationStateJustBoarded.Builder routeStationJustBoarded;
        protected final NoPlatformStationState.Builder noPlatformStation;
        protected final TramStationState.Builder tramStation;
        protected final ServiceState.Builder service;
        protected final PlatformState.Builder platform;
        protected final WalkingState.Builder walking;
        protected final MinuteState.Builder minute;
        protected final HourState.Builder hour;
        protected final DestinationState.Builder destination;

        public Builders(SortsPositions sortsPositions, LatLong destinationLatLon, TramchesterConfig config) {
            routeStation = new RouteStationStateOnTrip.Builder();
            routeStationEndTrip = new RouteStationStateEndTrip.Builder();
            routeStationJustBoarded = new RouteStationStateJustBoarded.Builder(sortsPositions, destinationLatLon);
            noPlatformStation = new NoPlatformStationState.Builder();
            service = new ServiceState.Builder();
            platform = new PlatformState.Builder();
            walking = new WalkingState.Builder();
            minute = new MinuteState.Builder(config);
            tramStation = new TramStationState.Builder();
            hour = new HourState.Builder();
            destination = new DestinationState.Builder();
        }
    }

}
