package com.tramchester.testSupport.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.deployment.cli.BaseCLI;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.metrics.Timing;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.EnumSet;

public class PerformanceTestCLI extends BaseCLI {

    private final Duration transactionTimeout;
    private final Scope scope;

    enum Scope {
        All,
        Interchanges,
        RouteEnds
    }

    public static void main(String[] args)  {
        Logger logger = LoggerFactory.getLogger(PerformanceTestCLI.class);

        if (args.length != 3) {
            throw new RuntimeException("Expected 2 arguments: <config file> <scope> <transaction timeout minutes>");
        }
        final Path configFile = Paths.get(args[0]).toAbsolutePath();
        Scope scope = Scope.valueOf(args[1]);
        final int transactionTimeout = Integer.parseInt(args[2]);
        logger.info("Config from: " + configFile +  " scope: " + scope + " txn timeout: " + transactionTimeout);

        try {
            PerformanceTestCLI performanceTestCLI = new PerformanceTestCLI(Duration.ofMinutes(transactionTimeout), scope);
            performanceTestCLI.run(configFile, logger, "BuildGraphCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed",e);
            System.exit(-1);
        }
    }

    PerformanceTestCLI(final Duration transactionTimeout, final Scope scope) {
        super();
        this.transactionTimeout = transactionTimeout;
        this.scope = scope;
    }

    @Override
    public boolean run(Logger logger, GuiceContainerDependencies componentContainer, TramchesterConfig config) {
        final ProvidesLocalNow providesLocalNow = componentContainer.get(ProvidesLocalNow.class);

        final TramDate date = providesLocalNow.getTramDate();

        final RouteCalculationCombinations<Station> combinations = new RouteCalculationCombinations<>(componentContainer,
                RouteCalculationCombinations.checkStationOpen(componentContainer));

        boolean stop = false;
        while ( !stop )  {
            display("Begin test run, hit enter. ('N' or 'n' to stop)");
            final String response = getLine();

            stop = response==null || "n".equalsIgnoreCase(response);

            if (!stop) {
                try (Timing timing = new Timing(logger, "Journeys Between All Stations")) {
                    doCalculations(date, config, combinations);
                    timing.close();
                    display(timing.toString());
                } catch (Exception exception) {
                    logger.error("Failed", exception);
                    return false;
                }
            }
        }

        return true;
    }

    private void doCalculations(TramDate date,
                                TramchesterConfig config, RouteCalculationCombinations<Station> combinations) {

        final EnumSet<TransportMode> modes = config.getTransportModes();

        final LocationIdPairSet<Station> stationIdPairs = getPairs(date, modes, combinations);

        final TramTime time = TramTime.of(8, 5);

        final JourneyRequest.MaxNumberOfChanges maxChanges = JourneyRequest.MaxNumberOfChanges.of(2);

        final JourneyRequest journeyRequest = new JourneyRequest(date, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, modes);

        combinations.getJourneysFor(stationIdPairs, journeyRequest, transactionTimeout);
    }

    private LocationIdPairSet<Station> getPairs(TramDate date, EnumSet<TransportMode> modes,
                                                       RouteCalculationCombinations<Station> combinations) {
        final RouteCalculationCombinations.CreatePairs createPairs = combinations.getCreatePairs(date);
        return switch (scope) {
            case All -> createPairs.createStationPairsForAll(modes);
            case Interchanges -> createPairs.InterchangeToInterchange(modes);
            case RouteEnds -> createPairs.EndOfRoutesToEndOfRoutes(modes);
        };

    }
}
