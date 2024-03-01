package com.tramchester.graph.facade;

import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

import java.util.function.Predicate;

public class GraphRelationshipFilter  implements Predicate<Relationship> {
        private final GraphTransaction txn;
        private final Predicate<GraphRelationship> contained;

        public GraphRelationshipFilter(GraphTransaction txn, Predicate<GraphRelationship> contained) {
            this.txn = txn;
            this.contained = contained;
        }

        @Override
        public boolean test(final Relationship relationship) {
            return contained.test(txn.wrapRelationship(relationship, TransportRelationshipTypes.from(relationship)));
        }

}
