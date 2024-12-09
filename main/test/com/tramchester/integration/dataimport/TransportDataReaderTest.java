package com.tramchester.integration.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.loader.ChecksForTripId;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.dataimport.loader.files.StringStreamReader;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
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
    private static TramchesterConfig tramchesterConfig;
    private TransportDataReader transportDataReader;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        tramchesterConfig = new IntegrationTramTestConfig();
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

        RemoteDataSourceConfig dataRemoteSourceConfig = tramchesterConfig.getDataRemoteSourceConfig(DataSourceID.tfgm);
        Path dataLoadLocation = dataRemoteSourceConfig.getDataPath();

        Path path = dataLoadLocation.resolve(TransportDataReader.InputFiles.stop_times.name()+".txt");
        CsvMapper mapper = CsvMapper.builder().
                addModule(new AfterburnerModule()).
                build();
        SpikeLoader loader = new SpikeLoader(path, mapper, tripChecker);

        start = Instant.now();

        Set<StopTimeData> fromSpike = loader.load().
                filter(StopTimeData::isValid).
                collect(Collectors.toSet());

        Duration second = Duration.between(start, Instant.now());

        assertEquals(stopTimes.size(), fromSpike.size());

        assertTrue(second.toMillis()<first.toMillis(), first + " " + second);

    }

    @Test
    void shouldJustLoad() {
        TripRepository tripRepo = componentContainer.get(TripRepository.class);
        TripChecker tripChecker = new TripChecker(tripRepo);

        RemoteDataSourceConfig dataRemoteSourceConfig = tramchesterConfig.getDataRemoteSourceConfig(DataSourceID.tfgm);
        Path dataLoadLocation = dataRemoteSourceConfig.getDataPath();

        Path path = dataLoadLocation.resolve(TransportDataReader.InputFiles.stop_times.name()+".txt");

        CsvMapper mapper = CsvMapper.builder().
                addModule(new AfterburnerModule()).
                build();

        SpikeLoader loader = new SpikeLoader(path, mapper, tripChecker);

        Set<StopTimeData> fromSpike = loader.load().
                filter(StopTimeData::isValid).
                collect(Collectors.toSet());

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

    private static class SpikeLoader extends TransportDataFromCSVFile<StopTimeData, StopTimeData> {

        private final ChecksForTripId checksForTripId;

        public SpikeLoader(Path filePath, CsvMapper mapper, ChecksForTripId checksForTripId) {
            super(filePath, StopTimeData.class, mapper);
            this.checksForTripId = checksForTripId;
        }

        @Override
        public Stream<StopTimeData> load() {
            try {
                final Reader reader = new FileReader(filePath.toString());
                final StringStreamReader stringStreamReader = createReaderFor(reader);
                return super.load(stringStreamReader);
            } catch (FileNotFoundException e) {
                String msg = "Unable to load from file " + filePath;
                //logger.error(msg, e);
                throw new RuntimeException(msg, e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private @NotNull StringStreamReader createReaderFor(final Reader reader) throws IOException {
            final BufferedReader bufferedReader = new BufferedReader(reader);

            final String header = bufferedReader.readLine();
            final int firstDelimit = header.indexOf(',');
            final String column = header.substring(0, firstDelimit);

            if (!"trip_id".equals(column)) {
                throw new RuntimeException("Mismatch on column " + column);
            }

            Stream<String> headerStream = Stream.of(header);

            Stream<String> filteredLines = bufferedReader.lines().filter(this::matches);

            return new StringStreamReader(Stream.concat(headerStream, filteredLines));
        }

        private boolean matches(final String line) {
            final int firstDelimit = line.indexOf(',');
            final String column = line.substring(0, firstDelimit);
            return checksForTripId.hasId(column);
        }
    }



}
