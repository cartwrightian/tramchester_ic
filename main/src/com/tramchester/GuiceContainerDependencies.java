package com.tramchester;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.tramchester.healthchecks.RegistersHealthchecks;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.resources.APIResource;
import jakarta.ws.rs.Path;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GuiceContainerDependencies implements ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContainerDependencies.class);

    private Set<ClosesResource> closeCallbacks;
    private final Reflections reflections;

    private Injector injector;

    public GuiceContainerDependencies(final List<AbstractModule> moduleList) {
        reflections = new Reflections(App.class.getPackageName());
        injector = LifecycleInjector.builder().
                withModules(moduleList).build().
                createInjector();
        closeCallbacks = new HashSet<>();
    }

    public void initialise() {
        logger.info("initialise");

        if (logger.isDebugEnabled()) {
            logger.warn("Debug logging is enabled, server performance will be impacted");
        }

        logger.info("Start components");
        final LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        try {
            manager.start();
        } catch (Exception e) {
            logger.error("Failed to start", e);
            throw new RuntimeException("Failed to start", e);
        }

        if (manager.hasStarted()) {
            logger.info("Lifecycle manager has started");
        } else {
            logger.error("Lifecycle manager not started");
        }

        logger.info("Done");
    }

    public Set<Class<? extends APIResource>> getResources() {
        final Set<Class<? extends APIResource>> apiResources = reflections.getSubTypesOf(APIResource.class);
        final Set<Class<?>> havePath = reflections.getTypesAnnotatedWith(Path.class);

        final Set<Class<? extends APIResource>> pathMissing = apiResources.stream().
                filter(apiType -> !havePath.contains(apiType)).collect(Collectors.toSet());
        if (!pathMissing.isEmpty()) {
            final String msg = "The following API resources lack a path: " + pathMissing;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final Set<Class<?>> pathNotAPIResource = havePath.stream().
                filter(hasPathType -> !apiResources.contains(hasPathType)).collect(Collectors.toSet());
        if (!pathNotAPIResource.isEmpty()) {
            final String msg = "The following Path annotated classes don't implement APIResource: " + pathNotAPIResource;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return apiResources;
    }

    public void registerHealthchecksInto(final HealthCheckRegistry healthChecks) {
        logger.info("Register healthchecks");
        final RegistersHealthchecks instance = get(RegistersHealthchecks.class);
        instance.registerAllInto(healthChecks);
    }

    synchronized public void close() {
        logger.info("Callbacks");
        closeCallbacks.forEach(closesResource -> {
            try {
                closesResource.close();
            } catch(Exception exception) {
                logger.warn("Exception while closing " + closesResource, exception);
            }
        });
        closeCallbacks.clear();

        logger.info("Dependencies close");

        if (injector==null) {
            logger.info("Already closed");
            return;
        }

        logger.info("Begin cache stats");
        CacheMetrics cacheMetrics = get(CacheMetrics.class);
        cacheMetrics.report();
        logger.info("End cache stats");

        logger.info("Stop components");
        stop();

        logger.info("Dependencies closed");
        System.gc(); // for tests which accumulate/free a lot of memory
    }

    @Override
    public void registerCallbackFor(ClosesResource closesResource) {
        closeCallbacks.add(closesResource);
    }

    protected void stop() {
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        if (manager==null) {
            logger.error("Unable to get lifecycle manager for close()");
        } else {
            logger.info("Services Manager close");
            manager.close();
        }

        // NOTE: Setting to null required since the test framework continues to hold a reference to this class and in
        // turn the injector holds a ref to all singletons, with no way to clear those
        injector = null;
    }

    public <C> C get(Class<C> klass) {
        return injector.getInstance(klass);
    }

}
