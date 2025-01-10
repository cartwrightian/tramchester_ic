package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import io.dropwizard.core.Configuration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

// https://www.raildeliverygroup.com/our-services/rail-data/timetable-data.html
// https://data.atoc.org/how-to

public class RailAppConfig extends Configuration implements RailConfig {

    private final Path dataPath;
    private final EnumSet<TransportMode> modes;
    private final int initialWaitMinutes;

    public RailAppConfig(@JsonProperty(value ="dataPath", required = true) Path dataPath,
                         @JsonProperty(value="modes", required = true) Set<TransportMode> modes,
                         @JsonProperty(value="initialWaitMinutes", required = true) int initialWaitMinutes) {
        this.dataPath = dataPath;
        this.modes = EnumSet.copyOf(modes);

        this.initialWaitMinutes = initialWaitMinutes;
    }

    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public boolean getOnlyMarkedInterchanges() {
        return true;
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.openRailData;
    }

    @Override
    public Duration getMaxInitialWait() {
        return Duration.ofMinutes(initialWaitMinutes);
    }

    @Override
    public EnumSet<TransportMode> getModes() {
        return modes;
    }

}
