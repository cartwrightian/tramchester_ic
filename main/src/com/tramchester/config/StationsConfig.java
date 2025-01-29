package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StationListConfig.class, name = "List"),
        @JsonSubTypes.Type(value = StationPairConfig.class, name = "Pair")
})
public abstract class StationsConfig {

    //abstract IdSet<Station> getStations();
}
