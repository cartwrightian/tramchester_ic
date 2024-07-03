package com.tramchester.integration.testSupport.tram;

import com.tramchester.domain.StationClosures;

import java.util.List;

public class IntegrationTramClosedStationsTestConfig extends IntegrationTramTestConfig {

    private final boolean planningEnabled;

    public IntegrationTramClosedStationsTestConfig(List<StationClosures> closures, boolean planningEnabled) {
        super(closures, Caching.Disabled);
        this.planningEnabled = planningEnabled;
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

}
