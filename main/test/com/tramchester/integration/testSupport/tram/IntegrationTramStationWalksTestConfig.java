package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.StationClosures;

import java.util.List;

public class IntegrationTramStationWalksTestConfig extends IntegrationTramTestConfig {

    public IntegrationTramStationWalksTestConfig(List<TemporaryStationsWalkIds> walks, List<StationClosures> currentClosures) {
        super(LiveData.Disabled, currentClosures, walks, Caching.Disabled);
    }

}
