package com.tramchester;

import ch.qos.logback.classic.LoggerContext;
import com.tramchester.cloud.SignalToCloudformationReady;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;


public class LifeCycleHandler implements LifeCycle.Listener {
    private static final Logger logger = LoggerFactory.getLogger(LifeCycleHandler.class);

    private final GuiceContainerDependencies dependencies;
    private final ScheduledExecutorService executor;

    public LifeCycleHandler(GuiceContainerDependencies dependencies, ScheduledExecutorService executor) {
        this.dependencies = dependencies;
        this.executor = executor;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        logger.info("Dropwizard starting");
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        logger.info("Dropwizard started");

        logger.info("Prepare to signal cloud formation if running in cloud");
        if (dependencies!=null) {
            final SignalToCloudformationReady signaller = dependencies.get(SignalToCloudformationReady.class);
            signaller.send();
        } else {
            logger.error("Dependencies null, did start up fail?");
        }
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        logger.error("Dropwizard failure", cause);
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        logger.warn("Dropwizard stopping");
        logger.info("Shutdown dependencies");
        if (dependencies!=null) {
            dependencies.close();
        } else {
            logger.error("Dependencies null, did start up fail?");
        }
        logger.info("Stop scheduled tasks");
        executor.shutdown();
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        logger.info("Attempt flush of logs. Bye.");
        // attempt to flush logs, messages are being lost when exception is uncaught
        final ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if(factory instanceof LoggerContext ctx) {
            ctx.stop();
        }

        logger.info("Dropwizard stopped");
    }
}
