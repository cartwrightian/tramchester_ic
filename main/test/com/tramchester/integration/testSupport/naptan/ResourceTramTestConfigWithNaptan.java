package com.tramchester.integration.testSupport.naptan;


import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.resources.APIResource;
import com.tramchester.resources.GraphDatabaseDependencyMarker;

import java.nio.file.Path;
import java.util.EnumSet;

public class ResourceTramTestConfigWithNaptan<T extends APIResource>  extends IntegrationTramTestConfigWithNaptan {
    private final boolean planningEnabled;

    public ResourceTramTestConfigWithNaptan(Class<T> resourceClass) {
        super(EnumSet.of(TransportMode.Tram));
        planningEnabled =  GraphDatabaseDependencyMarker.class.isAssignableFrom(resourceClass);
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }


    @Override
    public Path getCacheFolder() {
        return super.getCacheFolder();
    }
}
