package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public class MonitorTramLiveData extends BaseCLI {

    private final int numberToReceive;

    public MonitorTramLiveData(int numberToReceive) {
        super();
        this.numberToReceive = numberToReceive;
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(MonitorTramLiveData.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 2 arguments: <config filename> <num messages>");
        }
        final Path configFile = Paths.get(args[0]).toAbsolutePath();
        final int numberToReceive = Integer.parseInt(args[1]);

        logger.info("Config from " + configFile);
        logger.info("Number of messages to receive " + numberToReceive);

        final MonitorTramLiveData cli = new MonitorTramLiveData(numberToReceive);

        try {
            if (!cli.run(configFile, logger, "MonitorTramLiveData")) {
                logger.error("Check logs, not successful");
                System.exit(-1);
            }

        } catch (ConfigurationException | IOException e) {
            logger.error("Exception", e);
            System.exit(-1);
        }
        logger.info("Success");
    }

    @Override
    public boolean run(Logger logger, GuiceContainerDependencies dependencies, TramchesterConfig config) {
        if (!config.liveTfgmTramDataEnabled()) {
            logger.error("Live data needs to be enabled in config");
            return false;
        }

        final TfgmTramLiveDataConfig liveConfig = config.getLiveDataConfig();
        final Long refreshSeconds = liveConfig.getRefreshPeriodSeconds();

        final LiveDataMarshaller marshaller = dependencies.get(LiveDataMarshaller.class);
        final LiveDataFetcher fetcher = dependencies.get(LiveDataFetcher.class);
        final CountDownLatch latch = new CountDownLatch(numberToReceive);

        marshaller.addSubscriber(update -> {
            latch.countDown();
            logger.info("Received " + update.size() + " updates");
            return true;
        });

        fetcher.subscribe(text -> logger.info("Received " + text.length() + " bytes of data"));

        logger.info("Refresh interval is " + refreshSeconds + " seconds");

        final TimerTask fetchDataTask = new TimerTask() {
            @Override
            public void run() {
                fetcher.fetch();
            }
        };

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(fetchDataTask, 0, refreshSeconds * 1000);

        try {
            latch.await();
            logger.info("finished normally");
        } catch (InterruptedException e) {
            logger.error("Failed to wait for latch", e);
        }

        logger.info("Finishing");
        timer.cancel();
        return true;
    }

}
