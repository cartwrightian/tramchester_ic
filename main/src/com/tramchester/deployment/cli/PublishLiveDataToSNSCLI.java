package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataSNSPublisher;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public class PublishLiveDataToSNSCLI extends BaseCLI {

    private final int numberToSend;

    public PublishLiveDataToSNSCLI(int numberToSend) {
        super();
        this.numberToSend = numberToSend;
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(PublishLiveDataToSNSCLI.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 2 arguments: <config filename> <num msgs to send>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();

        int numberToSend = Integer.parseInt(args[1]);

        logger.info("Config from " + configFile);
        logger.info("Number of messages to send " + numberToSend);

        PublishLiveDataToSNSCLI cli = new PublishLiveDataToSNSCLI(numberToSend);

        try {
            if (!cli.run(configFile, logger, "UploadSourceDataCLI")) {
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
        String liveDataSNSTopic = config.getLiveDataSNSPublishTopic();
        if (liveDataSNSTopic.isEmpty()) {
            logger.error("No SNS topic in config (snsTopic)");
            return false;
        }

        logger.info("sns topic: " + liveDataSNSTopic);

        LiveDataSNSPublisher publisher = dependencies.get(LiveDataSNSPublisher.class);
        publisher.start();

        CountDownLatch latch = new CountDownLatch(numberToSend);

        LiveDataFetcher fetcher = dependencies.get(LiveDataFetcher.class);
        fetcher.subscribe(text -> {
            logger.info("Received " + text.length() + " bytes of data");
            latch.countDown();
        });

        TfgmTramLiveDataConfig liveConfig = config.getLiveDataConfig();
        Long refreshSeconds = liveConfig.getRefreshPeriodSeconds();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                fetcher.fetch();
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, refreshSeconds * 1000);

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Failed to wait for latch", e);
        }

        logger.info("Finishing");
        timer.cancel();
        return true;
    }

}
