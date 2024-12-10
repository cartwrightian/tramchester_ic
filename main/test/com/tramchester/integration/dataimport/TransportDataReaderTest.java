package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.loader.ChecksForTripId;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.dataimport.loader.TransportDataSource;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransportDataReaderTest {
    private static ComponentContainer componentContainer;
    private TransportDataReader transportDataReader;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig tramchesterConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void setUp() {
        TransportDataReaderFactory transportDataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> all = transportDataReaderFactory.getReaders();
        transportDataReader = all.getFirst();
    }

    @Test
    void shouldHaveTFGMSourceForTramConfig() {
        assertEquals(DataSourceID.tfgm, transportDataReader.getDataSourceInfo().getID());
    }

    @Test
    void shouldHaveExpectedStopTimes() {
        Stream<StopTimeData> stopTimesData = transportDataReader.getStopTimes();
        TripRepository tripRepo = componentContainer.get(TripRepository.class);

        TripChecker tripChecker = new TripChecker(tripRepo);

        Instant start = Instant.now();

        Set<StopTimeData> stopTimes = stopTimesData.
                filter(StopTimeData::isValid).
                filter(stopTimeData -> tripChecker.hasId(stopTimeData.getTripId())).
                collect(Collectors.toSet());

        Duration first = Duration.between(start, Instant.now());

        start = Instant.now();

        final TransportDataFromCSVFile.ReaderFactory readerFactory = new TransportDataSource.StopTimeFilteredReader(tripChecker);

        Set<StopTimeData> fromSpike = transportDataReader.getStopTimes(readerFactory).
                filter(StopTimeData::isValid).
                collect(Collectors.toSet());

        Duration second = Duration.between(start, Instant.now());

        assertEquals(stopTimes.size(), fromSpike.size());

        assertTrue(second.toMillis()<first.toMillis(), first + " " + second);

    }

    private static class TripChecker implements ChecksForTripId {

        private final Set<String> tripIds;

        public TripChecker(TripRepository tripRepository) {
            tripIds = tripRepository.getTrips().stream().
                    map(Trip::getId).
                    map(IdFor::getGraphId).
                    collect(Collectors.toSet());
        }

        @Override
        public boolean hasId(String tripId) {
            return tripIds.contains(tripId);
        }
    }

}
