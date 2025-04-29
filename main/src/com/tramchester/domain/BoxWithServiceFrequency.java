package com.tramchester.domain;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;

import java.util.Objects;
import java.util.Set;

public class BoxWithServiceFrequency extends BoundingBox {

    private final int numberOfStopCalls;
    private final Set<TransportMode> modes;
    private final Set<Station> stationsWithStopCalls;

    public BoxWithServiceFrequency(BoundingBox box, Set<Station> stationsWithStopCalls, int numberOfStopCalls, Set<TransportMode> modes) {
        super(box);
        this.stationsWithStopCalls = stationsWithStopCalls;
        this.numberOfStopCalls = numberOfStopCalls;
        this.modes = modes;
    }

    public int getNumberOfStopcalls() {
        return numberOfStopCalls;
    }

    public Set<Station> getStationsWithStopCalls() {
        return stationsWithStopCalls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BoxWithServiceFrequency that = (BoxWithServiceFrequency) o;

        if (numberOfStopCalls != that.numberOfStopCalls) return false;
        return Objects.equals(stationsWithStopCalls, that.stationsWithStopCalls);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + numberOfStopCalls;
        result = 31 * result + (stationsWithStopCalls != null ? stationsWithStopCalls.hashCode() : 0);
        return result;
    }

    public Set<TransportMode> getModes() {
        return modes;
    }
}
