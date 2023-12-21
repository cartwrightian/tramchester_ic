package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;

import jakarta.validation.Valid;
import java.nio.file.Path;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class GraphDBAppConfig extends Configuration implements GraphDBConfig {

    // TODO Make a path, rename config name
    private final String graphName;
    private final String neo4jPagecacheMemory;
    private final String memoryTransactionGlobalMaxSize;

    public GraphDBAppConfig(@JsonProperty(value = "graphName", required = true) String graphName,
                            @JsonProperty(value = "neo4jPagecacheMemory", required = true) String neo4jPagecacheMemory,
                            @JsonProperty(value = "memoryTransactionGlobalMaxSize", required = true) String memoryTransactionGlobalMaxSize) {
        this.graphName = graphName;
        this.neo4jPagecacheMemory = neo4jPagecacheMemory;
        this.memoryTransactionGlobalMaxSize = memoryTransactionGlobalMaxSize;
    }

    @Override
    public Path getDbPath() {
        return Path.of(graphName);
    }

    // page cache memory for neo4j
    // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing
    @Override
    public String getNeo4jPagecacheMemory() {
        return neo4jPagecacheMemory;
    }

    @Override
    public String getMemoryTransactionGlobalMaxSize() {
        return memoryTransactionGlobalMaxSize;
    }

    @Override
    public String toString() {
        return "GraphDBAppConfig{" +
                "graphName='" + graphName + '\'' +
                ", neo4jPagecacheMemory='" + neo4jPagecacheMemory + '\'' +
                ", memoryTransactionGlobalMaxSize='" + memoryTransactionGlobalMaxSize + '\'' +
                '}';
    }
}
