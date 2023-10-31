package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;

public abstract class StationStateBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StationStateBuilder.class);

//    protected Stream<Relationship> addValidDiversions(GraphNode node, Iterable<Relationship> relationships,
//                                                      TraversalState traversalState, boolean alreadyOnDiversion) {
//        return addValidDiversions(node, Streams.stream(relationships), traversalState, alreadyOnDiversion);
//    }


    public Stream<GraphRelationship> addValidDiversions(GraphNode node, Stream<GraphRelationship> relationships,
                                                        TraversalState traversalState, boolean alreadyOnDiversion, GraphTransaction txn) {

        if (alreadyOnDiversion) {
            logger.info("Already on diversion " + GraphProps.getStationId(node));
            return relationships;
        }

        if (node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            TramDate queryDate = traversalState.traversalOps.getQueryDate();
            Stream<GraphRelationship> diversions = node.getRelationships(txn, Direction.OUTGOING, DIVERSION);
            Stream<GraphRelationship> validOnDate = diversions.filter(relationship -> relationship.validOn(queryDate)); // GraphProps.validOn(queryDate, relationship));
            return Stream.concat(validOnDate, relationships);
        }

        return relationships;
    }


}
