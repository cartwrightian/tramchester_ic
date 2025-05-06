package com.tramchester.domain;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.*;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.CrossesDay;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Set;

public interface Route extends HasId<Route>, HasTransportMode, GraphProperty, CoreDomain, CrossesDay {

    IdFor<Route> getId();

    String getName();

    Set<Service> getServices();

    Agency getAgency();

    String getShortName();

    TransportMode getTransportMode();

    @Override
    GraphPropertyKey getProp();

    Trips getTrips();

    boolean isDateOverlap(Route otherRoute);

    DateRange getDateRange();

    boolean isAvailableOn(TramDate date);

    /***
     * May need to use RailRouteId or TramRouteId - see TransportEntityFactory implementations
     * @param text raw text id
     * @return A basic tram route id
     */
    static IdFor<Route> createBasicRouteId(String text) {
        return StringIdFor.createId(text, Route.class);
    }

    static IdFor<Route> parse(final String text) {
        if (TramRouteId.matches(text)) {
            return TramRouteId.parse(text);
        } else {
            return createBasicRouteId(text);
        }
    }

    IdSet<Station> getStartStations();

}
