package com.tramchester.graph.core;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.GraphPropertyKey.ROUTE_STATION_ID;

public class GraphEntityProperties<E extends GraphEntityProperties.GraphProps> {
    private static final Logger logger = LoggerFactory.getLogger(GraphEntityProperties.class);

    protected <C extends GraphProperty & CoreDomain & HasId<C>>  void set(final C domainItem, final E entity) {
        entity.setProperty(domainItem.getProp().getText(), domainItem.getId().getGraphId());
    }

    protected <C extends CoreDomain> IdFor<C> getIdFor(final Class<C> klass, final E entity) {
        final GraphPropertyKey key = GraphPropertyKey.getFor(klass);

        try {
            final String value = entity.getProperty(key.getText()).toString();
            if (RouteStation.class.equals(klass)) {
                return getIdForRouteStation(value);
            } else {
                return StringIdFor.createId(value, klass);
            }
        }
        catch (org.neo4j.graphdb.NotFoundException notFound) {
            String msg = String.format("Failed to get property %s from properties %s", key, entity.getAllProperties());
            logger.error(msg);
            throw new RuntimeException(msg, notFound);
        }
    }

    @SuppressWarnings("unchecked")
    private static <C extends CoreDomain> IdFor<C> getIdForRouteStation(final String value) {
        return (IdFor<C>) RouteStationId.parse(value);
    }

    protected RouteStationId getRouteStationId(final E entity) {
        final String value = entity.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    // public to support testing
    protected Object getProperty(final GraphPropertyKey graphPropertyKey, final E entity) {
        return entity.getProperty(graphPropertyKey.getText());
    }

    protected Map<String, Object> getAllProperties(final E entity) {
        return entity.getAllProperties();
    }

    public interface GraphProps {

        // TODO String -> GraphPropertyKey here

        void setProperty(String key, Object value);

        default void setProperty(final GraphPropertyKey graphPropertyKey, final Object value) {
            setProperty(graphPropertyKey.getText(), value);
        }

        Object getProperty(String text);

        default Object getProperty(final GraphPropertyKey graphPropertyKey) {
            return getProperty(graphPropertyKey.getText());
        }

        Map<String, Object> getAllProperties();

        default boolean hasProperty(final GraphPropertyKey graphPropertyKey) {
            return hasProperty(graphPropertyKey.getText());
        }

        boolean hasProperty(String key);

        void removeProperty(String key);

        void setTime(TramTime tramTime);

        TramTime getTime();

        void addTripId(IdFor<Trip> tripId);

        boolean hasTripIdInList(IdFor<Trip> tripId);

        IdSet<Trip> getTripIds();

        void setCost(TramDuration cost);

        TramDuration getCost();

        void setTransportMode(TransportMode transportMode);

        TransportMode getTransportMode();

        void addTransportMode(TransportMode mode);

        EnumSet<TransportMode> getTransportModes();
    }

}
