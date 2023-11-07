package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.EnumSet;

public class RouteStation implements HasId<RouteStation>, GraphProperty, HasTransportModes, CoreDomain, HasGraphLabel {
    // A station that serves a specific route

    private final Station station;
    private final Route route;
    private final RouteStationId id;

    public RouteStation(final Station station, final Route route) {
        this.station = station;
        this.route = route;
        id = createId(station.getId(), route.getId());
    }

    public static RouteStationId createId(final IdFor<Station> station, final IdFor<Route> route) {
        return RouteStationId.createId(route, station);
    }

    public IdFor<RouteStation> getId() {
        return id;
    }

    @Override
    public String toString() {
        return "RouteStation{" +
                "stationId=" + station.getId() +
                ", routeId=" + route.getId() +
                '}';
    }

    public Route getRoute() {
        return route;
    }

    public IdFor<Station> getStationId() {
        return station.getId();
    }

    public Station getStation() {
        return station;
    }

    /***
     * The single transport mode, see also getTransportMode()
     * @return Singleton containing the transport mode
     */
    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return EnumSet.of(route.getTransportMode());
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.ROUTE_STATION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteStation that = (RouteStation) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean isActive() {
        return station.servesRouteDropOff(route) || station.servesRoutePickup(route);
    }

    @Override
    public GraphLabel getNodeLabel() {
        return GraphLabel.ROUTE_STATION;
    }
}
