package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.filters.ActiveGraphFilter;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.filters.IncludeAllFilter;
import com.tramchester.repository.TransportData;

public class GraphFilterModule extends AbstractModule {

    private final ComponentsBuilder.DeferredSetupGraphFilter overrideDefaultIncludeAllFilter;
    private final boolean filteredSupplied;

    public GraphFilterModule(ComponentsBuilder.DeferredSetupGraphFilter overrideDefaultIncludeAllFilter) {
        this.filteredSupplied = overrideDefaultIncludeAllFilter!=null;
        this.overrideDefaultIncludeAllFilter = overrideDefaultIncludeAllFilter;
    }

    @LazySingleton
    @Provides
    GraphFilterActive providesGraphFilterPresent(TramchesterConfig config) {
        guardForValidConfiguration(config);

        return new GraphFilterActive(overrideDefaultIncludeAllFilter != null);
    }

    @LazySingleton
    @Provides
    GraphFilter providesConfiguredGraphFilter(TransportData transportData, TramchesterConfig config) {
        guardForValidConfiguration(config);

        if (overrideDefaultIncludeAllFilter == null) {
            return new IncludeAllFilter();
        }

        ActiveGraphFilter activeGraphFilter = new ActiveGraphFilter();
        overrideDefaultIncludeAllFilter.configure(activeGraphFilter, transportData);
        return activeGraphFilter;
    }

    void guardForValidConfiguration(TramchesterConfig config) {
        final boolean filteredConfigured = config.isGraphFiltered();

        if (filteredSupplied && !filteredConfigured) {
            throw new RuntimeException("Filter was provided, but isGraphFiltered not set");
        }
        if (filteredConfigured && !filteredSupplied) {
            throw new RuntimeException("No filter provided, but isGraphFiltered set");
        }
    }
}
