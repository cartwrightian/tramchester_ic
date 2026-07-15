package com.tramchester.graph.core.inMemory;

import com.google.common.collect.ImmutableMap;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphEntityProperties;

import java.util.*;

final class PropertyContainer implements GraphEntityProperties.GraphProps<PropertyContainer> {

    private static final int INITIAL_SIZE = 4;

    // NOTE: EnumMap pre-allocates space for every instance of the Enum, which is inefficient in this case
    private final Map<GraphPropertyKey, Object> props;
    private final EnumSet<GraphPropertyKey> used;
    private final boolean diagnostics;

    PropertyContainer(final boolean diagnostics) {
        this(new HashMap<>(INITIAL_SIZE), diagnostics);
    }

    public PropertyContainer(final List<PropertyDTO> properties) {
        this(false);
        properties.forEach(prop -> setProperty(GraphPropertyKey.parse(prop.getKey()), prop.getContainedValue()));
    }

    private PropertyContainer(final HashMap<GraphPropertyKey, Object> props, final boolean diagnostics) {
        this.props = props;
        this.diagnostics = diagnostics;
        used = diagnostics ? EnumSet.noneOf(GraphPropertyKey.class) : null;
    }

    @Override
    public PropertyContainer copy() {
        return new PropertyContainer(new HashMap<>(props), diagnostics);
    }

    @Override
    public void setProperty(final GraphPropertyKey key, final Object value) {
        props.put(key, value);
    }

    @Override
    public Object getProperty(final GraphPropertyKey key) {
        if (props.containsKey(key)) {
            if (diagnostics) {
                used.add(key);
            }
            return props.get(key);
        }
        throw new RuntimeException("No such property " + key);
    }

    @Deprecated
    @Override
    public Map<GraphPropertyKey, Object> getAllProperties() {
        return ImmutableMap.copyOf(props);
    }

    @Override
    public boolean hasProperty(final GraphPropertyKey key) {
        if (diagnostics) {
            used.add(key);
        }
        return props.containsKey(key);
    }

    @Override
    public void removeProperty(final GraphPropertyKey key) {
        props.remove(key);
    }

    @Override
    public Set<GraphPropertyKey> getUnused() {
        if (props.isEmpty()) {
            return GraphPropertyKey.EmptySet;
        }
        if (used==null) {
            return GraphPropertyKey.EmptySet;
        } else {
            final EnumSet<GraphPropertyKey> results = EnumSet.copyOf(props.keySet());
            results.removeAll(used);
            return results;
        }
    }

    @Override
    public String toString() {
        return "PropertyContainer{" +
                "props=" + props +
                ", used=" + used +
                ", diagnostics=" + diagnostics +
                '}';
    }
}
