package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TestRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.CentralZoneStation.StPetersSquare;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class StationRepositoryTest {
    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private TramDate when;
    private StationRepository stationRepository;
    private InterchangeRepository interchangeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);

        when = TestEnv.testDay();
    }

    @Test
    void shouldHaveExpectedStationsForRoute() {
        Route buryToAlty = routeHelper.getGreen(when);

        Set<Station> allStations = stationRepository.getStations(EnumSet.of(Tram));

        IdSet<Station> dropOffs = allStations.stream().filter(station -> station.servesRouteDropOff(buryToAlty)).collect(IdSet.collector());

        // summer 2025 closures
        int expectedNumStations = 27 - 1;

        assertEquals(expectedNumStations, dropOffs.size(), dropOffs.toString());

        assertTrue(dropOffs.contains(Altrincham.getId()));
        assertTrue(dropOffs.contains(Cornbrook.getId()));
        assertTrue(dropOffs.contains(Shudehill.getId()));

        assertTrue(dropOffs.contains(Bury.getId()));

        IdSet<Station> pickUps = allStations.stream().filter(station -> station.servesRoutePickup(buryToAlty)).collect(IdSet.collector());

        assertEquals(expectedNumStations, pickUps.size(), pickUps.toString());
        assertTrue(pickUps.contains(Bury.getId()));
        assertTrue(pickUps.contains(Cornbrook.getId()));
        assertTrue(pickUps.contains(Shudehill.getId()));
        assertTrue(pickUps.contains(Altrincham.getId()));
    }

    @Test
    void shouldReproIssueWithShudehillAppearingOnRedRoute() {

        Station shudehill = Shudehill.from(stationRepository);

        @NotNull Set<String> lines = shudehill.getDropoffRoutes().stream().
                filter(route -> route.isAvailableOn(when)).
                map(Route::getShortName).
                collect(Collectors.toSet());

        assertEquals(4, lines.size(), lines.toString());

    }

    @Test
    void shouldHaveAtLeastOnePlatformForEveryStation() {
        Set<Station> stations = stationRepository.getStations(EnumSet.of(Tram));
        Set<Station> noPlatforms = stations.stream().filter(station -> station.getPlatforms().isEmpty()).collect(Collectors.toSet());
        assertEquals(Collections.emptySet(),noPlatforms);
    }

    @Disabled("WIP - not clear if actually creates an issue")
    @Test
    void shouldCheckForTerminatingTripsAtNonInterchangeStations() {

        // some trips end at non-interchange stations i.e. early morning, late night, return to depots

        TripRepository repository = componentContainer.get(TripRepository.class);

        Set<Trip> allTips = repository.getTrips();

        List<Station> nonInterchanges = stationRepository.getAllStationStream().filter(station -> !interchangeRepository.isInterchange(station)).toList();

        Set<Station> results = new HashSet<>();

        nonInterchanges.forEach(station -> {
            IdFor<Station> stationId = station.getId();

            Set<Trip> tripsForStation = allTips.stream().
                    filter(trip -> trip.callsAt(stationId)).
                    collect(Collectors.toSet());

            long passingThroughOnly = tripsForStation.stream().
                    filter(trip -> !trip.firstStation().equals(stationId)).
                    filter(trip -> !trip.lastStation().equals(stationId)).count();

            List<Trip> terminatesHere = tripsForStation.stream().filter(trip -> trip.lastStation().equals(stationId)).toList();

            if ((passingThroughOnly!=0) && !terminatesHere.isEmpty()) {
                results.add(station);
            }
        });

        assertTrue(results.isEmpty(), HasId.asIds(results));
    }

    @Test
    void shouldEndOfLineGetStation() {

        NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);
        boolean naptanEnabled = naptanRepository.isEnabled();

        assertTrue(stationRepository.hasStationId(Altrincham.getId()));

        Station station = Altrincham.from(stationRepository);
        assertEquals("Altrincham", station.getName());

        assertEquals(LocationType.Station, station.getLocationType());
        assertTrue(station.servesMode(Tram));
        assertTrue(station.isActive());
        assertFalse(station.isMarkedInterchange());

        assertTrue(station.hasDropoff());
        assertTrue(station.hasPickup());

        assertEquals(Duration.ofMinutes(1), station.getMinChangeDuration());

        double delta = naptanEnabled ? 0.01 : 0.0001;
        LatLong expectedLatLong = Altrincham.getLatLong();
        assertEquals(expectedLatLong.getLat(), station.getLatLong().getLat(), delta);
        assertEquals(expectedLatLong.getLon(), station.getLatLong().getLon(), delta);

        if (!naptanEnabled) {
            // naptan position differ from tfgm
            GridPosition expectedGrid = CoordinateTransforms.getGridPosition(expectedLatLong);
            assertEquals(expectedGrid, station.getGridPosition());
        }

        assertEquals(DataSourceID.tfgm, station.getDataSourceID());

        if (naptanEnabled) {
            assertEquals(KnownLocality.Altrincham.getLocalityId(), station.getLocalityId());
            assertTrue(station.isCentral());
        } else {
            // Next two depend on naptan for "real" results
            assertEquals(NPTGLocality.InvalidId(), station.getLocalityId());
            assertFalse(station.isCentral());
        }

        // platform
        assertTrue(station.hasPlatforms());
        assertEquals(1, station.getPlatforms().size());
        final Optional<Platform> maybePlatformOne = station.getPlatforms().stream().findFirst();
        assertTrue(maybePlatformOne.isPresent());

        Platform platformOne = maybePlatformOne.get();
        final IdFor<Platform> expectedId = PlatformId.createId(Altrincham.getId(), "1");

        assertEquals(expectedId, platformOne.getId());
        assertEquals( "1", platformOne.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platformOne.getName());

        assertEquals(station.getDataSourceID(), platformOne.getDataSourceID());
        assertEquals(LocationType.Platform, platformOne.getLocationType());
    }

    @Test
    void shouldHaveExpectedPickupAndDropoffForCornbrook() {
        Station station = Cornbrook.from(stationRepository);

        assertTrue(station.hasDropoff());
        assertTrue(station.hasPickup());

        List<TestRoute> expected =
                Arrays.asList(
                        getGreen(when),
                        getBlue(when),
                        getNavy(when),
                        getRed(when),
                        //getPurple(when), // picc gardens summer 2025
                        getPink(when));

        IdSet<Route> expectedIds = expected.stream().
                map(TestRoute::getId).
                collect(IdSet.idCollector());

        IdSet<Route> pickups = station.getPickupRoutes().stream().
            filter(route -> route.isAvailableOn(when)).
            collect(IdSet.collector());

        IdSet<Route> mismatch = IdSet.disjunction(expectedIds, pickups);

        assertEquals(IdSet.emptySet(), mismatch, "expected " + expectedIds + "\n found " + pickups);

        IdSet<Route> dropOffs = station.getDropoffRoutes().stream().
            filter(route -> route.isAvailableOn(when)).
            collect(IdSet.collector());
        assertEquals(expectedIds.size(), dropOffs.size());
        assertTrue(pickups.containsAll(dropOffs));
    }

    @Test
    void shouldHaveExpectedPickupAndDropoffForOneStation() {
        Station station = TraffordCentre.from(stationRepository);

        assertTrue(station.hasDropoff());
        assertTrue(station.hasPickup());

        IdSet<Route> pickups = station.getPickupRoutes().stream().
            filter(route -> route.isAvailableOn(when)).
            collect(IdSet.collector());
        assertEquals(1, pickups.size());
        assertTrue(pickups.contains(getRed(when).getId()), pickups.toString());

        IdSet<Route> dropOffs = station.getDropoffRoutes().stream().
            filter(route -> route.isAvailableOn(when)).
            collect(IdSet.collector());
            
        assertEquals(1, dropOffs.size());
        assertTrue(pickups.contains(getRed(when).getId()));
    }

    @Test
    void shouldHaveExpectedPickupAndDropOffsForMediaCity() {
        // seen issues here
        Station mediaCity = MediaCityUK.from(stationRepository);
        IdFor<Route> blueRouteId = getBlue(when).getId();

        IdSet<Route> dropOffs = mediaCity.getDropoffRoutes().stream().collect(IdSet.collector());

        assertEquals(1, dropOffs.size(), dropOffs.toString());
        assertTrue(dropOffs.contains(blueRouteId));

        IdSet<Route> pickUps = mediaCity.getDropoffRoutes().stream().collect(IdSet.collector());
        assertEquals(1, pickUps.size(), pickUps.toString());
        assertTrue(pickUps.contains(blueRouteId));
    }

    @Test
    @Disabled("naptan load is disabled for trams")
    void shouldHaveLocalityForCityCenterStop() {
        Station station = stationRepository.getStationById(StPetersSquare.getId());
        assertEquals(KnownLocality.ManchesterCityCentre.getId(), station.getLocalityId());
    }

}
