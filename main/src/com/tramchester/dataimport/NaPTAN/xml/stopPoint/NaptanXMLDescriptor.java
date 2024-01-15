package com.tramchester.dataimport.NaPTAN.xml.stopPoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("Descriptor")
public class NaptanXMLDescriptor {

    private final String indicator;
    private final String commonName;
    private final String street;

    @JsonCreator
    public NaptanXMLDescriptor(@JsonProperty("Indicator") String indicator,
                               @JsonProperty("CommonName") String commonName,
                               @JsonProperty("Street") String street) {
        this.indicator = indicator;
        this.commonName = commonName;
        this.street = street;
    }

    public String getIndicator() {
        if (indicator==null) {
            return "";
        }
        return indicator;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getStreet() {
        if (street==null) {
            return "";
        }
        return street;
    }

    @Override
    public String toString() {
        return "NaptanXMLDescriptor{" +
                "indicator='" + indicator + '\'' +
                ", commonName='" + commonName + '\'' +
                ", street='" + street + '\'' +
                '}';
    }

}
