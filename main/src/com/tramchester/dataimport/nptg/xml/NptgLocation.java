package com.tramchester.dataimport.nptg.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Location")
public class NptgLocation {
    private final NptgTranslation translation;

    public NptgLocation(@JsonProperty("Translation") NptgTranslation translation) {
        this.translation = translation;
    }

    public NptgTranslation getTranslation() {
        return translation;
    }
}
