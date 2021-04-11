package com.tramchester.integration.testSupport.train;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

public class RailTestDataSourceConfig implements GTFSSourceConfig {
    private final String dataPath;

    public RailTestDataSourceConfig(String dataPath) {
        this.dataPath = dataPath;
    }

    // https://planar.network/projects/feeds

    @Override
    public Path getDataPath() {
        return Paths.get(dataPath);
    }

    @Override
    public String getName() {
        return "rail";
    }

    @Override
    public boolean getHasFeedInfo() {
        return false;
    }

    @Override
    public Set<GTFSTransportationType> getTransportModes() {
        return Collections.singleton(GTFSTransportationType.train);
    }

    @Override
    public Set<TransportMode> getTransportModesWithPlatforms() {
        return Collections.emptySet();
    }

    @Override
    public Set<LocalDate> getNoServices() {
        return Collections.emptySet();
    }
}