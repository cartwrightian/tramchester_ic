package com.tramchester.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.time.TramDuration;

@JsonDeserialize( as = TemporaryStationsWalkIdsConfig.class)
public interface TemporaryStationsWalkIds {

    DateRange getDateRange();
    StationIdPair getStationPair();

    static boolean areEqual(TemporaryStationsWalkIds a, TemporaryStationsWalkIds b) {
        return a.getDateRange().equals(b.getDateRange()) && a.getStationPair().equals(b.getStationPair());
    }

    boolean hasCostOverride();

    TramDuration getCostOverride();

    static String asString(TemporaryStationsWalkIds config) {
        return "TemporaryStationsWalkIds{" +
                "stationPair=" + config.getStationPair() +
                ", range=" + config.getDateRange();
    }
}
