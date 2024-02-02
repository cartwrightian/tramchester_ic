package com.tramchester.domain.presentation;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.locationtech.jts.geom.Coordinate;

import java.util.Objects;

// TODO Stop passing this to front-end and use LatLongForOpenStreetMap

@JsonIgnoreProperties(value = "valid", allowGetters = true)
public class LatLong {

    private double lat; // north/south
    private double lon; // east/west

    public static final LatLong Invalid = new LatLong(-1000,-1000);

    // for json
    public LatLong() {

    }

    public LatLong(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public static LatLong of(Coordinate coordinate) {
        return new LatLong(coordinate.getX(), coordinate.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatLong latLong = (LatLong) o;
        return Double.compare(latLong.lat, lat) == 0 &&
                Double.compare(latLong.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    // for json
    public void setLat(double lat) {
        this.lat = lat;
    }

    // for json
    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "LatLong{" +
                "lat=" + lat +
                ", lon=" + lon +
                ", valid=" + isValid() +
                '}';
    }

    public boolean isValid() {
        return ((lat>=-90) && (lat<=90) && (lon>=-180) && (lon<=180));
    }

    @JsonIgnore
    public Coordinate getCoordinate() {
        return new Coordinate(lon, lat);
    }
}
