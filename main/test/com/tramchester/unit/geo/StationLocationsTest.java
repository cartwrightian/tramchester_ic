package com.tramchester.unit.geo;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.*;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.StationHelper;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownLocations.*;
import static org.junit.jupiter.api.Assertions.*;

class StationLocationsTest extends EasyMockSupport {

    private StationLocations stationLocations;
    private StationRepository stationRepository;
    private NaptanRepository naptanRespository;
    private Geography geography;
    private EnumSet<TransportMode> modes;

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = createMock(StationRepository.class);
        naptanRespository = createMock(NaptanRepository.class);
        PlatformRepository platformRepository = createMock(PlatformRepository.class);
        geography = createMock(Geography.class);

        stationLocations = new StationLocations(stationRepository, platformRepository, naptanRespository, geography);

        modes = EnumSet.of(TransportMode.Tram);
    }

    @Test
    void shouldHaveGridPositionBehaviours() {
        GridPosition gridPositionA = new GridPosition(3,4); // 9 + 16 = 25
        assertEquals(3, gridPositionA.getEastings());
        assertEquals(4, gridPositionA.getNorthings());

        GridPosition origin = new GridPosition(0,0);
        assertEquals(5, GridPositions.distanceTo(origin, gridPositionA));
        assertEquals(5, GridPositions.distanceTo(gridPositionA, origin));

        // not in range
        assertFalse(GridPositions.withinDistEasting(origin, gridPositionA, MarginInMeters.ofMeters(2)));
        // within the given range
        assertTrue(GridPositions.withinDistEasting(origin, gridPositionA,  MarginInMeters.ofMeters(3)));
        assertTrue(GridPositions.withinDistEasting(origin, gridPositionA,  MarginInMeters.ofMeters(4)));

        // not in range
        assertFalse(GridPositions.withinDistNorthing(origin, gridPositionA,  MarginInMeters.ofMeters(2)));
        // within the given range
        assertTrue(GridPositions.withinDistNorthing(origin, gridPositionA,  MarginInMeters.ofMeters(4)));
        assertTrue(GridPositions.withinDistNorthing(origin, gridPositionA,  MarginInMeters.ofMeters(5)));
    }

    @NotNull
    private Station createTestStation(String id, String name, KnownLocations location) {
        return StationHelper.forTest(id, "area", name, location.latLong(), DataSourceID.tfgm, false);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCheckForNearbyStation() {

        Station stationA = createTestStation("id123", "nameA", nearAltrincham);
        Station stationC = createTestStation("id789", "nameC", nearShudehill);

        EasyMock.expect(naptanRespository.isEnabled()).andReturn(false);
        EasyMock.expect(stationRepository.getActiveStationStream()).andStubAnswer(() ->
                Stream.of(stationA, stationC));

        MarginInMeters margin = MarginInMeters.ofMeters(1600);

        EasyMock.expect(geography.getNearToUnsorted(EasyMock.isA(Geography.LocationsSource.class), EasyMock.eq(nearAltrincham.grid()),
                EasyMock.eq(margin))).andReturn(Stream.of(stationA));
        EasyMock.expect(geography.getNearToUnsorted(EasyMock.isA(Geography.LocationsSource.class), EasyMock.eq(nearShudehill.grid()),
                EasyMock.eq(margin))).andReturn(Stream.of(stationA));

        replayAll();
        stationLocations.start();
        assertTrue(stationLocations.anyStationsWithinRangeOf(nearAltrincham.location(), margin));
        assertFalse(stationLocations.anyStationsWithinRangeOf(nearWythenshaweHosp.location(), margin));
        assertTrue(stationLocations.anyStationsWithinRangeOf(nearShudehill.location(), margin));
        assertFalse(stationLocations.anyStationsWithinRangeOf(nearKnutsfordBusStation.location(), margin));
        verifyAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldOrderClosestFirstRespectingNumToFind() {
        Station stationA = createTestStation("id123", "nameA", nearAltrincham);
        Station stationB = createTestStation("id456", "nameB", nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", nearShudehill);

        MyLocation location = nearAltrincham.location();
        final MarginInMeters rangeInMeters = MarginInMeters.ofMeters(20000);

        EasyMock.expect(geography.getNearToSorted(EasyMock.isA(Geography.LocationsSource.class), EasyMock.eq(location.getGridPosition()),
                        EasyMock.eq(rangeInMeters))).
                andReturn(Stream.of(stationA, stationB, stationC));

        replayAll();
        List<Station> results = stationLocations.nearestStationsSorted(location, 1, rangeInMeters, modes);
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(stationA, results.getFirst());
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldOrderClosestFirst() {
        Station stationA = createTestStation("id123", "nameA", nearAltrincham);
        Station stationB = createTestStation("id456", "nameB", nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", nearShudehill);

        MyLocation location = nearAltrincham.location();
        final MarginInMeters rangeInMeters = MarginInMeters.ofMeters(20000);

        EasyMock.expect(geography.getNearToSorted(EasyMock.isA(Geography.LocationsSource.class), EasyMock.eq(location.getGridPosition()),
                        EasyMock.eq(rangeInMeters))).
                andReturn(Stream.of(stationA, stationB, stationC));

        replayAll();
        List<Station> results = stationLocations.nearestStationsSorted(location, 3, rangeInMeters, modes);
        verifyAll();

        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));
    }

    @Test
    void shouldCaptureBoundingAreaForStations() {
        Station testStationA = createTestStation("id123", "name", nearAltrincham);
        Station testStationB = createTestStation("id456", "name", nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", nearPiccGardens);

        EasyMock.expect(naptanRespository.isEnabled()).andReturn(false);
        EasyMock.expect(stationRepository.getActiveStationStream()).andStubAnswer(() -> Stream.of(testStationA, testStationB, testStationC));

        GridPosition posA = testStationA.getGridPosition();
        GridPosition posB = testStationB.getGridPosition();

        replayAll();
        stationLocations.start();
        BoundingBox bounds = stationLocations.getActiveStationBounds();
        verifyAll();

        // bottom left
        assertEquals(posA.getEastings(), bounds.getMinEastings());
        assertEquals(posA.getNorthings(), bounds.getMinNorthings());
        // top right
        assertEquals(posB.getEastings(), bounds.getMaxEasting());
        assertEquals(posB.getNorthings(), bounds.getMaxNorthings());
    }

    @Test
    void shouldHaveExpectedBoundingBoxes() {
        final int gridSize = 50;

        Station testStationA = createTestStation("id123", "name", nearPiccGardens);
        Station testStationC = createTestStation("id789", "nameB", nearShudehill);

        EasyMock.expect(naptanRespository.isEnabled()).andReturn(false);
        EasyMock.expect(stationRepository.getActiveStationStream()).andStubAnswer(() -> Stream.of(testStationA, testStationC));

        replayAll();
        stationLocations.start();
        BoundingBox area = stationLocations.getActiveStationBounds();
        // todo use StationBoxFactory here
        Set<BoundingBox> found = stationLocations.createBoundingBoxesFor(gridSize).collect(Collectors.toSet());
        verifyAll();

        Set<BoundingBox> expected = new HashSet<>();
        for (int x = area.getMinEastings(); x <= area.getMaxEasting(); x = x + gridSize) {
            for (int y = area.getMinNorthings(); y <= area.getMaxNorthings(); y = y + gridSize) {
                BoundingBox box = new BoundingBox(x, y, x + gridSize, y + gridSize);
                expected.add(box);
            }
        }
        assertEquals(12, expected.size());

        assertEquals(expected.size(), found.size());
        Set<BoundingBox> notFound = new HashSet<>(expected);
        notFound.removeAll(found);
        assertEquals(Collections.emptySet(), notFound);
    }

    @Test
    void shouldGridUpStations() {

        Station testStationA = createTestStation("id123", "name", nearAltrincham);
        Station testStationB = createTestStation("id456", "name", nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", nearPiccGardens);

        EasyMock.expect(naptanRespository.isEnabled()).andReturn(false);
        EasyMock.expect(stationRepository.getActiveStationStream()).andStubAnswer(() -> Stream.of(testStationA, testStationB, testStationC));

        replayAll();
        stationLocations.start();
        List<BoundingBoxWithStations> boxedStations = stationLocations.getStationsInGrids(1000).toList();

        assertEquals(2, boxedStations.size());

        // one box should contain the two central stations
        Optional<BoundingBoxWithStations> maybeCentral = boxedStations.stream().filter(box -> box.getStations().size() == 2).findFirst();
        assertTrue(maybeCentral.isPresent());
        BoundingBoxWithStations centralBox = maybeCentral.get();

        LocationSet<Station> central = centralBox.getStations();
        assertEquals(2, central.size());
        assertTrue(central.contains(testStationB));
        assertTrue(central.contains(testStationC));
//        assertTrue(central.containsAll(Arrays.asList(testStationB, testStationC)));
        assertTrue(centralBox.contained(nearShudehill.grid()));
        assertTrue(centralBox.contained(nearPiccGardens.grid()));

        // other box should contain the one non-central
        Optional<BoundingBoxWithStations> maybeAlty = boxedStations.stream().filter(box -> box.getStations().size() == 1).findFirst();
        assertTrue(maybeAlty.isPresent());
        BoundingBoxWithStations altyBox = maybeAlty.get();

        LocationSet<Station> alty = altyBox.getStations();
        assertEquals(1, alty.size());
        assertTrue(alty.contains(testStationA));
        assertTrue(altyBox.contained(nearAltrincham.grid()));

        verifyAll();

    }

}
