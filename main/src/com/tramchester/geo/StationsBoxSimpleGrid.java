package com.tramchester.geo;

import com.tramchester.domain.LocationSet;

import java.util.Objects;

public class StationsBoxSimpleGrid extends BoundingBoxWithStations {
    final int x;
    final int y;

    public StationsBoxSimpleGrid(int x, int y, BoundingBox box, LocationSet stationsWithin) {
        super(box, stationsWithin);
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StationsBoxSimpleGrid that = (StationsBoxSimpleGrid) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), x, y);
    }

    @Override
    public String toString() {
        return "StationsBoxSimpleGrid{" +
                "x=" + x +
                ", y=" + y +
                "} " + super.toString();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
