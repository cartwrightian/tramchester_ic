package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.collections.IterableWithEmptyCheck;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.core.*;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.Towards;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.DIVERSION;

public abstract class StateBuilder<T extends TraversalState> implements Towards<T> {
    private static final Logger logger = LoggerFactory.getLogger(StateBuilder.class);

    private final TramDate queryDate;
    private final TowardsDestination towardsDestination;
    private final int queryHour;

    protected StateBuilder(final StateBuilderParameters parameters) {
        this.queryDate = parameters.queryDate();
        this.towardsDestination = parameters.towardsDestination();
        this.queryHour = parameters.queryHour();
    }

    public TramDate getQueryDate() {
        return queryDate;
    }

    public Stream<GraphRelationship> addValidDiversions(final Stream<GraphRelationship> existing,
                                                        final GraphNode node, final JourneyStateUpdate journeyStateUpdate,
                                                        final GraphTransaction txn) {

        if (journeyStateUpdate.onDiversion()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Already on diversion " + node.getStationId());
            }
            return existing;
        }

        // TODO Is this ordering the right approach, or require only one diversion from each location (doesn't work either?)
        if (node.hasRelationship(txn, GraphDirection.Outgoing, DIVERSION)) {
            final Stream<GraphRelationship> diversions = node.getRelationships(txn, GraphDirection.Outgoing, DIVERSION).
                    filter(diversion -> diversion.validOn(queryDate)).
                    sorted(Comparator.comparing(GraphRelationship::getCost));

            // TODO ordering here?
            return Streams.concat(existing, diversions);
        }

        return existing;
    }

    protected int getQueryHour() {
        return queryHour;
    }

    protected <R extends GraphRelationship> Stream<R> filterExcludingNode(final GraphTransaction txn,
                                                                          final Stream<R> relationships,
                                                                          final NodeId hasNodeId) {
        final GraphNodeId nodeId = hasNodeId.nodeId();
        return relationships.filter(relationship -> !relationship.getEndNodeId(txn).equals(nodeId));
    }

    protected IterableWithEmptyCheck<GraphRelationship> getTowardsDestinationFromRouteStation(GraphNode node, GraphTransaction txn) {
        return towardsDestination.fromRouteStation(txn, node);
    }

    public IterableWithEmptyCheck<GraphRelationship> getTowardsDestinationFromPlatform(GraphTransaction txn, GraphNode node) {
        return towardsDestination.fromPlatform(txn, node);
    }

    public IterableWithEmptyCheck<GraphRelationship> getTowardsDestinationFromNonPlatformStation(GraphTransaction txn, GraphNode node) {
        return towardsDestination.fromStation(txn, node);
    }

    protected IterableWithEmptyCheck<GraphRelationship> getTowardsDestinationFromWalk(GraphTransaction txn, GraphNode node) {
        return towardsDestination.fromWalk(txn, node);
    }
}
