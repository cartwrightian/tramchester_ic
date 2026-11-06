package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.InterchangeType;
import com.tramchester.domain.places.LinkedInterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.inMemory.FindLinkedStationsInMemory;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.Interchanges;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
class InterchangesRailAndTramTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;
    private RailStationIds InferredButNotMarked = RailStationIds.EastDidsbury;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldHaveExpectedInterchangesRail() {
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.ManchesterPiccadilly)));
        assertTrue(interchangeRepository.isInterchange(getStation(RailStationIds.Stockport)));
        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Hale)));

        // not gm
        //assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Knutsford)));

        assertFalse(interchangeRepository.isInterchange(getStation(RailStationIds.Mobberley)));
    }

    @Test
    void shouldHaveAdditionalTramInterchanges() {
        for (IdFor<Station> interchangeId : AdditionalTramInterchanges.stations()) {
            Station interchange = stationRepository.getStationById(interchangeId);
            assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
        }
    }

    @Test
    void shouldHaveExpectedConfig() {
        assertTrue(config.getRail().getOnlyMarkedInterchanges());
        Station station = getStation(RailStationIds.ManchesterPiccadilly);
        assertTrue(config.onlyMarkedInterchanges(station.getDataSourceID()));
    }

    @Test
    void shouldHaveCorrectBehavioursForPicc() {
        // not duplication with victoria as Picc usually added by config

        Station piccadillyRail = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        InterchangeStation trainInterchange = interchangeRepository.getInterchange(piccadillyRail);

        assertTrue(trainInterchange.isMultiMode());
        assertEquals(InterchangeType.NeighbourLinksMultimodal, trainInterchange.getType());
        assertTrue(trainInterchange.isAvailableSameMode(TransportMode.TrainOnly));

        Station piccadillyTram = TramStations.Piccadilly.from(stationRepository);

        assertTrue(interchangeRepository.isInterchange(piccadillyTram));

        InterchangeStation tramInterchange = interchangeRepository.getInterchange(piccadillyTram);

        assertEquals(InterchangeType.NeighbourLinksMultimodal, tramInterchange.getType());

        FindLinkedStations findStations = componentContainer.get(FindLinkedStationsInMemory.class);

        TramDate today = TramDate.of(LocalDate.now());
        DateRange timeRange = DateRange.of(today, today.plusWeeks(Interchanges.WEEKS_AHEAD_FOR_LINKS));
        IdSet<Station> overThreshhold = findStations.atLeastNLinkedStations(Tram, Interchanges.getLinkThreshhold(Tram), timeRange);
        boolean isAutomaticallyExchange = overThreshhold.contains(piccadillyTram.getId());

        // can be true iff Piccadilly is Id'ed automatically as an interchance
        assertEquals(isAutomaticallyExchange, tramInterchange.isAvailableSameMode(TransportMode.TramsOnly));

        assertTrue(tramInterchange.isMultiMode());
    }

    @Test
    void shouldHaveCorrectBehavioursForVic() {
        Station vicRail = RailStationIds.ManchesterVictoria.from(stationRepository);

        InterchangeStation trainInterchange = interchangeRepository.getInterchange(vicRail);

        assertTrue(trainInterchange.isMultiMode());
        assertEquals(InterchangeType.NeighbourLinksMultimodal, trainInterchange.getType());
        assertTrue(trainInterchange.isAvailableSameMode(TransportMode.TrainOnly));

        Station vicTram = TramStations.Victoria.from(stationRepository);

        InterchangeStation tramInterchange = interchangeRepository.getInterchange(vicTram);

        assertTrue(tramInterchange.isAvailableSameMode(TransportMode.TramsOnly));
        assertTrue(tramInterchange.isMultiMode());
        assertEquals(InterchangeType.NeighbourLinksMultimodal, tramInterchange.getType());
    }

    @Test
    void shouldHaveModesForNeighbourInterchangeTram() {
        Station tramStation = TramStations.NavigationRoad.from(stationRepository);
        Station railStation = RailStationIds.NavigationRaod.from(stationRepository);

        // check not being added as interchange via config or source data
        assertFalse(tramStation.isMarkedInterchange());
        assertTrue(interchangeRepository.isInterchange(tramStation));

        InterchangeStation tramInterchange = interchangeRepository.getInterchange(tramStation);

        assertFalse(tramInterchange.isAvailableSameMode(TransportMode.TramsOnly));
        assertFalse(tramInterchange.isAvailableSameMode(TransportMode.TrainOnly));

        assertEquals(InterchangeType.NeighbourLinksMultimodal, tramInterchange.getType());
        assertTrue(tramInterchange.isMultiMode(), tramInterchange.toString());

        IdSet<Route> tramPickups = getIds(tramStation.getPickupRoutes());
        IdSet<Route> trainPickups = getIds(railStation.getPickupRoutes());

        assertEquals(IdSet.union(trainPickups, tramPickups), getIds(tramInterchange.getPickupRoutes()));

        IdSet<Route> tramDropOffs = getIds(tramStation.getDropoffRoutes());
        IdSet<Route> trainDropOffs = getIds(railStation.getDropoffRoutes());

        assertEquals(IdSet.union(trainDropOffs, tramDropOffs), getIds(tramInterchange.getDropoffRoutes()));

        assertTrue(tramInterchange.getTransportModes().containsAll(EnumSet.of(Tram, Train)));
    }

    private IdSet<Route> getIds(Set<Route> pickupRoutes) {
        return pickupRoutes.stream().collect(IdSet.collector());
    }

    @Test
    void shouldCheckTrainStationsMarkedAsInterchange() {
        Set<Station> notMarkedInterchange = stationRepository.getStations(TrainOnly).stream().
                filter(station -> !station.isMarkedInterchange()).
                collect(Collectors.toSet());

        assertFalse(notMarkedInterchange.isEmpty());

        IdSet<Station> inferredOnly = notMarkedInterchange.stream().
                filter(notMarked -> interchangeRepository.isInterchange(notMarked)).
                collect(IdSet.collector());

        InferredButNotMarked = RailStationIds.EastDidsbury;
        assertTrue(inferredOnly.contains(InferredButNotMarked.getId()), "Not within " + inferredOnly);
    }

    @Test
    void shouldHaveExpectedModesForNeighbourInterchangeUnmarkedTrain() {

        Station train = InferredButNotMarked.from(stationRepository);
        assertTrue(interchangeRepository.isInterchange(train));

        LinkedInterchangeStation trainInterchange = (LinkedInterchangeStation) interchangeRepository.getInterchange(train);
        assertNotNull(trainInterchange);

        Station tram = TramStations.EastDidsbury.from(stationRepository);

        Set<Station> allLinked = trainInterchange.getLinked();
        assertEquals(1, allLinked.size());
        assertTrue(allLinked.contains(tram));

        // for train source dataset indicates if an interchange or not, not via config
        assertFalse(tram.isMarkedInterchange());
        assertFalse(train.isMarkedInterchange());

        assertFalse(trainInterchange.isAvailableSameMode(TransportMode.TramsOnly));
        assertFalse(trainInterchange.isAvailableSameMode(TransportMode.TrainOnly));

        assertEquals(InterchangeType.NeighbourLinksMultimodal, trainInterchange.getType());

        assertTrue(trainInterchange.isMultiMode());

        ImmutableIdSet<Route> expectedPickups = IdSet.union(getIds(train.getPickupRoutes()), getIds(tram.getPickupRoutes()));

        assertEquals(expectedPickups, getIds(trainInterchange.getPickupRoutes()));

        assertEquals(IdSet.union(getIds(train.getDropoffRoutes()), getIds(tram.getDropoffRoutes())),
                getIds(trainInterchange.getDropoffRoutes()));

    }

    @Test
    void sourceDataInterchangesShouldBeSingleModeAllowed() {
        Set<InterchangeStation> notAvailableTram = interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> interchangeStation.getType() == InterchangeType.FromSourceData).
                filter(interchangeStation -> interchangeStation.getTransportModes().contains(Tram)).
                filter(interchangeStation -> !interchangeStation.isAvailableSameMode(TramsOnly)).
                collect(Collectors.toSet());
        assertTrue(notAvailableTram.isEmpty(), "unexpected " + HasId.asIds(notAvailableTram));

        Set<InterchangeStation> notAvailableTrain = interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> interchangeStation.getType() == InterchangeType.FromSourceData).
                filter(interchangeStation -> !interchangeStation.isAvailableSameMode(TrainOnly)).
                collect(Collectors.toSet());
        assertTrue(notAvailableTrain.isEmpty(), "unexpected " + HasId.asIds(notAvailableTrain));
    }

    @Disabled("todo for mixed transport modes")
    @Test
    void shouldNotAddAnyInterchangeNotAlreadyMarked() {
        ImmutableIdSet<Station> interchangeButNotMarked = stationRepository.getStations().stream().
                filter(station -> interchangeRepository.isInterchange(station)).
                filter(station -> station.getTransportModes().size()==1).
                filter(found -> !found.isMarkedInterchange()).
                collect(IdSet.collector());

        assertTrue(interchangeButNotMarked.isEmpty(), interchangeButNotMarked.toString());
    }

    private Station getStation(RailStationIds railStationIds) {
        return stationRepository.getStationById(railStationIds.getId());
    }


}
