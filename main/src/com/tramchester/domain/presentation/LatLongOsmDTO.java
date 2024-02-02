package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;


// Note: enforce need to switch coordinate systems when passing to front-end
// EPSG:3857

@JsonTypeName("LatLong")
@JsonIgnoreProperties(value = "valid", allowGetters = true)
public class LatLongOsmDTO {
    private final double lat; // north/south
    private final double lon; // east/west

    private static final double MAX_METERS = 20037508.34D;

    public static final LatLongOsmDTO Invalid = new LatLongOsmDTO((MAX_METERS*2),(MAX_METERS*2));

    public LatLongOsmDTO(@JsonProperty(required = true, value = "lat") double lat,
                         @JsonProperty(required = true, value = "lon") double lon) {
        this.lat = lat;
        this.lon = lon;
    }

//    public LatLongOsmDTO(final DirectPosition directPositionLatLong) {
//        this(directPositionLatLong.getOrdinate(0), directPositionLatLong.getOrdinate(1));
//    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public boolean isValid() {
        return ((lat>=-MAX_METERS) && (lat<=MAX_METERS) && (lon>=-MAX_METERS) && (lon<=MAX_METERS));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatLongOsmDTO that = (LatLongOsmDTO) o;
        return Double.compare(lat, that.lat) == 0 && Double.compare(lon, that.lon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }

    @Override
    public String toString() {
        return "LatLongOsmDTO{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
