package com.tramchester.graph.core.neo4j;

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

import java.time.LocalTime;
import java.util.Map;

import static com.tramchester.graph.GraphPropertyKey.*;

public class HaveGraphProperties<E extends HaveGraphProperties.KeyValueProps> {
    private static final Logger logger = LoggerFactory.getLogger(HaveGraphProperties.class);

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
            String msg = String.format("Failed to get property %s for element id %s properties %s", key, entity.getId(),
                    entity.getAllProperties());
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

    protected Map<String, Object> getAllProperties(final E entity) {
        return entity.getAllProperties();
    }

    protected void setTime(final TramTime tramTime, final E entity) {
        entity.setProperty(TIME.getText(), tramTime.asLocalTime());
        if (tramTime.isNextDay()) {
            entity.setProperty(DAY_OFFSET.getText(), tramTime.isNextDay());
        }
    }

    protected TramTime getTime(final E entity) {
        final LocalTime localTime = (LocalTime) entity.getProperty(TIME.getText());
        final boolean nextDay = entity.hasProperty(DAY_OFFSET.getText());
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }


    protected Object getProperty(final GraphPropertyKey graphPropertyKey, final E entity) {
        return entity.getProperty(graphPropertyKey.getText());
    }

    interface KeyValueProps {

        void setProperty(String key, Object value);

        Object getProperty(String text);

        String getId();

        Map<String, Object> getAllProperties();

        boolean hasProperty(String key);
    }

}
