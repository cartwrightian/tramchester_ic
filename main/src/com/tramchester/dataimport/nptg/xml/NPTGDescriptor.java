package com.tramchester.dataimport.nptg.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Place")
public class NPTGDescriptor {

    private final String localityName;

    public NPTGDescriptor(@JsonProperty("LocalityName") String localityName) {
        this.localityName = localityName;
    }

    public String getLocalityName() {
        return localityName;
    }
}
