package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.OptionalResourceIterator;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;

public abstract class StateBuilder<T extends TraversalState> implements Towards<T> {
    private static final Logger logger = LoggerFactory.getLogger(StateBuilder.class);

    private final TramDate queryDate;
    private final LocationCollection destinationIds;
    private final int queryHour;
    private final NodeContentsRepository nodeContents;

    private static final EnumSet<TransportRelationshipTypes> haveStationId = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    protected StateBuilder(StateBuilderParameters parameters) {
        this.queryDate = parameters.queryDate();
        this.destinationIds = parameters.destinationIds();
        this.queryHour = parameters.queryHour();
        this.nodeContents = parameters.nodeContents();
    }

    public TramDate getQueryDate() {
        return queryDate;
    }

    public <R extends GraphRelationship> OptionalResourceIterator<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> destinationIds.contains(getLocationIdFor(depart))).
                collect(Collectors.toList());
        return OptionalResourceIterator.from(filtered);
    }

    private static LocationId getLocationIdFor(final GraphRelationship depart) {
        final TransportRelationshipTypes departType = depart.getType();
        if (haveStationId.contains(departType)) {
            return new LocationId(depart.getStationId());
        } else if (departType==GROUPED_TO_PARENT) {
            return new LocationId(depart.getStationGroupId());
        } else {
            throw new RuntimeException("Unsupported relationship type " + departType);
        }
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
