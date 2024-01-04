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

    final private Integer easting;
    final private Integer northing;
    final private Double latitude;
    private final String gridType;
    private final Double longitude;

    @JsonCreator
    public NaptanXMLLocationTranslation(@JsonProperty("Easting") Integer easting,
                                        @JsonProperty("Northing") Integer northing,
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
