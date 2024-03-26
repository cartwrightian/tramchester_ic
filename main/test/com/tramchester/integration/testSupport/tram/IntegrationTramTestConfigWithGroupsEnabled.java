package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class IntegrationTramTestConfigWithGroupsEnabled extends IntegrationTramTestConfigWithNaptan  {
    private final TFGMGTFSSourceTestConfig overideTFGMTestConfig;

    public IntegrationTramTestConfigWithGroupsEnabled() {
        super(EnumSet.of(TransportMode.Tram));

        final List<StationClosures> closedStations = Collections.emptyList();
        final Set<TransportMode> groupStationModes = Collections.singleton(TransportMode.Tram);

        overideTFGMTestConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                TransportMode.Tram, AdditionalTramInterchanges.stations(), groupStationModes, closedStations,
                IntegrationTramTestConfig.MAX_INITIAL_WAIT);
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(overideTFGMTestConfig);
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder().resolve("_tram_groups");
    }
}
