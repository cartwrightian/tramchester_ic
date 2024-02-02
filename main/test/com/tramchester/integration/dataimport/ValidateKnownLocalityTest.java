package com.tramchester.integration.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.dataimport.loader.files.TransportDataFromFileFactory;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocality.GreaterManchester;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateKnownLocalityTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationGroupsRepository stationGroupsRepository;
    private BoundingBox boundingBox;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        boundingBox = config.getBounds();
    }

    @Test
    void shouldLoadKnownLocalities() {
        Set<KnownLocality> missing = KnownLocality.GreaterManchester.stream().
                filter(place -> !stationGroupsRepository.hasGroup(place.getId())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), "missing " + missing);
    }

    @Test
    void shouldBeInBounds() {
        Set<StationGroup> outOfBounds = GreaterManchester.stream().
                map(place -> place.from(stationGroupsRepository)).
                filter(place -> !boundingBox.contained(place.getLatLong())).
                collect(Collectors.toSet());

        assertTrue(outOfBounds.isEmpty(), "out of bounds " + outOfBounds);

    }

    @Test
    void shouldHaveStationsWithinBounds() {
        Map<KnownLocality, IdSet<Station>> outOfBounds = new HashMap<>();
        for(KnownLocality knownLocality : KnownLocality.GreaterManchester) {
            StationGroup stationGroup = knownLocality.from(stationGroupsRepository);

            Set<Station> contained = stationGroup.getAllContained();
            IdSet<Station> outside = contained.stream().
                    filter(station -> !boundingBox.contained(station.getLatLong())).
                    collect(IdSet.collector());
            if (!outside.isEmpty()) {
                outOfBounds.put(knownLocality, outside);
            }
        }
        assertTrue(outOfBounds.isEmpty(), "Have out of bounds " + outOfBounds);
    }

    @Test
    void shouldHaveStationsInGroups() {

        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        GreaterManchester.forEach(knowLocality -> {
            StationGroup group = knowLocality.from(stationGroupsRepository);
            assertTrue(group.getAllContained().size()>1, "not enough stations for " + group);
        });
    }

    @Test
    void shouldHaveExpectedStationsLoadedForLocalities() {
        final NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);

        final TransportDataReader reader = getDataReader(DataSourceID.tfgm);

        // need to filter out stations that are REALLY missing from the source data, to avoid false positives
        final IdSet<Station> allIds = reader.getStops().
                map(stopData -> Station.createId(stopData.getCode())).
                collect(IdSet.idCollector());

        assertFalse(allIds.isEmpty(), "validate");

        Map<KnownLocality, IdSet<Station>> results = new HashMap<>();
        GreaterManchester.forEach(knowLocality -> {
            StationGroup group = knowLocality.from(stationGroupsRepository);
            Set<NaptanRecord> records = naptanRepository.getRecordsForLocality(group.getLocalityId());

            final IdSet<Station> fromGroup = group.getAllContained().stream().collect(IdSet.collector());

            final IdSet<Station> actoCodesFromNaptanAlsoInStops = records.stream().
                    map(NaptanRecord::getId).
                    map(id -> StringIdFor.convert(id, Station.class)).
                    filter(allIds::contains).
                    collect(IdSet.idCollector());

            assertFalse(actoCodesFromNaptanAlsoInStops.isEmpty(), "validate");

            final IdSet<Station> inNaptanOnly = actoCodesFromNaptanAlsoInStops.stream().
                    filter(code -> !fromGroup.contains(code)).
                    collect(IdSet.idCollector());

            if (!inNaptanOnly.isEmpty()) {
                results.put(knowLocality, inNaptanOnly);
            }
        });

        assertTrue(results.isEmpty(), "got mismatches " + results);
    }

    public TransportDataReader getDataReader(final DataSourceID dataSourceID) {

        final GetsFileModTime getsFileModTime = componentContainer.get(GetsFileModTime.class);

        final List<GTFSSourceConfig> configs = config.getGTFSDataSource();
        final Optional<GTFSSourceConfig> findConfig = configs.stream().filter(config -> config.getDataSourceId() == dataSourceID).findFirst();
        assertFalse(findConfig.isEmpty());

        final GTFSSourceConfig sourceConfig = findConfig.get();

        CsvMapper mapper = CsvMapper.builder().
                addModule(new BlackbirdModule()).
                build();

        final RemoteDataSourceConfig dataRemoteSourceConfig = config.getDataRemoteSourceConfig(dataSourceID);
        final Path dataLoadLocation = dataRemoteSourceConfig.getDataPath();

        final TransportDataFromFileFactory factory = new TransportDataFromFileFactory(dataLoadLocation, mapper);
        final TransportDataReaderFactory.GetModTimeFor getModTimeFor = new TransportDataReaderFactory.GetModTimeFor(dataRemoteSourceConfig, getsFileModTime);

        return  new TransportDataReader(factory, sourceConfig, getModTimeFor);
    }
}
