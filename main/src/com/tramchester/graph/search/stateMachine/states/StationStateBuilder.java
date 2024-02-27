package com.tramchester.graph.search.stateMachine.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StationStateBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StationStateBuilder.class);

//    public Stream<ImmutableGraphRelationship> addValidDiversions(final GraphNode node,
//                                                                 final TraversalState traversalState,
//                                                                 final boolean alreadyOnDiversion, final GraphTransaction txn) {
//
//        if (alreadyOnDiversion) {
//            logger.info("Already on diversion " + node.getStationId());
//            return Stream.empty();
//        }
//
//        if (node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
//            final TramDate queryDate = traversalState.traversalOps.getQueryDate();
//            return node.
//                    getRelationships(txn, Direction.OUTGOING, DIVERSION).
//                    filter(diversion -> diversion.validOn(queryDate));
//        }
//
//        return Stream.empty();
//    }


}
