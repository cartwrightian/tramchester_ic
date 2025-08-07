package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.core.GraphRelationship;
import org.neo4j.graphdb.Relationship;

public class GraphTestHelperNeo4J {

    /***
     * Test support only
     * @param txn containing transaction, using returned Relationship outside of this txn will give unpredictable results
     * @param graphRelationship the facaded relationship
     * @return underlying relationship
     */
    public Relationship getUnderlyingUnsafe(MutableGraphTransactionNeo4J txn, final GraphRelationship graphRelationship) {
        return txn.unwrap(graphRelationship);
    }
}
