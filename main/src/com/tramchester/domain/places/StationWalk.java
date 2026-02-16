package com.tramchester.domain.places;


import com.tramchester.domain.id.HasId;
import com.tramchester.domain.time.TramDuration;

public class StationWalk {
    private final Station station;
    private final TramDuration cost;

    public StationWalk(Station station, TramDuration cost) {
        this.cost = cost;
        this.station = station;
    }

    public TramDuration getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "StationWalk{" +
                "station=" + HasId.asId(station) +
                ", cost=" + cost +
                '}';
    }

    public Station getStation() {
        return station;
    }
}
