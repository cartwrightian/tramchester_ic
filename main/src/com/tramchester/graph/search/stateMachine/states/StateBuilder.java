package com.tramchester.graph.search.stateMachine.states;

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

    public <R extends GraphRelationship> FilterByDestinations<R> getTowardsDestination(final Stream<R> outgoing) {
        return towardsDestination.getTowardsDestination(outgoing);
    }

    public Stream<ImmutableGraphRelationship> addValidDiversions(final GraphNode node, JourneyStateUpdate journeyStateUpdate, final GraphTransaction txn) {

        if (journeyStateUpdate.onDiversion()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Already on diversion " + node.getStationId());
            }
            return Stream.empty();
        }

        if (node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            return node.getRelationships(txn, Direction.OUTGOING, DIVERSION).filter(diversion -> diversion.validOn(queryDate));
        }

        return Stream.empty();
    }

    protected int getQueryHour() {
        return queryHour;
    }

    protected IdFor<Trip> getTripId(ImmutableGraphRelationship relationship) {
        return nodeContents.getTripId(relationship);
    }

    protected <R extends GraphRelationship> Stream<R> filterExcludingEndNode(final GraphTransaction txn,
                                                                                    final Stream<R> relationships,
                                                                                    final NodeId hasNodeId) {
        final GraphNodeId nodeId = hasNodeId.nodeId();
        return relationships.filter(relationship -> !relationship.getEndNodeId(txn).equals(nodeId));
    }
}
