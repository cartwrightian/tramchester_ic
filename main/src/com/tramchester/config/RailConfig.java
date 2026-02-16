package com.tramchester.config;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

public interface RailConfig extends HasDataPath, TransportDataSourceConfig {
    ImmutableEnumSet<TransportMode> getModes();
}
