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

import java.io.*;
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

    protected String getLine() {
        final Console console = System.console();
        if (console==null) {
            return readFromSystemIn();
        } else {
            return console.readLine();
        }
    }

    private static @NotNull String readFromSystemIn() {
        InputStream in = System.in;
        if (in==null) {
            throw new RuntimeException("System.in is null");
        }

        try {
            final InputStreamReader reader = new InputStreamReader(in);
            final BufferedReader bufferedReader = new BufferedReader(reader);

            while (!bufferedReader.ready()) {
                Thread.sleep(1000); // not great, but this is just for testing....
            }
            return bufferedReader.readLine().trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void display(final String text) {
        final Console console = System.console();
        if (console ==null) {
            System.out.println("out:" + text);
        } else {
            console.writer().println("console:" + text);
        }
    }

    private static class NoOpCacheMetrics implements CacheMetrics.RegistersCacheMetrics {
        @Override
        public <T> void register(String metricName, Gauge<T> Gauge) {
            // noop
        }
    }
}
