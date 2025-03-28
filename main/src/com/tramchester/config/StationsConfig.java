package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StationListConfig.class, name = "List"),
        @JsonSubTypes.Type(value = StationPairConfig.class, name = "Pair")
})
public interface StationsConfig {

    //abstract IdSet<Station> getStations();


    static IdSet<Station> getStationsFrom(final StationsConfig stationsConfig) {
        if (stationsConfig instanceof StationPairConfig stationPairConfig) {
            return IdSet.from(stationPairConfig.getStationPair());
        } else if (stationsConfig instanceof StationListConfig stationListConfig) {
            return stationListConfig.getStations();
        } else {
            throw new RuntimeException("Unknown type of StationClosures " + stationsConfig.getClass() + " " + stationsConfig);
        }
    }
}
