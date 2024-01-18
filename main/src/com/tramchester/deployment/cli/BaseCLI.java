package com.tramchester.deployment.cli;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.StandaloneConfigLoader;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.metrics.CacheMetrics;
import io.dropwizard.configuration.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public abstract class BaseCLI {
    @NotNull
    protected GuiceContainerDependencies bootstrap(Path configFile, String name) throws IOException, ConfigurationException {
        TramchesterConfig configuration = StandaloneConfigLoader.LoadConfigFromFile(configFile);
        configuration.getLoggingFactory().configure(new MetricRegistry(), name);

        GuiceContainerDependencies container = new ComponentsBuilder().create(configuration, new NoOpCacheMetrics());
        container.initialise();
        return container;
    }

    protected boolean run(Path configFile, Logger logger, String name) throws ConfigurationException, IOException {
        GuiceContainerDependencies container = bootstrap(configFile, name);
        final TramchesterConfig config = container.get(TramchesterConfig.class);
        boolean success = run(logger, container, config);
        container.close();
        return success;
    }

    public abstract  boolean run(Logger logger,  GuiceContainerDependencies dependencies, TramchesterConfig config);

    private static class NoOpCacheMetrics implements CacheMetrics.RegistersCacheMetrics {
        @Override
        public <T> void register(String metricName, Gauge<T> Gauge) {
            // noop
        }
    }
}
