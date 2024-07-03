package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;

import java.util.List;

public class IntegrationTramStationWalksTestConfig extends IntegrationTramTestConfig {

    public IntegrationTramStationWalksTestConfig(List<TemporaryStationsWalkIds> walks) {
        super(LiveData.Disabled, IntegrationTestConfig.CurrentClosures, walks, Caching.Disabled);
    }

}
