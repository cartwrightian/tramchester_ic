package com.tramchester.graph.facade;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class DBSchema {
    private static final Logger logger = LoggerFactory.getLogger(DBSchema.class);

    private final Schema schema;

    public DBSchema(Schema schema) {
        this.schema = schema;
    }

    public void createIndex(GraphLabel graphLabel, GraphPropertyKey property) {
        schema.indexFor(graphLabel).on(property.getText()).create();
    }

    public void waitForIndexes() {
        schema.awaitIndexesOnline(5, TimeUnit.SECONDS);

        schema.getIndexes().forEach(indexDefinition -> {
            Schema.IndexState state = schema.getIndexState(indexDefinition);
            if (indexDefinition.isNodeIndex()) {
                logger.info(String.format("Node Index %s labels %s keys %s state %s",
                        indexDefinition.getName(),
                        indexDefinition.getLabels(), indexDefinition.getPropertyKeys(), state));
            } else {
                logger.info(String.format("Non-Node Index %s keys %s state %s",
                        indexDefinition.getName(), indexDefinition.getPropertyKeys(), state));
            }
        });

        schema.getConstraints().forEach(definition -> logger.info(String.format("Constraint label %s keys %s type %s",
                definition.getLabel(), definition.getPropertyKeys(), definition.getConstraintType()
        )));
    }
}
