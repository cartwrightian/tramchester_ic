package com.tramchester.integration.testSupport.rail;


import com.tramchester.resources.APIResource;
import com.tramchester.resources.GraphDatabaseDependencyMarker;

public class ResourceRailTestConfig<T extends APIResource>  extends IntegrationRailTestConfig {
    private final boolean planningEnabled;

    public ResourceRailTestConfig(Class<T> resourceClass, Scope national) {
        super(national);
        planningEnabled =  GraphDatabaseDependencyMarker.class.isAssignableFrom(resourceClass);
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }
}
