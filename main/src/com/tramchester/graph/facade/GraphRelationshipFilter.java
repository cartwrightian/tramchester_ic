package com.tramchester.graph.facade;

import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import org.neo4j.graphdb.Relationship;

import java.util.function.Predicate;

public class GraphRelationshipFilter  implements Predicate<Relationship> {
        private final ImmutableGraphTransactionNeo4J txn;
        private final Predicate<GraphRelationship> contained;

        public GraphRelationshipFilter(ImmutableGraphTransactionNeo4J txn, Predicate<GraphRelationship> contained) {
            this.txn = txn;
            this.contained = contained;
        }

        @Override
        public boolean test(final Relationship relationship) {
            return contained.test(txn.wrapRelationship(relationship));
        }

}
