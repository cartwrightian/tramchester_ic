package com.tramchester.graph.core.inMemory;

import com.google.common.collect.ImmutableMap;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.id.TripIdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphEntityProperties;

import java.util.*;

import static com.tramchester.graph.GraphPropertyKey.*;

final class PropertyContainer implements GraphEntityProperties.GraphProps<PropertyContainer> {

    //private static final int INITIAL_SIZE = 4;

    private final EnumMap<GraphPropertyKey, Object> props;
    private final EnumSet<GraphPropertyKey> used;
    private final boolean diagnostics;

    PropertyContainer(final boolean diagnostics) {
        this(new EnumMap<>(GraphPropertyKey.class), diagnostics);
    }

    public PropertyContainer(final List<PropertyDTO> properties) {
        this(false);
        properties.forEach(prop -> setProperty(GraphPropertyKey.parse(prop.getKey()), prop.getContainedValue()));
    }

    private PropertyContainer(final EnumMap<GraphPropertyKey, Object> props, final boolean diagnostics) {
        this.props = props;
        this.diagnostics = diagnostics;
        used = diagnostics ? EnumSet.noneOf(GraphPropertyKey.class) : null;
    }

    @Override
    public PropertyContainer copy() {
        return new PropertyContainer(new EnumMap<>(props), diagnostics);
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
    public void setTime(final TramTime tramTime) {
        setProperty(TIME, tramTime);
        // to allow backwards compatible comparison with props in Neo4J
        if (tramTime.isNextDay()) {
            setProperty(DAY_OFFSET, tramTime.isNextDay());
        }
    }

    @Override
    public TramTime getTime() {
        return (TramTime) getProperty(TIME);
    }

    @Override
    public void addTripId(final IdFor<Trip> tripId) {

        final TripIdSet existing = getTripGraphIds();

        final TripIdSet updated;
        if (existing.isEmpty()) {
            updated = TripIdSet.Factory.singleton(tripId);
        } else {
            updated = TripIdSet.Factory.copyThenAppend(existing,tripId);
        }
        setProperty(TRIP_ID_LIST, updated);
    }

    private TripIdSet getTripGraphIds() {
        if (hasProperty(TRIP_ID_LIST)) {
            return (TripIdSet) getProperty(TRIP_ID_LIST);
        } else {
            return TripIdSet.Factory.empty();
        }
    }

    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        if (hasProperty(TRIP_ID_LIST)) {
            return getTripGraphIds().contains(tripId);
        } else {
            return false;
        }
    }

    @Override
    public ImmutableIdSet<Trip> getTripIds() {
        if (hasProperty(TRIP_ID_LIST)) {
            return getTripGraphIds();
        } else {
            return IdSet.emptySet();
        }
    }

    @Override
    public void setCost(TramDuration cost) {
        setProperty(COST, cost);
    }

    @Override
    public TramDuration getCost() {
        if (hasProperty(COST)) {
            return (TramDuration) getProperty(COST);
        }
        throw new RuntimeException("Cost is missing for " + this);
    }

    @Override
    public int getHour() {
        if (hasProperty(HOUR)) {
            return (int) getProperty(HOUR);
        }
        throw new RuntimeException("Hour is missing for " + this);
    }

    @Override
    public void setTransportMode(final TransportMode transportMode) {
        setProperty(TRANSPORT_MODE, transportMode);
    }

    @Override
    public TransportMode getTransportMode() {
        return (TransportMode) getProperty(TRANSPORT_MODE);
    }

    @Override
    public void addTransportMode(final TransportMode mode) {
        final EnumSet<TransportMode> current;
        if (hasProperty(TRANSPORT_MODES)) {
            current = (EnumSet<TransportMode>) getProperty(TRANSPORT_MODES);
        } else {
            current = EnumSet.noneOf(TransportMode.class);
        }

        final EnumSet<TransportMode> replacement = EnumSet.copyOf(current);
        replacement.add(mode);

        setProperty(TRANSPORT_MODES, replacement);
    }

    @Override
    public ImmutableEnumSet<TransportMode> getTransportModes() {
        if (hasProperty(TRANSPORT_MODES)) {
            final Set<TransportMode> theSet = (Set<TransportMode>) getProperty(TRANSPORT_MODES);
            return ImmutableEnumSet.copyOf(theSet);
        } else {
            return ImmutableEnumSet.noneOf(TransportMode.class);
        }
    }


}
