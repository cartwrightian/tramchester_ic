package com.tramchester.integration.testSupport.tram;


import com.tramchester.resources.APIResource;
import com.tramchester.resources.GraphDatabaseDependencyMarker;

import java.nio.file.Path;

public class ResourceTramTestConfig<T extends APIResource>  extends IntegrationTramTestConfig {
    private final boolean planningEnabled;

    public ResourceTramTestConfig(Class<T> resourceClass) {
        planningEnabled =  GraphDatabaseDependencyMarker.class.isAssignableFrom(resourceClass);
    }

    public ResourceTramTestConfig(Class<T> resourceClass, LiveData liveDataEnabled) {
        super(liveDataEnabled);
        planningEnabled = GraphDatabaseDependencyMarker.class.isAssignableFrom(resourceClass);
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
