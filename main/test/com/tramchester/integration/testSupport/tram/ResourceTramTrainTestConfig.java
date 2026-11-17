package com.tramchester.integration.testSupport.tram;

import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.resources.APIResource;
import com.tramchester.resources.GraphDatabaseDependencyMarker;

import java.nio.file.Path;

public class ResourceTramTrainTestConfig<T extends APIResource> extends RailAndTramGreaterManchesterConfig {
    private final boolean planningEnabled;

    public ResourceTramTrainTestConfig(Class<T> resourceClass) {
        planningEnabled =  GraphDatabaseDependencyMarker.class.isAssignableFrom(resourceClass);
    }

    public ResourceTramTrainTestConfig(Class<T> resourceClass, IntegrationTramTestConfig.LiveData liveDataEnabled) {
        this(liveDataEnabled, GraphDatabaseDependencyMarker.class.isAssignableFrom(resourceClass));
    }

    public ResourceTramTrainTestConfig(IntegrationTramTestConfig.LiveData liveData, boolean journeyPlanningEnabled) {
        super(liveData);
        this.planningEnabled = journeyPlanningEnabled;
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

    @Override
    public Path getCacheFolder() {
        // save to use same as super here, data set is the same
        return super.getCacheFolder();
    }

}
