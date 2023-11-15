package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;

public abstract class StationStateBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StationStateBuilder.class);

    public <R extends ImmutableGraphRelationship> Stream<R> addValidDiversions(GraphNode node, Stream<R> relationships,
                                                                               TraversalState traversalState, boolean alreadyOnDiversion, GraphTransaction txn) {

        if (alreadyOnDiversion) {
            logger.info("Already on diversion " + node.getStationId());
            return relationships;
        }

        if (node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            TramDate queryDate = traversalState.traversalOps.getQueryDate();
            Stream<R> diversions = node.getRelationships(txn, Direction.OUTGOING, DIVERSION);
            Stream<R> validOnDate = diversions.filter(relationship -> relationship.validOn(queryDate));
            return Stream.concat(validOnDate, relationships);
        }

        return relationships;
    }


}
