package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import io.dropwizard.configuration.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;

public class TidyOldDistDataCLI extends BaseCLI {

    private final String s3Preifx;

    public TidyOldDistDataCLI(String s3Preifx) {
        super();

        this.s3Preifx = s3Preifx;
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(TidyOldDistDataCLI.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 1 arguments: <config filename>");
        }
        final Path configFile = Paths.get(args[0]).toAbsolutePath();
        final String s3Preifx = args[1];

        logger.info("Config from " + configFile + " s3 prefix: " + s3Preifx);

        TidyOldDistDataCLI tidyOldDistDataCLI = new TidyOldDistDataCLI(s3Preifx);

        try {
            if (!tidyOldDistDataCLI.run(configFile, logger, "TidyOldDistDataCLI")) {
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
        final ProvidesLocalNow providesLocalNow = dependencies.get(ProvidesLocalNow.class);

        final ZonedDateTime cutoff = providesLocalNow.getZoneDateTimeUTC().minusYears(1);

        logger.info("Cutoff is " + cutoff);

        final ClientForS3 clientForS3 = dependencies.get(ClientForS3.class);

        final List<S3ObjectWithModTime> matchingKeys = findMatchingKeys(logger, config, clientForS3, cutoff);

        if (matchingKeys.isEmpty()) {
            logger.warn("No matching keys found for " + cutoff);
            return true;
        }

        matchingKeys.stream().map(item -> item.s3Object().key() + " mod: " + item.modTime() + System.lineSeparator()).
                forEachOrdered(logger::info);

        boolean proceed = shouldProceed(matchingKeys);

        final boolean success;
        if (proceed) {
            logger.warn("Deleting keys");
            List<S3Object> toDelete = matchingKeys.stream().map(S3ObjectWithModTime::s3Object).toList();
            success = clientForS3.deleteKeys(config.getDistributionBucket(), toDelete);
            if (success) {
                display("Keys deleted");
            } else {
                display("Error during delete, check logs");
            }

        } else {
            display("Skipped");
            logger.warn("Skipped");
            success = true;
        }

        return success;
    }

    private @NotNull List<S3ObjectWithModTime> findMatchingKeys(Logger logger, TramchesterConfig config, ClientForS3 clientForS3, ZonedDateTime cutoff) {
        List<S3Object> items = clientForS3.getSummaryForPrefix(config.getDistributionBucket(), s3Preifx).toList();

        logger.info("Found " + items.size() + " items for prefix " + s3Preifx);

        List<S3ObjectWithModTime> matchingKeys = items.stream().
                map(item -> new S3ObjectWithModTime(item, getModTime(item))).
                filter(item -> item.modTime().isBefore(cutoff)).
                sorted((a,b) -> a.modTime().compareTo(b.modTime)).
                toList();

        logger.warn("Found " + matchingKeys.size() + " items from before cutoff of " + cutoff);
        return matchingKeys;
    }

    private boolean shouldProceed(List<S3ObjectWithModTime> old) {
        display("Delete " + old.size() + " items between " + old.getFirst().modTime()
            + " and " + old.getLast().modTime);
        display("Type Y or y to continue");

        String consent = getLine();

        return "Y".equals(consent) || "y".equals(consent);
    }

    private String getLine() {
        final Console console = System.console();
        if (console==null) {
            InputStreamReader reader = new InputStreamReader(System.in);
            BufferedReader bufferedReader = new BufferedReader(reader);
            try {
                return bufferedReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return console.readLine();
        }
    }

    private void display(String text) {
        final Console console = System.console();
        if (console ==null) {
            System.out.println(text);
        } else {
            console.writer().println(text);
        }
    }

    private ZonedDateTime getModTime(final S3Object response) {
        final Instant lastModified = response.lastModified();
        return ZonedDateTime.ofInstant(lastModified, UTC);
    }

    private record S3ObjectWithModTime(S3Object s3Object, ZonedDateTime modTime) {
    }
}
