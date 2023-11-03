package com.tramchester.config;

import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.util.EnumSet;

public interface RailConfig extends HasDataPath, TransportDataSourceConfig {
    Path getStations();
    Path getTimetable();
    EnumSet<TransportMode> getModes();
    String getVersion();
}
