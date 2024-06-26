package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import io.dropwizard.core.Configuration;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

// config example
//    stationClosures:
//            - stations: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
//            begin: 2023-07-15
//            end: 2023-09-20
//            fullyClosed: true
//            diversionsOnly: []

public class StationClosuresConfig extends Configuration implements StationClosures {

    private final Set<String> stationsText;
    private final LocalDate begin;
    private final LocalDate end;
    private final Boolean fullyClosed;
    private final Set<String> diversionsOnly;

    public StationClosuresConfig(@JsonProperty(value = "stations", required = true) Set<String> stationsText,
                                 @JsonProperty(value = "begin", required = true) LocalDate begin,
                                 @JsonProperty(value = "end", required = true) LocalDate end,
                                 @JsonProperty(value = "fullyClosed", required = true) Boolean fullyClosed,
                                 @JsonProperty(value = "diversionsOnly", required = true) Set<String> diversionsOnly)  {
        this.stationsText = stationsText;
        this.begin = begin;
        this.end = end;
        this.fullyClosed = fullyClosed;
        this.diversionsOnly = diversionsOnly;
    }


    @Override
    public IdSet<Station> getStations() {
        //return IdSet.wrap(stationsText);
        return stationsText.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public TramDate getBegin() {
        return TramDate.of(begin);
    }

    @Override
    public TramDate getEnd() {
        return TramDate.of(end);
    }

    @Override
    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @JsonIgnore
    @Override
    public DateRange getDateRange() {
        return new DateRange(getBegin(), getEnd());
    }

    @Override
    public IdSet<Station> getDiversionsOnly() {
        return diversionsOnly.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public String toString() {
        return "StationClosuresConfig{" +
                "stationsText=" + stationsText +
                ", begin=" + begin +
                ", end=" + end +
                ", fullyClosed=" + fullyClosed +
                ", diversionsOnly=" + diversionsOnly +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationClosuresConfig that = (StationClosuresConfig) o;
        return Objects.equals(stationsText, that.stationsText) && Objects.equals(begin, that.begin) && Objects.equals(end, that.end) && Objects.equals(fullyClosed, that.fullyClosed) && Objects.equals(diversionsOnly, that.diversionsOnly);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationsText, begin, end, fullyClosed, diversionsOnly);
    }
}
