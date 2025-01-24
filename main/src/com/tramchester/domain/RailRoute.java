package com.tramchester.domain;

import com.tramchester.domain.places.Station;

import java.util.List;

public interface RailRoute extends Route {
    Station getBegin();
    Station getEnd();
    boolean callsAtInOrder(Station first, Station second);
    List<Station> getCallingPoints();
}
