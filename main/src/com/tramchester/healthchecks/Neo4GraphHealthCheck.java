package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.graph.core.GraphDatabase;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@LazySingleton
public class Neo4GraphHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(Neo4GraphHealthCheck.class);

    private static final long TIMEOUT_MILLIS = 5;
    private final GraphDatabase graphDatabase;
    private final TramchesterConfig config;
    private final boolean neo4J;

    @Inject
    public Neo4GraphHealthCheck(GraphDatabase graphDatabase, ServiceTimeLimits serviceTimeLimits, TramchesterConfig config) {
        super(serviceTimeLimits);
        this.graphDatabase = graphDatabase;
        this.config = config;
        this.neo4J = !config.getInMemoryGraph();
    }

    @Override
    protected Result check() {
        final Path dbPath = config.getGraphDBConfig().getDbPath();

        if (graphDatabase.isAvailable(TIMEOUT_MILLIS)) {
            final String available = "Graph DB available at " + dbPath;
            logger.info(available);
            return Result.healthy(available);
        }
        final String unavailable = "Graph DB unavailable, path " + dbPath;
        logger.error(unavailable);
        return Result.unhealthy(unavailable);
    }

    @Override
    public String getName() {
        return "graphDB";
    }

    @Override
    public boolean isEnabled() {
        return config.getPlanningEnabled() && neo4J;
    }
}
