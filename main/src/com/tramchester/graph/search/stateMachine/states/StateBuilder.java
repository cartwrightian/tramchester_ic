package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.FilterByDestinations;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.Towards;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;

public abstract class StateBuilder<T extends TraversalState> implements Towards<T> {
    private static final Logger logger = LoggerFactory.getLogger(StateBuilder.class);

    private final TramDate queryDate;
    private final TowardsDestination towardsDestination;
    private final int queryHour;
    private final NodeContentsRepository nodeContents;

    protected StateBuilder(StateBuilderParameters parameters) {
        this.queryDate = parameters.queryDate();
        this.towardsDestination = parameters.towardsDestination();
        this.queryHour = parameters.queryHour();
        this.nodeContents = parameters.nodeContents();
    }

    public TramDate getQueryDate() {
        return queryDate;
    }

    public Stream<ImmutableGraphRelationship> addValidDiversions(final Stream<ImmutableGraphRelationship> existing,
                                                                 final GraphNode node, JourneyStateUpdate journeyStateUpdate,
                                                                 final GraphTransaction txn) {

        if (journeyStateUpdate.onDiversion()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Already on diversion " + node.getStationId());
            }
            return existing;
        }

        // TODO Is this ordering the right approach, or require only one diversion from each location (doesn't work either?)
        if (node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            Stream<ImmutableGraphRelationship> diversions = node.getRelationships(txn, Direction.OUTGOING, DIVERSION).
                    filter(diversion -> diversion.validOn(queryDate)).
                    sorted(Comparator.comparing(ImmutableGraphRelationship::getCost));
            //return relationships;

            // TODO ordering here?
            return Streams.concat(existing, diversions);
        }

        return existing;
    }

    protected int getQueryHour() {
        return queryHour;
    }

    protected IdFor<Trip> getTripId(ImmutableGraphRelationship relationship) {
        return nodeContents.getTripId(relationship);
    }

    protected <R extends GraphRelationship> Stream<R> filterExcludingNode(final GraphTransaction txn,
                                                                          final Stream<R> relationships,
                                                                          final NodeId hasNodeId) {
        final GraphNodeId nodeId = hasNodeId.nodeId();
        return relationships.filter(relationship -> !relationship.getEndNodeId(txn).equals(nodeId));
    }

    protected FilterByDestinations<ImmutableGraphRelationship> getTowardsDestinationFromRouteStation(GraphNode node, GraphTransaction txn) {
        return towardsDestination.fromRouteStation(txn, node);
    }

    public FilterByDestinations<ImmutableGraphRelationship> getTowardsDestinationFromPlatform(GraphTransaction txn, GraphNode node) {
        return towardsDestination.fromPlatform(txn, node);
    }

    public FilterByDestinations<ImmutableGraphRelationship> getTowardsDestinationFromNonPlatformStation(GraphTransaction txn, GraphNode node) {
        return towardsDestination.fromStation(txn, node);
    }

    protected FilterByDestinations<ImmutableGraphRelationship> getTowardsDestinationFromWalk(GraphTransaction txn, GraphNode node) {
        return towardsDestination.fromWalk(txn, node);
    }
}
