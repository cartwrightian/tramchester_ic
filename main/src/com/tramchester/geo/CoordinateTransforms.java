package com.tramchester.geo;

import com.tramchester.domain.presentation.LatLong;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.AxisDirection;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.api.referencing.operation.CoordinateOperation;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.LogbackLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinateTransforms {
    private static final Logger logger;

    // NationGrid (i.e. from naptan) uses OSGB36 a different DATUM from WGS84

    public static final String NATION_GRID_EPSG = "EPSG:27700";
    public static final String WGS84_LATLONG_EPGS = "EPSG:4326";

    private static final MathTransform latLongToGridTransform;
    private static final MathTransform gridToLatLongTransform;

    private static final CoordinateReferenceSystem nationalGridCRS;
    private static final CoordinateReferenceSystem latLongCRS;

    // these can (and do) vary based on hsql or wkt data sources
    private static final int longDimension;
    private static final int latDimension;
    private static final int eastingDimension;
    private static final int northingDimension;

    static {
        logger = LoggerFactory.getLogger(CoordinateTransforms.class);

        try {

            logger.info("geotools init");
            GeoTools.setLoggerFactory(LogbackLoggerFactory.getInstance());
            GeoTools.init();

            // Need to enable DEBUG logging in logback config for most geo tools logging
//            java.util.logging.Logger geoToolsLogging = Logging.getLogger("org.geotools");
//            geoToolsLogging.info("test geo tools logging");

            logger.info("geotools started, load CRS");

            nationalGridCRS = CRS.decode(NATION_GRID_EPSG);
            latLongCRS = CRS.decode(WGS84_LATLONG_EPGS);

            logCoordinateRefSystem("national grid", nationalGridCRS);
            logCoordinateRefSystem("lat long", latLongCRS);

            final CoordinateSystemAxis latlongAxis = getFirstAxis(latLongCRS);
            final boolean longFirst = isEastDirection(latlongAxis);
            longDimension = longFirst ? 0 : 1;
            latDimension = longFirst ? 1 : 0;

            final CoordinateSystemAxis gridAxis = getFirstAxis(nationalGridCRS);
            final boolean eastingFirst = isEastDirection(gridAxis);
            eastingDimension = eastingFirst ? 0 : 1;
            northingDimension = eastingFirst ? 1 : 0;

            logger.info("CRS loaded");

            DefaultCoordinateOperationFactory defaultCoordinateOperationFactory = new DefaultCoordinateOperationFactory();

            CoordinateOperation latLongToGridOp = defaultCoordinateOperationFactory.createOperation(latLongCRS, nationalGridCRS);

            gridToLatLongTransform = CRS.findMathTransform(nationalGridCRS, latLongCRS, false);

            latLongToGridTransform = latLongToGridOp.getMathTransform();

        } catch (FactoryException e) {
            String msg = "Unable to init geotools factory or transforms";
            logger.error(msg, e);
            throw new RuntimeException(msg);
        }
    }


    private static void logCoordinateRefSystem(final String prefix, final CoordinateReferenceSystem referenceSystem) {
        final CoordinateSystem coordinateSystem = referenceSystem.getCoordinateSystem();
        logger.info(prefix +": ids:'" + referenceSystem.getIdentifiers() + "' name:'" + referenceSystem.getName() + "'"
            + "axis: " + coordinateSystem.getAxis(0) + "," + coordinateSystem.getAxis(1));
    }

    @NotNull
    public static GridPosition getGridPosition(final LatLong latLong) {
        if (!latLong.isValid()) {
            logger.warn("Position invalid " + latLong);
            return GridPosition.Invalid;
        }

        try {
            final Position directPositionLatLong = toPosition(latLong);

            final Position directPositionGrid = latLongToGridTransform.transform(directPositionLatLong, null);

            final int easting = Math.toIntExact(Math.round(directPositionGrid.getOrdinate(eastingDimension)));
            final int northing = Math.toIntExact(Math.round(directPositionGrid.getOrdinate(northingDimension)));

            return new GridPosition(easting, northing);
        }
        catch (TransformException transformException) {
            logger.warn("Could not transform " + latLong, transformException);
            return GridPosition.Invalid;
        }
    }

    public static LatLong getLatLong(final GridPosition gridPosition) {
        if (!gridPosition.isValid()) {
            logger.warn("Position invalid " + gridPosition);
            return LatLong.Invalid;
        }

        try {
            final Position directPositionGrid = toPosition(gridPosition);

            final Position directPositionLatLong = gridToLatLongTransform.transform(directPositionGrid, null);

            final double lat = directPositionLatLong.getOrdinate(latDimension);
            final double lon = directPositionLatLong.getOrdinate(longDimension);
            return new LatLong(lat, lon);
        }
        catch (TransformException transformException) {
            logger.warn("Could not transform " + gridPosition, transformException);
            return LatLong.Invalid;
        }
    }

    private static @NotNull Position toPosition(final LatLong latLong) {
        final Position directPositionLatLong;
        if (latDimension==0) {
            directPositionLatLong = new Position2D(latLongCRS, latLong.getLat(), latLong.getLon());
        } else {
            directPositionLatLong = new Position2D(latLongCRS, latLong.getLon(), latLong.getLat());
        }
        return directPositionLatLong;
    }

    private static @NotNull Position toPosition(final GridPosition gridPosition) {
        final Position directPositionGrid;
        if (eastingDimension==0) {
            directPositionGrid = new Position2D(nationalGridCRS, gridPosition.getEastings(),
                    gridPosition.getNorthings());
        } else {
            directPositionGrid = new Position2D(nationalGridCRS, gridPosition.getNorthings(),
                    gridPosition.getEastings());
        }
        return directPositionGrid;
    }

    private static CoordinateSystemAxis getFirstAxis(CoordinateReferenceSystem referenceSystem) {
        return referenceSystem.getCoordinateSystem().getAxis(0);
    }

    private static boolean isEastDirection(CoordinateSystemAxis latlongAxis) {
        return latlongAxis.getDirection().equals(AxisDirection.EAST);
    }

}
