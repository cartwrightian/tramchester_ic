package com.tramchester.integration.testSupport.bus;


import com.tramchester.resources.APIResource;
import com.tramchester.resources.GraphDatabaseDependencyMarker;

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

}
