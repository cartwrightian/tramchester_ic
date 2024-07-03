package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;

import java.time.LocalDate;
import java.util.Objects;

public class TemporaryStationsWalkIdsConfig implements TemporaryStationsWalkIds {
    private final StationPairConfig stationPair;
    private final LocalDate begin;
    private final LocalDate end;

    public TemporaryStationsWalkIdsConfig(@JsonProperty(value = "stations", required = true) StationPairConfig stationPair,
                                          @JsonProperty(value = "begin", required = true) LocalDate begin,
                                          @JsonProperty(value = "end", required = true) LocalDate end) {
        this.stationPair = stationPair;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public DateRange getDateRange() {

        return DateRange.of(TramDate.of(begin), TramDate.of(end));
    }

    @Override
    public StationIdPair getStationPair() {
        return stationPair.getStations();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemporaryStationsWalkIdsConfig that = (TemporaryStationsWalkIdsConfig) o;
        return Objects.equals(getStationPair(), that.getStationPair()) && Objects.equals(begin, that.begin) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStationPair(), begin, end);
    }

    @Override
    public String toString() {
        return "TemporaryStationsWalkIdsConfig{" +
                "stationPair=" + stationPair +
                ", begin=" + begin +
                ", end=" + end +
                '}';
    }
}
