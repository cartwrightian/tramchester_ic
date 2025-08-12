package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphEntityProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class PropertyContainer implements GraphEntityProperties.GraphProps {

    private final ConcurrentMap<String, Object> props;

    PropertyContainer() {
        props = new ConcurrentHashMap<>();
    }

    @Override
    public void setProperty(final String key, final Object value) {
        props.put(key, value);
    }

    @Override
    public Object getProperty(final String key) {
        if (props.containsKey(key)) {
            return props.get(key);
        }
        throw new RuntimeException("No such property " + key);
    }


    @Override
    public Map<String, Object> getAllProperties() {
        return new HashMap<>(props);
    }

    @Override
    public boolean hasProperty(final String key) {
        return props.containsKey(key);
    }

    @Override
    public void removeProperty(final String key) {
        props.remove(key);
    }
}
