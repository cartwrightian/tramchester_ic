package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.testSupport.reference.KnownLocations.*;
import static org.junit.jupiter.api.Assertions.*;

public class StationLocationsTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private StationLocations locations;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfigWithNaptan(EnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        locations = componentContainer.get(StationLocations.class);
        closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    @Test
    void shouldHaveAllStationsContainedWithBounds() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        Set<Station> allStations = stationRepository.getStations();
        BoundingBox stationBounds = locations.getActiveStationBounds();

        TramDate date = TestEnv.testDay();

        IdSet<Station> missing = allStations.stream().
                filter(station -> !closedStationsRepository.isClosed(station, date)).
                filter(station -> !stationBounds.contained(station.getGridPosition())).
                collect(IdSet.collector());

        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldHaveExpectedNumberOfQuadrant() {
        assertEquals(88, locations.getQuadrants().size());
    }

    @Test
    void shouldHaveLocationsInBounds() {
        MarginInMeters margin = config.getWalkingDistanceRange();
        final BoundingBox fullBoundsOfAllTramStations = locations.getActiveStationBounds();

        assertTrue(fullBoundsOfAllTramStations.contained(nearShudehill.grid()));
        assertTrue(fullBoundsOfAllTramStations.contained(nearPiccGardens.grid()));

        assertTrue(fullBoundsOfAllTramStations.within(margin, nearAltrincham.grid()));

        assertFalse(fullBoundsOfAllTramStations.contained(nearGreenwichLondon.grid()));
    }

    @Test
    void shouldHaveExpectedStationLocations() {
        MarginInMeters margin = config.getWalkingDistanceRange();

        assertTrue(locations.anyStationsWithinRangeOf(nearShudehill.location(), margin));
        assertTrue(locations.anyStationsWithinRangeOf(nearPiccGardens.location(), margin));

        assertTrue(locations.anyStationsWithinRangeOf(nearAltrincham.location(), margin));

        assertFalse(locations.anyStationsWithinRangeOf(nearGreenwichLondon.location(), margin));
        assertFalse(locations.anyStationsWithinRangeOf(nearStockportBus.location(), margin));
    }

    @Test
    void shouldHaveStationsInAnArea() {
        Station station = TramStations.StPetersSquare.from(stationRepository);

        IdFor<NPTGLocality> areaId = station.getLocalityId();
        assertTrue(areaId.isValid());

        assertTrue(locations.hasStationsOrPlatformsIn(areaId));

        LocationCollection found = locations.getLocationsWithin(areaId);
        assertFalse(found.isEmpty());
        assertTrue(found.contains(station.getLocationId()));
    }

    @Test
    void shouldHavePlatformsInAnArea() {
        Station stPeters = TramStations.StPetersSquare.from(stationRepository);

        final Set<Platform> platforms = stPeters.getPlatforms();

        assertEquals(4, platforms.size());

        platforms.forEach(platform -> {
            IdFor<NPTGLocality> areaId = platform.getLocalityId();
            assertTrue(areaId.isValid(), "platform " + platform);

            assertTrue(locations.hasStationsOrPlatformsIn(areaId), "platform " + platform);

            LocationCollection found = locations.getLocationsWithin(areaId);
            assertFalse(found.isEmpty(), "platform " + platform);
            assertTrue(found.contains(platform.getLocationId()), "platform " + platform + " not in " + found);
        });
    }

    @Test
    void shouldGetBoundsForTrams() {
        BoundingBox box = locations.getActiveStationBounds();

        assertEquals(376979, box.getMinEastings());
        assertEquals(385427, box.getMinNorthings());
        assertEquals(394169, box.getMaxEasting());
        assertEquals(413431, box.getMaxNorthings());

    }


}
