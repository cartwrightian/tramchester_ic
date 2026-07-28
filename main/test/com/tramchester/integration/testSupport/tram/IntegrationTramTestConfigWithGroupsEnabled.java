package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.testSupport.AdditionalTramInterchanges;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class IntegrationTramTestConfigWithGroupsEnabled extends IntegrationTramTestConfigWithNaptan  {
    private final TFGMGTFSSourceTestConfig overrideTFGMTestConfig;

    public IntegrationTramTestConfigWithGroupsEnabled() {
        super(EnumSet.of(Tram));

        final Set<TransportMode> groupStationModes = Collections.singleton(Tram);

        overrideTFGMTestConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                Tram, AdditionalTramInterchanges.stations(),
                groupStationModes, CurrentClosures,
                IntegrationTramTestConfig.MAX_INITIAL_WAIT,
                Collections.emptyList());
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        return Collections.singletonList(overrideTFGMTestConfig);
    }

    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder().resolve("_tram_groups");
    }
}
