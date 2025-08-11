package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.MutableGraphTransaction;
import org.neo4j.graphdb.Relationship;

public class GraphTestHelperNeo4J {

    /***
     * Test support only
     * @param txn containing transaction, using returned Relationship outside of this txn will give unpredictable results
     * @param graphRelationship the facaded relationship
     * @return underlying relationship
     */
    public Relationship getUnderlyingUnsafe(MutableGraphTransaction txn, final GraphRelationship graphRelationship) {
        if (txn instanceof MutableGraphTransactionNeo4J neo4J) {
            return neo4J.unwrap(graphRelationship);
        } else {
            throw new RuntimeException("not implemented");
        }
    }
}
