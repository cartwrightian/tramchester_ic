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

    private final LatLong latLong;
    private final GridPosition gridPosition;

    @JsonCreator
    public NaptanXMLLocationTranslation(@JsonProperty(value = "Easting", defaultValue = "0") Integer easting,
                                        @JsonProperty(value = "Northing", defaultValue = "0") Integer northing,
                                        @JsonProperty("Latitude") Double latitude,
                                        @JsonProperty("GridType") String gridType,
                                        @JsonProperty("Longitude") Double longitude) {
        gridPosition = createGridPosition(gridType, easting, northing);
        latLong = createGridPosition(latitude, longitude);
    }

    private GridPosition createGridPosition(final String gridType, final int easting, final int northing) {
        if (gridType==null || "UKOS".equals(gridType)) {
            if (easting==0 || northing==0) {
                return GridPosition.Invalid;
            }
            return new GridPosition(easting, northing);
        }
        return GridPosition.Invalid;
    }

    private LatLong createGridPosition(final double latitude, final double longitude) {
        if (latitude==0 || longitude==0) {
            return LatLong.Invalid;
        }
        return new LatLong(latitude, longitude);
    }

    @Override
    public String toString() {
        return "NaptanXMLLocationTranslation{" +
                "latLong=" + latLong +
                ", gridPosition=" + gridPosition +
                '}';
    }

    public GridPosition getGridPosition() {
        return gridPosition;
    }

    public LatLong getLatLong() {
        return latLong;
    }
}
