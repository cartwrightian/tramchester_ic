package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphEntityProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;
import static com.tramchester.graph.GraphPropertyKey.TIME;

final class PropertyContainer implements GraphEntityProperties.GraphProps {

    private final ConcurrentMap<String, Object> props;

    PropertyContainer() {
        props = new ConcurrentHashMap<>();
    }

    public PropertyContainer(final List<GraphNodeInMemory.PropertyDTO> properties) {
        this();
        properties.forEach(prop -> setProperty(prop.getKey(), prop.getValue()));
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

    @Override
    public void setTime(TramTime tramTime) {
        setProperty(TIME.getText(), tramTime);
        if (tramTime.isNextDay()) {
            setProperty(DAY_OFFSET.getText(), tramTime.isNextDay());
        }
    }

    @Override
    public TramTime getTime() {
        return (TramTime) getProperty(TIME.getText());
    }
}
