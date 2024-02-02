package com.tramchester.unit.domain.presentation;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.domain.presentation.LatLong;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.CoordinateOperation;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


class LatLongTest {

    private static final double DELTA = 0.05D;
    private static CoordinateReferenceSystem nationalGridRefSys;
    private static CoordinateReferenceSystem latLongRef;
    private static final ObjectMapper mapper = JsonMapper.builder().addModule(new AfterburnerModule()).build();

    @BeforeAll
    static void onceBeforeAllTests() throws FactoryException {
        CRSAuthorityFactory authorityFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);

        nationalGridRefSys = authorityFactory.createCoordinateReferenceSystem("27700");
        latLongRef = authorityFactory.createCoordinateReferenceSystem("4326");

    }

    @Test
    void shouldBeAbleToSerialiseAndDeSerialise() throws IOException {
        LatLong latLong = new LatLong(-1,2);

        String output = mapper.writeValueAsString(latLong);
        assertEquals("{\"lat\":-1.0,\"lon\":2.0,\"valid\":true}", output);

        LatLong result = mapper.readValue(output, LatLong.class);

        assertEquals(-1, result.getLat(),0);
        assertEquals(2, result.getLon(),0);
        assertTrue(result.isValid());
    }

    @Test
    void shouldBeAbleToSerialiseAndDeSerialiseInvalid() throws IOException {
        LatLong latLong = LatLong.Invalid;

        String output = mapper.writeValueAsString(latLong);

        LatLong result = mapper.readValue(output, LatLong.class);

        assertFalse(result.isValid());
    }

    @Test
    void shouldBeAbleToSetGet() {
        LatLong latLong = new LatLong();
        latLong.setLat(5);
        latLong.setLon(2);

        assertEquals(5, latLong.getLat(), 0);
        assertEquals(2, latLong.getLon(), 0);
        assertTrue(latLong.isValid());
    }

    @Test
    void shouldConvertEastingsNorthingToLatLong() throws FactoryException, TransformException {
        int easting = 433931;
        int northing = 338207;

        Position eastNorth = new Position2D(easting, northing);

        CoordinateOperation operation = new DefaultCoordinateOperationFactory().createOperation(nationalGridRefSys, latLongRef);
        Position latLong = operation.getMathTransform().transform(eastNorth, null);

        double expectedLat = 52.940190;
        double expectedLon = -1.4965572;

        assertEquals(expectedLat, latLong.getOrdinate(0), DELTA);
        assertEquals(expectedLon, latLong.getOrdinate(1), DELTA);

    }

    @Test
    void shouldHaveInvalidLatLong() {
        LatLong latLong = LatLong.Invalid;

        assertFalse(latLong.isValid());
    }

    @Test
    void shouldConvertLatLongToEastingsNorthing() throws FactoryException, TransformException {
        double lat = 52.940190;
        double lon = -1.4965572;

        Position latLong = new Position2D(lat, lon);

        CoordinateOperation operation = new DefaultCoordinateOperationFactory().createOperation(latLongRef, nationalGridRefSys);
        Position nationalGrid = operation.getMathTransform().transform(latLong, null);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        long easting = Math.round(nationalGrid.getOrdinate(0));
        long northing = Math.round(nationalGrid.getOrdinate(1));

        assertEquals(expectedEasting, easting);
        assertEquals(expectedNorthing, northing);
    }

}
