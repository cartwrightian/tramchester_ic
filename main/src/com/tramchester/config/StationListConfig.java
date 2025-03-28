package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import jakarta.validation.Valid;

import java.util.Objects;
import java.util.Set;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class StationListConfig implements StationsConfig {

    private final Set<String> stationsIdsText;

    public StationListConfig(@JsonProperty(value = "ids", required = true) Set<String> stationsIdsText) {
        this.stationsIdsText = stationsIdsText;
    }

    public IdSet<Station> getStations() {
        return stationsIdsText.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public String toString() {
        return "StationListConfig{" +
                "stationsIdsText=" + stationsIdsText +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StationListConfig that)) return false;
        return Objects.equals(stationsIdsText, that.stationsIdsText);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stationsIdsText);
    }
}
