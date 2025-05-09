package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.places.Station;
import jakarta.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class StationPairConfig implements StationsConfig {
    private final String first;
    private final String second;

    public StationPairConfig(@JsonProperty(value = "first", required = true) String first,
                             @JsonProperty(value = "second", required = true) String second) {
        this.first = first;
        this.second = second;
    }

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    @JsonIgnore
    public StationIdPair getStationPair() {
        return new StationIdPair(Station.createId(first), Station.createId(second));
    }

    @Override
    public String toString() {
        return "StationPairConfig{" +
                "first='" + first + '\'' +
                ", second='" + second + '\'' +
                '}';
    }

}
