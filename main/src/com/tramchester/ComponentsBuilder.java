package com.tramchester;

import com.google.inject.AbstractModule;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.modules.*;
import com.tramchester.dataimport.loader.TransportDataFactory;
import com.tramchester.dataimport.loader.PopulateTransportDataFromSources;
import com.tramchester.repository.TransportData;

import java.util.Arrays;
import java.util.List;

public class ComponentsBuilder {
    private Class<? extends TransportDataFactory> transportDataFactoryType;
    private SetupGraphFilter setupGraphFilter;

    public ComponentsBuilder() {
        this.transportDataFactoryType = PopulateTransportDataFromSources.class;
        setupGraphFilter = null;
    }

    public GuiceContainerDependencies create(TramchesterConfig config, CacheMetrics.RegistersCacheMetrics registerCacheMetrics) {

        List<AbstractModule> modules = Arrays.asList(
                new MappersAndConfigurationModule(config, registerCacheMetrics),
                new GetReadyModule(),
                new TransportDataFactoryModule<>(transportDataFactoryType),
                new GraphFilterModule(setupGraphFilter),
                new LiveDataModule(config));

        return new GuiceContainerDependencies(modules);
    }

    public <T extends TransportDataFactory>  ComponentsBuilder overrideProvider(Class<T> transportDataFactoryType) {
        this.transportDataFactoryType = transportDataFactoryType;
        return this;
    }

    public ComponentsBuilder configureGraphFilter(SetupGraphFilter setupGraphFilter) {
        this.setupGraphFilter = setupGraphFilter;
        return this;
    }

    public interface SetupGraphFilter {
        void configure(ConfigurableGraphFilter filterToConfigure, TransportData transportData);
    }
}