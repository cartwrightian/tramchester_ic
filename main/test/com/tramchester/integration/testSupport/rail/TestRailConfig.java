package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RailConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;

import java.nio.file.Path;

import static com.tramchester.domain.reference.TransportMode.RailReplacementBus;
import static com.tramchester.domain.reference.TransportMode.Train;

public class TestRailConfig implements RailConfig {

    public static final int INITIAL_WAIT_MINS = 60;

    @Override
    public boolean getOnlyMarkedInterchanges() {
            return true;
        }

    @Override
    public DataSourceID getDataSourceId() {
            return DataSourceID.openRailData;
        }

    @Override
    public TramDuration getMaxInitialWait() {
        return TramDuration.ofMinutes(INITIAL_WAIT_MINS);
    }

    @Override
    public Path getDataPath() {
            return Path.of("data/openRailData");
        }

    @Override
    public ImmutableEnumSet<TransportMode> getModes() {
        return ImmutableEnumSet.of(Train, RailReplacementBus);
    }

}
