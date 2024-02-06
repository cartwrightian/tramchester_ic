package com.tramchester.dataimport.nptg.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Translation")
public class NptgTranslation {
    private final String easting;
    private final String northing;
    private final String latitude;
    private final String longitude;

    public NptgTranslation(@JsonProperty("Easting") String easting,
                           @JsonProperty("Northing") String northing,
                           @JsonProperty("Latitude") String latitude,
                           @JsonProperty("Longitude") String longitude) {
        this.easting = easting;
        this.northing = northing;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getEasting() {
        return easting;
    }

    public String getNorthing() {
        return northing;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
