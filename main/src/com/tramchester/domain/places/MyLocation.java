package com.tramchester.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class MyLocation implements Location<MyLocation> {

    @Deprecated
    public static final String MY_LOCATION_PLACEHOLDER_ID = "MyLocationPlaceholderId";

    public static IdFor<MyLocation> createId(LatLong latLong) {
        String text = String.format("%s,%s", latLong.getLat(), latLong.getLon());
        return StringIdFor.createId(text, MyLocation.class);
    }

    private final LatLong latLong;

    public static MyLocation create(LatLong latLong) {
        return new MyLocation(latLong);
    }

    public static MyLocation parseFromId(String rawId) {
        String[] parts = rawId.split(",");
        if (parts.length!=2) {
            throw new NumberFormatException("Cannot parse LatLong " + rawId);
        }
        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);
        return new MyLocation(new LatLong(lat, lon));
    }

    @Override
    public String toString() {
        return "MyLocation{" + latLong + '}';
    }

    public MyLocation(LatLong latLong) {
        this.latLong = latLong;
    }

    @Override
    public IdFor<MyLocation> getId() {
        return createId(latLong);
    }

    @Override
    public String getName() {
        return "My Location";
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public GridPosition getGridPosition() {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    @Override
    public IdFor<NPTGLocality> getLocalityId() {
        return StringIdFor.invalid(NPTGLocality.class);
    }

    @Override
    public boolean hasPlatforms() {
        return false;
    }

    @Override
    public Set<Platform> getPlatforms() {
        return null;
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return EnumSet.of(TransportMode.Walk);
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.MyLocation;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return DataSourceID.internal;
    }

    @Override
    public boolean hasPickup() {
        return true;
    }

    @Override
    public boolean hasDropoff() {
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isMarkedInterchange() {
        return false;
    }

    @Override
    public GraphLabel getNodeLabel() {
        return GraphLabel.QUERY_NODE;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.WALK_ID;
    }

    @Deprecated
    public static boolean isUserLocation(String text) {
        return MY_LOCATION_PLACEHOLDER_ID.equals(text);
    }

    @Override
    public boolean containsOthers() {
        return false;
    }

    @Override
    public LocationSet<Station> getAllContained() {
        throw new RuntimeException("Not implemented");
    }
}
