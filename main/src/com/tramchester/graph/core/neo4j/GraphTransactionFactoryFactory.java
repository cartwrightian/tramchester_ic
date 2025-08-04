package com.tramchester.graph.core.neo4j;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.graph.caches.SharedNodeCache;
import com.tramchester.graph.caches.SharedRelationshipCache;
import jakarta.inject.Inject;
import org.neo4j.graphdb.GraphDatabaseService;

@LazySingleton
public class GraphTransactionFactoryFactory {
    private final SharedNodeCache nodeCache;
    private final SharedRelationshipCache relationshipCache;
    private final GraphReferenceMapper graphReferenceMapper;

    @Inject
    public GraphTransactionFactoryFactory(SharedNodeCache nodeCache, SharedRelationshipCache relationshipCache, GraphReferenceMapper graphReferenceMapper) {
        this.nodeCache = nodeCache;
        this.relationshipCache = relationshipCache;
        this.graphReferenceMapper = graphReferenceMapper;
    }

    public GraphTransactionFactory create(GraphDatabaseService databaseService, GraphDBConfig dbConfig) {
        return GraphTransactionFactory.create(databaseService, nodeCache,
                relationshipCache, graphReferenceMapper, dbConfig.enableDiagnostics());
    }
}
