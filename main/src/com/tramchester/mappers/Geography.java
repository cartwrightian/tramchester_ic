package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.GridPositions;
import com.tramchester.geo.MarginInMeters;
import org.apache.commons.lang3.stream.Streams;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.inject.Inject;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static tech.units.indriya.unit.Units.SECOND;



// TODO Consider using GeodeticCalculator from geotools?
// https://gis.stackexchange.com/questions/110249/coordinate-conversion-epsg3857-to-epsg4326-using-opengis-jts-too-slow

@LazySingleton
public class Geography {
    private static final Logger logger = LoggerFactory.getLogger(Geography.class);

    private static final String AUTHORITY = "EPSG";

    // EPSG:4326
    // see also CoordinateTransformations
    private static final String latLongCode = DefaultGeographicCRS.WGS84.getIdentifier(new CitationImpl(AUTHORITY)).getCode();

    private final GeometryFactory geometryFactoryLatLong;
    private final Quantity<Speed> walkingSpeed;

    @Inject
    public Geography(TramchesterConfig config) {
        walkingSpeed = config.getWalkingSpeed();

        final int srid = Integer.parseInt(latLongCode);
        geometryFactoryLatLong = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), srid);
    }

    public Quantity<Time> getWalkingTime(final Quantity<Length> distance) {
        return distance.divide(walkingSpeed).asType(Time.class);
    }

    private Duration getWalkingDuration(final Quantity<Length> distance) {
        final double seconds = getWalkingTime(distance).to(SECOND).getValue().doubleValue();
        //noinspection WrapperTypeMayBePrimitive , should be no warning since longValue() is called.....
        final Double roundUp = Math.ceil(seconds);
        return Duration.of(roundUp.longValue(), SECONDS);
    }

    public Duration getWalkingDuration(final Location<?> locationA, final Location<?> locationB) {
        final Quantity<Length> distance = getDistanceBetweenInMeters(locationA, locationB);
        return getWalkingDuration(distance);
    }

    /***
     * Uses lat/long, slower but accurate
     * @param placeA location A
     * @param placeB location B
     * @return distance between A and B
     */
    public ComparableQuantity<Length> getDistanceBetweenInMeters(final Location<?> placeA, final Location<?> placeB) {
        final Point pointA = geometryFactoryLatLong.createPoint(placeA.getLatLong().getCoordinate());
        final Point pointB = geometryFactoryLatLong.createPoint(placeB.getLatLong().getCoordinate());

        final GeodeticCalculator geodeticCalculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);

        geodeticCalculator.setStartingGeographicPoint(pointA.getX(), pointA.getY());
        geodeticCalculator.setDestinationGeographicPoint(pointB.getX(), pointB.getY());

        return Quantities.getQuantity(geodeticCalculator.getOrthodromicDistance(), Units.METRE);
    }

    public List<LatLong> createBoundaryFor(final Stream<LatLong> locations) {
        final Coordinate[] coords = locations.
                map(latLong -> new Coordinate(latLong.getLat(), latLong.getLon())).
                collect(Streams.toArray(Coordinate.class));

        final MultiPoint multiPoint = geometryFactoryLatLong.createMultiPointFromCoords(coords);

        final Geometry boundary = multiPoint.convexHull().getBoundary();

        if (boundary.getNumPoints()==0) {
            logger.warn("Created a boundary with zero points");
        }

        return Arrays.stream(boundary.getCoordinates()).map(LatLong::of).toList();
    }

    private <T extends Location<T>> Stream<T> getNearbyCrude(final LocationsSource<T> locationsSource,
                                                             final GridPosition gridPosition, final MarginInMeters rangeInMeters) {

        return locationsSource.get().
                filter(location -> location.getGridPosition().isValid()).
                filter(location -> GridPositions.withinDistEasting(gridPosition, location.getGridPosition(), rangeInMeters)).
                filter(location -> GridPositions.withinDistNorthing(gridPosition, location.getGridPosition(), rangeInMeters));
    }

    public <T extends Location<T>> Stream<T> getNearToUnsorted(final LocationsSource<T> locationsSource, final GridPosition gridPosition,
                                                               final MarginInMeters rangeInMeters) {
        return getNearbyCrude(locationsSource, gridPosition, rangeInMeters).
                filter(entry -> GridPositions.withinDist(gridPosition, entry.getGridPosition(), rangeInMeters));
    }

    public <T extends Location<T>> Stream<T> getNearToSorted(final LocationsSource<T> locationsSource,
                                                             final GridPosition gridPosition, final MarginInMeters rangeInMeters) {
        return getNearToUnsorted(locationsSource, gridPosition, rangeInMeters).
                sorted((a, b) -> chooseNearestToGrid(gridPosition, a.getGridPosition(), b.getGridPosition()));
    }

    public int chooseNearestToGrid(final GridPosition grid, final GridPosition first, final GridPosition second) {
        final long firstDist = GridPositions.distanceTo(grid, first);
        final long secondDist = GridPositions.distanceTo(grid, second);
        return Long.compare(firstDist, secondDist);
    }

    public interface LocationsSource<T extends Location<T>> {
        Stream<T> get();
    }

}
