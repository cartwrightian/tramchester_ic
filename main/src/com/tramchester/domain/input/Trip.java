package com.tramchester.domain.input;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

public interface Trip extends HasId<Trip>, HasTransportMode, GraphProperty, CoreDomain {

    static IdFor<Trip> createId(final String text) {
        return StringIdFor.createId(text, Trip.class);
    }

    static IdFor<Trip> InvalidId() {
        return new InvalidId<>(Trip.class);
    }

    IdFor<Trip> getId();

    StopCalls getStopCalls();

    Service getService();

    String getHeadsign();

    Route getRoute();

    TransportMode getTransportMode();

    GraphPropertyKey getProp();

    boolean isFiltered();

    boolean intoNextDay();

    TramTime departTime();

    TramTime arrivalTime();

    boolean hasStops();

    boolean callsAt(IdFor<Station> station);

    boolean callsAt(InterchangeStation interchangeStation);

    boolean serviceOperatesOn(TramDate date);

    IdFor<Station> firstStation();

    IdFor<Station> lastStation();

    boolean isAfter(IdFor<Station> first, IdFor<Station> second);
}
