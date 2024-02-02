package com.tramchester.geo;

import com.tramchester.domain.presentation.LatLong;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.CoordinateOperation;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
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

    static {
        logger = LoggerFactory.getLogger(CoordinateTransforms.class);

        try {
            nationalGridCRS = CRS.decode(NATION_GRID_EPSG);
            latLongCRS = CRS.decode(WGS84_LATLONG_EPGS);

            logCoordinateRefSystem("national grid", nationalGridCRS);
            logCoordinateRefSystem("lat long", latLongCRS);

            DefaultCoordinateOperationFactory defaultCoordinateOperationFactory = new DefaultCoordinateOperationFactory();

            CoordinateOperation latLongToGridOp = defaultCoordinateOperationFactory.createOperation(latLongCRS, nationalGridCRS);
//            CoordinateOperation gridToLatLongOp = defaultCoordinateOperationFactory.createOperation(nationalGridCRS, latLongCRS);

            gridToLatLongTransform = CRS.findMathTransform(nationalGridCRS, latLongCRS, false);

//            gridToLatLongTransform = gridToLatLongOp.getMathTransform();
            latLongToGridTransform = latLongToGridOp.getMathTransform();

        } catch (FactoryException e) {
            String msg = "Unable to init geotools factory or transforms";
            logger.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    private static void logCoordinateRefSystem(String prefix, CoordinateReferenceSystem nationalGridRefSys) {
        logger.info(prefix +": ids:'" + nationalGridRefSys.getIdentifiers() + "' name:'" + nationalGridRefSys.getName() + "'");
    }

    @NotNull
    public static GridPosition getGridPosition(final LatLong position) {
        if (!position.isValid()) {
            logger.warn("Position invalid " + position);
            return GridPosition.Invalid;
        }

        try {
            // note the lat(y) lon(x) ordering here
            final Position2D directPositionLatLong = new Position2D(latLongCRS, position.getLat(), position.getLon());
            final Position directPositionGrid = latLongToGridTransform.transform(directPositionLatLong, null);

            final long easting = Math.round(directPositionGrid.getOrdinate(0));
            final long northing = Math.round(directPositionGrid.getOrdinate(1));

            return new GridPosition(easting, northing);
        }
        catch (TransformException transformException) {
            logger.warn("Could not transform " + position, transformException);
            return GridPosition.Invalid;
        }
    }

    public static LatLong getLatLong(final GridPosition gridPosition) {
        if (!gridPosition.isValid()) {
            logger.warn("Position invalid " + gridPosition);
            return LatLong.Invalid;
        }

        try {
            final Position2D directPositionGrid = new Position2D(nationalGridCRS, gridPosition.getEastings(),
                    gridPosition.getNorthings());

            final Position directPositionLatLong = gridToLatLongTransform.transform(directPositionGrid, null);
            final double lat = directPositionLatLong.getOrdinate(0);
            final double lon = directPositionLatLong.getOrdinate(1);
            return new LatLong(lat, lon);
        }
        catch (TransformException transformException) {
            logger.warn("Could not transform " + gridPosition, transformException);
            return LatLong.Invalid;
        }
    }


}
