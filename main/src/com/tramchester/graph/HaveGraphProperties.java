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

import java.time.LocalTime;
import java.util.Map;

import static com.tramchester.graph.GraphPropertyKey.*;

public class HaveGraphProperties {

    protected <C extends CoreDomain> IdFor<C> getIdFor(Class<C> klass, Entity entity) {
        if (RouteStation.class.equals(klass)) {
            throw new RuntimeException("Use getRouteStationId() for route station id");
        }
        GraphPropertyKey key = GraphPropertyKey.getFor(klass);
        String value =  entity.getProperty(key.getText()).toString();
        return StringIdFor.createId(value, klass);
    }

    protected IdFor<RouteStation> getRouteStationId(Entity entity) {
        String value = entity.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    protected <C extends GraphProperty & CoreDomain & HasId<C>>  void set(C domainItem, Entity entity) {
        entity.setProperty(domainItem.getProp().getText(), domainItem.getId().getGraphId());
    }

    protected Map<String, Object> getAllProperties(Entity entity) {
        return entity.getAllProperties();
    }

    protected void setTime(TramTime tramTime, Entity entity) {
        entity.setProperty(TIME.getText(), tramTime.asLocalTime());
        if (tramTime.isNextDay()) {
            entity.setProperty(DAY_OFFSET.getText(), tramTime.isNextDay());
        }
    }

    protected TramTime getTime(Entity entity) {
        LocalTime localTime = (LocalTime) entity.getProperty(TIME.getText());
        boolean nextDay = entity.hasProperty(DAY_OFFSET.getText());
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }


}
