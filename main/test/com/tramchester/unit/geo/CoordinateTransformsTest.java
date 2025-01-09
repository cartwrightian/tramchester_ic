package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.BusStations.PiccadilyStationStopA;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateTransformsTest {

    final static BoundingBox bounds = TestEnv.getGreaterManchesterBounds();

    private static final GridPosition MaccTheTowersFromNaptan = new GridPosition(391829, 373136);

    // useful table https://blis.com/precision-matters-critical-importance-decimal-places-five-lowest-go/
    private static final double DELTA = 0.0001D; // approx 11 meters

    @Disabled("suspect data")
    @Test
    void shouldConvertToGridCorrectly() {

        double lat = 52.940190;
        double lon = -1.4965572;

        LatLong latLong = new LatLong(lat, lon);

        GridPosition result = CoordinateTransforms.getGridPosition(latLong);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        assertEquals(expectedEasting, result.getEastings());
        assertEquals(expectedNorthing, result.getNorthings());
        assertTrue(result.isValid());
    }

    @Disabled("suspect data, see test based on real data below")
    @Test
    void shouldConvertToLatLongCorrectly() {

        int easting = 433931;
        int northing = 338207;
        GridPosition position = new GridPosition(easting, northing);

        LatLong result = CoordinateTransforms.getLatLong(position);

        double lat = 52.94018971498456;
        double lon = -1.496557148808237;
        assertEquals(lat, result.getLat(), 0.00000000001);
        assertEquals(lon, result.getLon(), 0.00000000001);
        assertTrue(result.isValid());
    }

    @Test
    void shouldConvertInvalidLatLongToInvalid() {
        LatLong  latLong = LatLong.Invalid;
        GridPosition position = CoordinateTransforms.getGridPosition(latLong);
        assertFalse(position.isValid());
    }

    @Test
    void shouldConvertInvalidGridToInvalid() {
       GridPosition gridPosition = GridPosition.Invalid;
       LatLong result = CoordinateTransforms.getLatLong(gridPosition);
       assertFalse(result.isValid());
    }

    @Test
    void shouldConvertForRailFormatGrid() {
        LatLong derby = new LatLong(52.9161645,-1.4655347);

        @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(derby);

        assertEquals(436036, grid.getEastings());
        assertEquals(335549, grid.getNorthings());
    }

    @Test
    void shouldHaveRoundTripStartLatLong() {

        LatLong latLong = BusStations.MacclesfieldTheTowers.getLatLong();

        GridPosition grid = CoordinateTransforms.getGridPosition(latLong);

        LatLong result = CoordinateTransforms.getLatLong(grid);

        assertEquals(latLong.getLon(), result.getLon(), DELTA);
        assertEquals(latLong.getLat(), result.getLat(), DELTA);
    }

    @Test
    void shouldHaveRoundTripStartGrid() {

        // NOTE: will not catch issues if transformations are *equally* off spec

        GridPosition grid = bounds.getBottomLeft();

        LatLong latLong = CoordinateTransforms.getLatLong(grid);

        GridPosition result = CoordinateTransforms.getGridPosition(latLong);

        assertEquals(grid.getEastings(), result.getEastings());
        assertEquals(grid.getNorthings(), result.getNorthings());
    }

    @Disabled("suspect data")
    @Test
    void shouldHaveExpectedGrid() {
        GridPosition grid = CoordinateTransforms.getGridPosition(PiccadilyStationStopA.getLatLong());

        assertEquals(397814, grid.getNorthings());
        assertEquals(384735, grid.getEastings());
    }

    @Test
    void shouldHaveSameConversionFromGridAsNaptan() {

        GridPosition gridPosition = MaccTheTowersFromNaptan;

        LatLong result = CoordinateTransforms.getLatLong(gridPosition);

        LatLong macclesfieldTheTowers = BusStations.MacclesfieldTheTowers.getLatLong();

        assertEquals(macclesfieldTheTowers.getLon(), result.getLon(), DELTA);
        assertEquals(macclesfieldTheTowers.getLat(), result.getLat(), DELTA);

    }

    @Test
    void shouldHaveSameConversionForLatLongAsNaptan() {
        BusStations macclesfieldTheTowers = BusStations.MacclesfieldTheTowers;

        GridPosition result = CoordinateTransforms.getGridPosition(macclesfieldTheTowers.getLatLong());

        int deltaMeters = 2;
        assertEquals(MaccTheTowersFromNaptan.getEastings(), result.getEastings(), deltaMeters);
        assertEquals(MaccTheTowersFromNaptan.getNorthings(), result.getNorthings(), deltaMeters);

    }


}
