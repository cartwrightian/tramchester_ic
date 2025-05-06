package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

public class RouteStationId implements IdFor<RouteStation> {

    private static final String ROUTE_STATION_ID_DIVIDER = "_";

    private final IdFor<Route> routeId;
    private final IdFor<Station> stationId;

    @JsonCreator
    private RouteStationId(@JsonProperty("routeId")IdFor<Route> routeId,
                           @JsonProperty("stationId")IdFor<Station> stationId) {
        this.routeId = routeId;
        this.stationId = stationId;
    }

    public static RouteStationId createId(final IdFor<Route> routeId, final IdFor<Station> stationId) {
        return new RouteStationId(routeId, stationId);
    }

    public static RouteStationId invalid() {
        return createId(StringIdFor.invalid(Route.class), StringIdFor.invalid(Station.class));
    }

    public static RouteStationId parse(final String text) {
        final int indexOf = text.indexOf(ROUTE_STATION_ID_DIVIDER);
        if (indexOf<0) {
            return RouteStationId.invalid();
        }
        // todo rail route or normal route id?
        final IdFor<Route> routeId  = Route.parse(text.substring(0, indexOf));
        final IdFor<Station> stationId = Station.createId(text.substring(indexOf+1));
        return createId(routeId, stationId);
    }

    public IdFor<Route> getRouteId() {
        return routeId;
    }

    public IdFor<Station> getStationId() {
        return stationId;
    }

    @JsonIgnore
    @Override
    public String getGraphId() {
        return routeId.getGraphId()+ ROUTE_STATION_ID_DIVIDER +stationId.getGraphId();
    }

    @JsonIgnore
    @Override
    public boolean isValid() {
        return routeId.isValid() && stationId.isValid();
    }

    @JsonIgnore
    @Override
    public Class<RouteStation> getDomainType() {
        return RouteStation.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteStationId that = (RouteStationId) o;

        if (!routeId.equals(that.routeId)) return false;
        return stationId.equals(that.stationId);
    }

    @Override
    public int hashCode() {
        int result = routeId.hashCode();
        result = 31 * result + stationId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RouteStationId{" +
                "routeId=" + routeId +
                ", stationId=" + stationId +
                '}';
    }


}
