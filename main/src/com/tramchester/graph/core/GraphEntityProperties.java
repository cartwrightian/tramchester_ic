package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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

        void setProperty(String key, Object value);

        Object getProperty(String text);

        Map<String, Object> getAllProperties();

        boolean hasProperty(String key);

        void removeProperty(String key);

        void setTime(TramTime tramTime);

        TramTime getTime();
    }

    public static class PropertyDTO {

        @JsonProperty
        private final String key;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = String.class, name = "string"),
                @JsonSubTypes.Type(value = Double.class, name = "double"),
                @JsonSubTypes.Type(value = TramTime.class, name = "TramTime")
        })
        private final Object value;

        public PropertyDTO(
                @JsonProperty("key") final String key,
                @JsonProperty("value") final Object value) {
            this.key = key;
            this.value = value;
        }

        public PropertyDTO(Map.Entry<String, Object> entry) {
            this(entry.getKey(), entry.getValue());
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }

}
