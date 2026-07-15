package com.tramchester.graph.core;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphEntityProperties<E extends GraphEntityProperties.GraphProps<E>> {

    protected <C extends GraphProperty & CoreDomain & HasId<C>>  void set(final C domainItem, final E entity) {
        entity.setProperty(domainItem.getProp(), domainItem.getId().getGraphId());
    }

    protected <C extends CoreDomain> IdFor<C> getIdFor(final Class<C> klass, final E entity) {
        final GraphPropertyKey key = GraphPropertyKey.getFor(klass);

            final String value = entity.getProperty(key).toString();
            if (RouteStation.class.equals(klass)) {
                return getIdForRouteStation(value);
            } else {
                return StringIdFor.createId(value, klass);
            }
    }

    @SuppressWarnings("unchecked")
    private static <C extends CoreDomain> IdFor<C> getIdForRouteStation(final String value) {
        return (IdFor<C>) RouteStationId.parse(value);
    }

    protected RouteStationId getRouteStationId(final E entity) {
        final String value = entity.getProperty(ROUTE_STATION_ID).toString();
        return RouteStationId.parse(value);
    }

    // public to support testing
    protected Object getProperty(final GraphPropertyKey graphPropertyKey, final E entity) {
        return entity.getProperty(graphPropertyKey);
    }

    protected Map<GraphPropertyKey, Object> getAllProperties(final E entity) {
        return entity.getAllProperties();
    }

    public interface GraphProps<IMPL extends GraphProps<IMPL>> {

        void setProperty(final GraphPropertyKey graphPropertyKey, final Object value);

        Object getProperty(final GraphPropertyKey graphPropertyKey);

        Map<GraphPropertyKey, Object> getAllProperties();

        boolean hasProperty(final GraphPropertyKey graphPropertyKey);

        void removeProperty(final GraphPropertyKey graphPropertyKey);

        IMPL copy();

        // supports optimization
        Set<GraphPropertyKey> getUnused();

        default void addTripId(final IdFor<Trip> tripId) {

            final TripIdSet existing = getTripIds();

            final TripIdSet updated;
            if (existing.isEmpty()) {
                updated = TripIdSet.Factory.singleton(tripId);
            } else {
                updated = TripIdSet.Factory.copyThenAppend(existing, tripId);
            }
            setProperty(TRIP_ID_LIST, updated);
        }

        default boolean hasTripIdInList(final IdFor<Trip> tripId) {
            return getTripIds().contains(tripId);
        }

        default TripIdSet getTripIds() {
            if (hasProperty(TRIP_ID_LIST)) {
                return (TripIdSet) getProperty(TRIP_ID_LIST);
            } else {
                return TripIdSet.emptySet();
            }
        }

        default void setTime(final TramTime tramTime) {
            setProperty(TIME, tramTime);
            // to allow backwards compatible comparison with props in Neo4J
            if (tramTime.isNextDay()) {
                setProperty(DAY_OFFSET, tramTime.isNextDay());
            }
        }

        default TramTime getTime() {
            return (TramTime) getProperty(TIME);
        }

        default TramDate getStartDate() {
            return (TramDate) getProperty(START_DATE);
        }

        default void setCost(TramDuration cost) {
            setProperty(COST, cost);
        }

        default TramDuration getCost() {
            if (hasProperty(COST)) {
                return (TramDuration) getProperty(COST);
            }
            throw new RuntimeException("Cost is missing for " + this);
        }

        default void setTransportMode(final TransportMode transportMode) {
            setProperty(TRANSPORT_MODE, transportMode);
        }

        default TransportMode getTransportMode() {
            return (TransportMode) getProperty(TRANSPORT_MODE);
        }

        default void addTransportMode(final TransportMode mode) {
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

        default ImmutableEnumSet<TransportMode> getTransportModes() {
            if (hasProperty(TRANSPORT_MODES)) {
                final EnumSet<TransportMode> theSet = (EnumSet<TransportMode>) getProperty(TRANSPORT_MODES);
                return ImmutableEnumSet.copyOf(theSet);
            } else {
                return TransportMode.noneOf();
                //return ImmutableEnumSet.noneOf(TransportMode.class);
            }
        }

        default int getHour() {
            if (hasProperty(HOUR)) {
                return (int) getProperty(HOUR);
            }
            throw new RuntimeException("Hour is missing for " + this);
        }
    }

}
