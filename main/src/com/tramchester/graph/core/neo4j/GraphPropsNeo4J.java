package com.tramchester.graph.core.neo4j;

import com.tramchester.graph.core.HaveGraphProperties;
import org.neo4j.graphdb.Entity;

import java.util.Map;

public class KeyValuePropsNeo4J implements HaveGraphProperties.KeyValueProps {
    private final Entity entity;

    private KeyValuePropsNeo4J(final Entity entity) {
        this.entity = entity;
    }

    public static KeyValuePropsNeo4J wrap(final Entity entity) {
        return new KeyValuePropsNeo4J(entity);
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
    public String getId() {
        return entity.getElementId();
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return entity.getAllProperties();
    }

    @Override
    public boolean hasProperty(final String key) {
        return entity.hasProperty(key);
    }
}
