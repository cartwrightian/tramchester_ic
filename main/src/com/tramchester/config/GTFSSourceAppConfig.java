package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = false)
@Valid
public class GTFSSourceAppConfig extends Configuration implements GTFSSourceConfig {

    final private String name;
    final private Boolean hasFeedInfo;
    final private Set<GTFSTransportationType> transportModes;
    final private Set<TransportMode> transportModesWithPlatforms;
    final private Set<LocalDate> noServices;   // date format: 2020-12-25
    final private Set<String> additionalInterchanges;
    final private Set<TransportMode> groupedStationModes;
    final private List<StationClosures> closures;
    final private Boolean addWalksForClosed;
    final private Boolean markedInterchangesOnly;
    final private Integer initialWaitMinutes;

    @JsonCreator
    public GTFSSourceAppConfig(@JsonProperty(value = "name", required = true) String name,
                               @JsonProperty(value = "hasFeedInfo", required = true) Boolean hasFeedInfo,
                               @JsonProperty(value = "transportModes", required = true) Set<GTFSTransportationType> transportModes,
                               @JsonProperty(value = "transportModesWithPlatforms", required = true) Set<TransportMode> transportModesWithPlatforms,
                               @JsonProperty(value = "noServices", required = true) Set<LocalDate> noServices,
                               @JsonProperty(value = "additionalInterchanges", required = true) Set<String> additionalInterchanges,
                               @JsonProperty(value = "groupedStationModes", required = true) Set<TransportMode> groupedStationModes,
                               @JsonProperty(value = "stationClosures", required = true) List<StationClosures> closures,
                               @JsonProperty(value = "addWalksForClosed", required = true) Boolean addWalksForClosed,
                               @JsonProperty(value = "markedInterchangesOnly", required = true) Boolean markedInterchangesOnly,
                               @JsonProperty(value = "initialWaitMinutes", required = true)Integer initialWaitMinutes) {
        this.name = name;
        this.hasFeedInfo = hasFeedInfo;
        this.transportModes = transportModes;
        this.transportModesWithPlatforms = transportModesWithPlatforms;
        this.noServices = noServices;
        this.additionalInterchanges = additionalInterchanges;
        this.groupedStationModes = groupedStationModes;
        this.closures = closures;
        this.addWalksForClosed = addWalksForClosed;
        this.markedInterchangesOnly = markedInterchangesOnly;
        this.initialWaitMinutes = initialWaitMinutes;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getHasFeedInfo() {
        return hasFeedInfo;
    }

    @Override
    public Set<GTFSTransportationType> getTransportGTFSModes() {
        return transportModes;
    }

    @Override
    public Set<TransportMode> getTransportModesWithPlatforms() {
        return transportModesWithPlatforms;
    }

    @Override
    public Set<LocalDate> getNoServices() {
        return noServices;
    }

    @Override
    public IdSet<Station> getAdditionalInterchanges() {
        return StringIdFor.createIds(additionalInterchanges, Station.class);
    }

    @Override
    public Set<TransportMode> groupedStationModes() {
        return groupedStationModes;
    }

    @Override
    public List<StationClosures> getStationClosures() {
        return closures;
    }

    @Override
    public boolean getAddWalksForClosed() {
        return addWalksForClosed;
    }

    @NotNull
    @Override
    public boolean getOnlyMarkedInterchanges() {
        return markedInterchangesOnly;
    }

    @Override
    public Duration getMaxInitialWait() {
        return Duration.ofMinutes(initialWaitMinutes);
    }
}
