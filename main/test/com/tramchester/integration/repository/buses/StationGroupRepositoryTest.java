package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class StationGroupRepositoryTest {
    private StationGroupsRepository stationGroupsRepository;
    private StationRepository stationRepository;

    private static ComponentContainer componentContainer;
    private Geography geography;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        geography = componentContainer.get(Geography.class);

    }

    @Test
    void shouldFindExpectedGroupStations() {
        final StationGroup groupedStations = stationGroupsRepository.findByName("Altrincham");
        assertNotNull(groupedStations);

        LocationSet<Station> contained = groupedStations.getAllContained();
        assertEquals(25, contained.size(), groupedStations.toString());

        assertEquals(LocationType.StationGroup, groupedStations.getLocationType());
        assertEquals(KnownLocality.Altrincham.getId(), groupedStations.getId());

        IdSet<Station> ids = contained.stream().collect(IdSet.collector());
        assertTrue(ids.contains(BusStations.StopAtAltrinchamInterchange.getId()));
    }

    @Test
    void shouldHaveParentGroupId() {
        StationGroup shudehill = stationGroupsRepository.getStationGroup(KnownLocality.Shudehill.getId());

        assertEquals(KnownLocality.ManchesterCityCentre.getId(), shudehill.getParentId());
    }

    @Test
    void allParentGroupsShouldBeValid() {
        Set<StationGroup> missingParent = stationGroupsRepository.getAllGroups().stream().
                filter(StationGroup::hasParent).
                filter(group -> !stationGroupsRepository.hasGroup(group.getParentId())).
                collect(Collectors.toSet());

        assertTrue(missingParent.isEmpty(), missingParent.toString());
    }

    @Disabled("illustrates threshold issue on number of stations. TODO")
    @Test
    void shouldHaveValidParentForKersalBar() {
        StationGroup kersalBar = stationGroupsRepository.findByName("Kersal Bar");
        assertNotNull(kersalBar);

        assertTrue(kersalBar.hasParent());

        assertTrue(stationGroupsRepository.hasGroup(kersalBar.getParentId()), "missing parent for " + kersalBar);
    }

    @Disabled("seems parent locality relationship in NPTG does not mean close together....")
    @Test
    void shouldHaveReasonableWalkingCostsBetweenChildParentGroups() {

        final int maxCostMins = 15;

        Set<StationGroup> tooLong = stationGroupsRepository.getAllGroups().
                stream().filter(StationGroup::hasParent).
                filter(group -> geography.getWalkingDuration(group, stationGroupsRepository.getStationGroup(group.getParentId())).toMinutes() > maxCostMins).
                collect(Collectors.toSet());

        assertEquals(10, tooLong.size(), HasId.asIds(tooLong));
    }

    @Test
    void shouldHaveReasonableDurationShudehillToParent() {
        StationGroup shudehill = stationGroupsRepository.getStationGroup(KnownLocality.Shudehill.getId());

        StationGroup parent = stationGroupsRepository.getStationGroup(shudehill.getParentId());

        Duration cost = geography.getWalkingDuration(shudehill, parent);

        assertTrue(cost.toMinutes()<6, "took too long " + cost);
    }

    @Disabled("seems parent locality relationship in NPTG does not mean close together....")
    @Test
    void shouldHaveReasonableCostAgecroftToParent() {
        StationGroup agecroft = stationGroupsRepository.getStationGroup(StationGroup.createId("E0028249"));

        StationGroup parent = stationGroupsRepository.getStationGroup(agecroft.getParentId());

        assertEquals(10L, geography.getWalkingDuration(agecroft, parent).toMinutes());
    }

    @Test
    void compareHaveNPTGLocations() {

        KnownLocality manchesterCityCentre = KnownLocality.ManchesterCityCentre;

        NPTGRepository nptgRepository = componentContainer.get(NPTGRepository.class);

        NPTGLocality locality = nptgRepository.get(manchesterCityCentre.getLocalityId());

        StationGroup group = stationGroupsRepository.getStationGroup(manchesterCityCentre.getId());

        assertEquals(locality.getLatLong(), group.getLatLong());
    }

    @Test
    void shouldHaveUniqueIds() {
        IdSet<Station> uniqueIds = new IdSet<>();

        stationRepository.getStationsServing(Bus).forEach(station -> {
            IdFor<Station> id = station.getId();
            assertFalse(uniqueIds.contains(id), "Not unique " + id);
            uniqueIds.add(id);
        });
    }

    @Test
    void shouldHaveValidStationsInGroupedStation() {
        Set<StationGroup> compositesFor = stationGroupsRepository.getStationGroupsFor(Bus);
        assertFalse(compositesFor.isEmpty());

        compositesFor.forEach(group -> {

            group.getAllContained().stream().forEach(station -> {
                IdFor<Station> id = station.getId();
                assertTrue(stationRepository.hasStationId(id), "could not find " + id + " for group " + group.getId());
            });
        });
    }


}
