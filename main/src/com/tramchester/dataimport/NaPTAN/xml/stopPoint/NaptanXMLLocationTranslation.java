package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Translation")
public class NaptanXMLLocationTranslation {

    private final String gridType;
    final private int easting;
    final private int northing;
    final private double latitude;
    private final double longitude;

    @JsonCreator
    public NaptanXMLLocationTranslation(@JsonProperty(value = "Easting", defaultValue = "0") Integer easting,
                                        @JsonProperty(value = "Northing", defaultValue = "0") Integer northing,
                                        @JsonProperty("Latitude") Double latitude,
                                        @JsonProperty("GridType") String gridType,
                                        @JsonProperty("Longitude") Double longitude) {
        this.easting = easting;
        this.northing = northing;
        this.latitude = latitude;
        this.gridType = gridType;
        this.longitude = longitude;
    }

    public GridPosition getGridPosition() {
        if (gridType==null || "UKOS".equals(gridType)) {
            if (easting==0 || northing==0) {
                return GridPosition.Invalid;
            }
            return new GridPosition(easting, northing);
        }
        return GridPosition.Invalid;
    }

    public LatLong getLatLong() {
        if (latitude==0 || longitude==0) {
            return LatLong.Invalid;
        }
        return new LatLong(latitude, longitude);
    }

    @Override
    public String toString() {
        return "NaptanXMLLocationTranslation{" +
                "easting=" + easting +
                ", northing=" + northing +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

}
