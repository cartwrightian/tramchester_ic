package com.tramchester.domain;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.CrossesDay;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.graph.GraphPropertyKey;

import java.time.DayOfWeek;
import java.util.EnumSet;
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

    Set<Trip> getTrips();

    boolean isDateOverlap(Route otherRoute);

    EnumSet<DayOfWeek> getOperatingDays();

    DateRange getDateRange();

    boolean isAvailableOn(TramDate date);

    static IdFor<Route> createId(String text) {
        return StringIdFor.createId(text, Route.class);
    }

    IdSet<Station> getStartStations();

}
