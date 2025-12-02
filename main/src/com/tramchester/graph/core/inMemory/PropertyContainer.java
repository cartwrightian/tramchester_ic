package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphEntityProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.tramchester.graph.GraphPropertyKey.*;

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

    @Override
    public void addTripId(final IdFor<Trip> tripId) {
        final IdSet<Trip> existing = getTripIds();

        final IdSet<Trip> updated;
        if (existing.isEmpty()) {
            updated = IdSet.singleton(tripId);
        } else {
            updated = IdSet.copy(existing).add(tripId);
        }
        setProperty(TRIP_ID_LIST, updated);
    }

    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        return getTripIds().contains(tripId);
    }

    @Override
    public IdSet<Trip> getTripIds() {
        if (hasProperty(TRIP_ID_LIST)) {
            return (IdSet<Trip>) getProperty(TRIP_ID_LIST);
        } else {
            return IdSet.emptySet();
        }
    }

    @Override
    public void setCost(Duration cost) {
        setProperty(COST, cost);
    }

    @Override
    public Duration getCost() {
        if (hasProperty(COST)) {
            return (Duration) getProperty(COST);
        }
        throw new RuntimeException("Cost is missing for " + this);
    }
}
