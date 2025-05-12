package com.tramchester.testSupport.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.deployment.cli.BaseCLI;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.graph.RouteCalculatorAllTramJourneysTest;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;

public class PerformanceTestCLI extends BaseCLI {

    public static void main(String[] args)  {
        Logger logger = LoggerFactory.getLogger(PerformanceTestCLI.class);

        if (args.length != 1) {
            throw new RuntimeException("Expected 1 arguments: <config file>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();
        logger.info("Config from " + configFile);

        try {
            PerformanceTestCLI performanceTestCLI = new PerformanceTestCLI();
            performanceTestCLI.run(configFile, logger, "BuildGraphCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed",e);
            System.exit(-1);
        }
    }

    public PerformanceTestCLI() {
        super();
    }

    @Override
    public boolean run(Logger logger, GuiceContainerDependencies componentContainer, TramchesterConfig config) {
        final StationRepository stationRepository = componentContainer.get(StationRepository.class);
        final InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        final ProvidesLocalNow providesLocalNow = componentContainer.get(ProvidesLocalNow.class);

        final TramDate date = providesLocalNow.getTramDate();

        final RouteCalculationCombinations<Station> combinations = new RouteCalculationCombinations<>(componentContainer,
                RouteCalculationCombinations.checkStationOpen(componentContainer));

        boolean stop = false;
        while ( !stop )  {
            display("Begin test run, hit enter. ('N' or 'n' to stop)");
            final String response = getLine();

            stop = response==null || "N".equals(response) || "n".equals(response);

            if (!stop) {
                try (Timing ignored = new Timing(logger, "Journeys Between All Stations")) {
                    doCalculations(stationRepository, interchangeRepository, date, config, combinations);
                } catch (Exception exception) {
                    logger.error("Failed", exception);
                    return false;
                }
            }
        }

        return true;
    }

    private void doCalculations(StationRepository stationRepository, InterchangeRepository interchangeRepository, TramDate date,
                                TramchesterConfig config, RouteCalculationCombinations<Station> combinations) {

        LocationIdPairSet<Station> stationIdPairs = RouteCalculatorAllTramJourneysTest.
                createStationPairs(stationRepository, interchangeRepository, date);

        final TramTime time = TramTime.of(8, 5);

        final JourneyRequest.MaxNumberOfChanges maxChanges = JourneyRequest.MaxNumberOfChanges.of(2);

        final JourneyRequest journeyRequest = new JourneyRequest(date, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, TramsOnly);

        combinations.getJourneysFor(stationIdPairs, journeyRequest);

    }
}
