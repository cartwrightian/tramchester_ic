package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RailConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;

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
    public Duration getMaxInitialWait() {
        return Duration.ofMinutes(INITIAL_WAIT_MINS);
    }

    @Override
        public Path getDataPath() {
            return Path.of("data/openRailData");
        }

//    @Override
//    public Path getStations(String version) {
//        return Path.of(remoteConfig.getFilePrefix(version) + ".MSN");
//    }
//
//    @Override
//    public Path getTimetable(String version) {
//        return Path.of(remoteConfig.getFilePrefix(version) + ".MCA");
//    }

    @Override
    public EnumSet<TransportMode> getModes() {
        return EnumSet.of(Train, RailReplacementBus);
    }

//    @Override
//    public String getVersion() {
//        return RailRemoteDataSourceConfig.VERSION;
//    }

}
