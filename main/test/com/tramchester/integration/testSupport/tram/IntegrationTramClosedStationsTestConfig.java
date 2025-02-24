package com.tramchester.integration.testSupport.tram;

import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.StationClosures;

import java.util.List;

public class IntegrationTramClosedStationsTestConfig extends IntegrationTramTestConfig {

    private final boolean planningEnabled;

    public IntegrationTramClosedStationsTestConfig(final List<StationClosures> closures, boolean planningEnabled,
                                                   List<TemporaryStationsWalkIds> currentStationWalks) {
        super(closures, Caching.Disabled, currentStationWalks);
        this.planningEnabled = planningEnabled;
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

}
