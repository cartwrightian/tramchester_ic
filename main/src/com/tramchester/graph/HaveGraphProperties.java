package com.tramchester.graph;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Map;

import static com.tramchester.graph.GraphPropertyKey.*;

public class HaveGraphProperties {
    private static final Logger logger = LoggerFactory.getLogger(HaveGraphProperties.class);

    protected <C extends GraphProperty & CoreDomain & HasId<C>>  void set(final C domainItem, final Entity entity) {
        entity.setProperty(domainItem.getProp().getText(), domainItem.getId().getGraphId());
    }

    protected <C extends CoreDomain> IdFor<C> getIdFor(final Class<C> klass, final Entity entity) {
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
            String msg = String.format("Failed to get property %s for element id %s properties %s", key, entity.getElementId(),
                    entity.getAllProperties());
            logger.error(msg);
            throw new RuntimeException(msg, notFound);
        }
    }

    @SuppressWarnings("unchecked")
    private static <C extends CoreDomain> IdFor<C> getIdForRouteStation(String value) {
        return (IdFor<C>) RouteStationId.parse(value);
    }

    protected IdFor<RouteStation> getRouteStationId(final Entity entity) {
        String value = entity.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    protected Map<String, Object> getAllProperties(final Entity entity) {
        return entity.getAllProperties();
    }

    protected void setTime(final TramTime tramTime, final Entity entity) {
        entity.setProperty(TIME.getText(), tramTime.asLocalTime());
        if (tramTime.isNextDay()) {
            entity.setProperty(DAY_OFFSET.getText(), tramTime.isNextDay());
        }
    }

    protected TramTime getTime(final Entity entity) {
        LocalTime localTime = (LocalTime) entity.getProperty(TIME.getText());
        boolean nextDay = entity.hasProperty(DAY_OFFSET.getText());
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }


    protected Object getProperty(final GraphPropertyKey graphPropertyKey, final Entity entity) {
        return entity.getProperty(graphPropertyKey.getText());
    }

}
