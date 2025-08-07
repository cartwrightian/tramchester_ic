package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.core.GraphEntityProperties;
import com.tramchester.graph.core.GraphId;
import org.neo4j.graphdb.Entity;

import java.util.Map;

public class GraphPropsNeo4J implements GraphEntityProperties.GraphProps {
    private final Entity entity;

    private GraphPropsNeo4J(final Entity entity) {
        this.entity = entity;
    }

    public static GraphPropsNeo4J wrap(final Entity entity) {
        return new GraphPropsNeo4J(entity);
    }

    @Override
    public void setProperty(final String key, final Object value) {
        entity.setProperty(key, value);
    }

    @Override
    public Object getProperty(final String key) {
        return entity.getProperty(key);
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return entity.getAllProperties();
    }

    @Override
    public boolean hasProperty(final String key) {
        return entity.hasProperty(key);
    }

    @Override
    public void removeProperty(String key) {
        entity.removeProperty(key);
    }
}
