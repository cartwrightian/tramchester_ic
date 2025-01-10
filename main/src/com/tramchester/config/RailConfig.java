package com.tramchester.config;

import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;

public interface RailConfig extends HasDataPath, TransportDataSourceConfig {
    EnumSet<TransportMode> getModes();
}
