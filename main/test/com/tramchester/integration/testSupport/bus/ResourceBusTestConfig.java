package com.tramchester.integration.testSupport.bus;


import com.tramchester.resources.APIResource;
import com.tramchester.resources.GraphDatabaseDependencyMarker;

import java.nio.file.Path;

public class ResourceBusTestConfig<T extends APIResource>  extends IntegrationBusTestConfig {
    private final boolean planningEnabled;

    public ResourceBusTestConfig(Class<T> resourceClass) {
        super();
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
