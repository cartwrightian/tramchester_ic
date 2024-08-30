package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import io.dropwizard.core.Configuration;

import java.util.Objects;
import java.util.Set;

// config example
/*
    stationClosures:
        - stations: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
        dateRange:
            begin: 2023-07-15
            end: 2023-09-20
        timeRange:
            begin: 00:15
            end: 10:35
        fullyClosed: true
        diversionsAroundClosure: []
        diversionsToFromClosure: []
 */
// NOTE: final two optional, will be auto populated with nearby stations in ClosedStationRepository if not provided in config

public class StationClosuresConfig extends Configuration implements StationClosures {

    private final Set<String> stationsText;
    private final DateRangeConfig dateRangeConfig;
    private final TimeRangeConfig timeRangeConfig;
    private final Boolean fullyClosed;
    private final Set<String> diversionsAroundClosure;
    private final Set<String> diversionsToFromClosure;

    public StationClosuresConfig(@JsonProperty(value = "stations", required = true) Set<String> stationsText,
                                 @JsonProperty(value="dateRange", required = true) DateRangeConfig dateRangeConfig,
                                 @JsonProperty(value="timeRange", required = false) TimeRangeConfig timeRangeConfig,
                                 @JsonProperty(value = "fullyClosed", required = true) Boolean fullyClosed,
                                 @JsonProperty(value = "diversionsAroundClosure", required = false) Set<String> diversionsAroundClosure,
                                 @JsonProperty(value = "diversionsToFromClosure", required = false) Set<String> diversionsToFromClosure)  {
        this.stationsText = stationsText;
        this.dateRangeConfig = dateRangeConfig;
        this.timeRangeConfig = timeRangeConfig;
        this.fullyClosed = fullyClosed;
        this.diversionsAroundClosure = diversionsAroundClosure;
        this.diversionsToFromClosure = diversionsToFromClosure;
        if (!validDates()) {
            throw new RuntimeException("Invalid dates " + this);
        }
    }

    private boolean validDates() {
        return dateRangeConfig.isValid();
    }


    @Override
    public IdSet<Station> getStations() {
        return stationsText.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @JsonIgnore
    @Override
    public DateRange getDateRange() {
        return dateRangeConfig.getRange();
    }

    @Override
    public boolean hasTimeRange() {
        return timeRangeConfig!=null;
    }

    @JsonIgnore
    @Override
    public boolean hasDiversionsAroundClosure() {
        return diversionsAroundClosure!=null;
    }

    @Override
    public IdSet<Station> getDiversionsAroundClosure() {
        if (diversionsAroundClosure==null) {
            throw new RuntimeException("Not set for diversionsAroundClosure");
        }
        return diversionsAroundClosure.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @JsonIgnore
    @Override
    public boolean hasDiversionsToFromClosure() {
        return diversionsToFromClosure!=null;
    }

    @Override
    public String toString() {
        return "StationClosuresConfig{" +
                "stationsText=" + stationsText +
                ", dateRangeConfig=" + dateRangeConfig +
                ", fullyClosed=" + fullyClosed +
                ", diversionsAroundClosure=" + diversionsAroundClosure +
                ", diversionsToFromClosure=" + diversionsToFromClosure +
                '}';
    }

    @Override
    public IdSet<Station> getDiversionsToFromClosure() {
        if (diversionsToFromClosure==null) {
            throw new RuntimeException("Not set for diversionsToFromClosure");
        }
        return diversionsToFromClosure.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public TimeRange getTimeRange() {
        return timeRangeConfig.getRange();
    }

    @Override
    public boolean equals(Object o) {
        return StationClosures.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationsText, dateRangeConfig, fullyClosed, diversionsAroundClosure, diversionsToFromClosure);
    }
}
