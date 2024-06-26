package com.tramchester.geo;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;

public class BoundingBoxWithStations extends BoundingBox {

    private final LocationSet<Station> stationsWithin;

    public BoundingBoxWithStations(BoundingBox box, LocationSet<Station> stationsWithin) {
        super(box);
        this.stationsWithin = stationsWithin;
    }

    public boolean hasStations() {
        return !stationsWithin.isEmpty();
    }

    public LocationSet<Station> getStations() {
        return stationsWithin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BoundingBoxWithStations that = (BoundingBoxWithStations) o;

        return stationsWithin.equals(that.stationsWithin);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + stationsWithin.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BoundingBoxWithStations{" +
                "stationsWithin=" + HasId.asIds(stationsWithin) +
                "} " + super.toString();
    }
}
