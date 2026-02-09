package com.tramchester.graph.core.inMemory;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.*;

final class PropertyContainer implements GraphEntityProperties.GraphProps<PropertyContainer> {

    private final ConcurrentMap<GraphPropertyKey, Object> props;

    PropertyContainer() {
        this(new ConcurrentHashMap<>());
    }

    public PropertyContainer(final List<PropertyDTO> properties) {
        this();
        properties.forEach(prop -> setProperty(GraphPropertyKey.parse(prop.getKey()), prop.getContainedValue()));
    }

    private PropertyContainer(ConcurrentHashMap<GraphPropertyKey, Object> props) {
        this.props = props;
    }

    @Override
    public PropertyContainer copy() {
        return new PropertyContainer(new ConcurrentHashMap<>(props));
    }

    @Override
    public void setProperty(final GraphPropertyKey key, final Object value) {
        props.put(key, value);
    }

    @Override
    public Object getProperty(final GraphPropertyKey key) {
        if (props.containsKey(key)) {
            return props.get(key);
        }
        throw new RuntimeException("No such property " + key);
    }

    @Deprecated
    @Override
    public Map<String, Object> getAllProperties() {
        // TODO Update i/f to GraphPropertyKey
        return props.entrySet().
                stream().
                collect(Collectors.toMap(entry -> entry.getKey().getText(), Map.Entry::getValue));
    }

    @Override
    public boolean hasProperty(final GraphPropertyKey key) {
        return props.containsKey(key);
    }

    @Override
    public void removeProperty(final GraphPropertyKey key) {
        props.remove(key);
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
    public void setTransportMode(TransportMode transportMode) {
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

        final EnumSet<TransportMode> updated = EnumSet.copyOf(current);
        updated.add(mode);
        setProperty(TRANSPORT_MODES, updated);
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        if (hasProperty(TRANSPORT_MODES)) {
            Set<TransportMode> theSet = (Set<TransportMode>) getProperty(TRANSPORT_MODES);
            return EnumSet.copyOf(theSet);
        } else {
            return EnumSet.noneOf(TransportMode.class);
        }
    }


}
