package com.tramchester.dataimport.nptg.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Translation")
public class NptgTranslation {
    private final String easting;
    private final String northing;

    public NptgTranslation(@JsonProperty("Easting") String easting,
                           @JsonProperty("Northing") String northing) {
        this.easting = easting;
        this.northing = northing;
    }

    public String getEasting() {
        return easting;
    }

    public String getNorthing() {
        return northing;
    }
}
