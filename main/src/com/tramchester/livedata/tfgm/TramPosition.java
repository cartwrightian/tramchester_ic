package com.tramchester.livedata.tfgm;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.util.Set;

public class TramPosition {
    private final StationPair stationPair;
    private final Set<UpcomingDeparture> trams;
    private final TramDuration cost;

    public TramPosition(StationPair stationPair, Set<UpcomingDeparture> trams, TramDuration cost) {
        this.stationPair = stationPair;
        this.trams = trams;
        this.cost = cost;
    }

    public Station getFirst() {
        return stationPair.getBegin();
    }

    public Station getSecond() {
        return stationPair.getEnd();
    }

    public Set<UpcomingDeparture> getTrams() {
        return trams;
    }

    public TramDuration getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "TramPosition{" +
                "stationPair=" + stationPair +
                ", trams=" + trams +
                ", cost=" + cost +
                '}';
    }

    public boolean hasTrams() {
        return !trams.isEmpty();
    }
}
