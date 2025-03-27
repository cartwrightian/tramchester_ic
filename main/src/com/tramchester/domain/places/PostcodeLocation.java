package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class PostcodeLocation implements Location<PostcodeLocation>, CoreDomain, HasId<PostcodeLocation> {

    private final PostcodeLocationId id;
    private final String name;

    private LatLong latLong;
    private GridPosition gridPosition;

    public PostcodeLocation(GridPosition gridPosition, PostcodeLocationId id) {
        this(id, CoordinateTransforms.getLatLong(gridPosition), gridPosition);
    }

    private PostcodeLocation(PostcodeLocationId id, LatLong latLong, GridPosition gridPosition) {
        this.id = id;
        this.name = id.getName();

        this.latLong = latLong;
        this.gridPosition = gridPosition;
    }

    public static PostcodeLocationId createId(String rawId) {
        return PostcodeLocationId.create(rawId);
    }

    @Override
    public PostcodeLocationId getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        if (latLong==null) {
            latLong = CoordinateTransforms.getLatLong(gridPosition);
        }
        return latLong;
    }

    @Override
    public GridPosition getGridPosition() {
        if (gridPosition==null) {
            gridPosition = CoordinateTransforms.getGridPosition(latLong);
        }
        return gridPosition;
    }

    @Override
    public boolean containsOthers() {
        return false;
    }

    @Override
    public LocationSet<Station> getAllContained() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IdFor<NPTGLocality> getLocalityId() {
        // note: if needed since know grid for locality and for postcode could associate
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
        return LocationType.Postcode;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return DataSourceID.postcode;
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
    public boolean servesRoutePickup(Route route) {
        // TODO is this correct
        return false;
    }

    @Override
    public boolean servesRouteDropOff(Route route) {
        // TODO is this correct
        return false;
    }

    @Override
    public boolean isMarkedInterchange() {
        return false;
    }

    @Override
    public LocationId<PostcodeLocation> getLocationId() {
        return id.getLocationId();
    }

    @Override
    public GraphLabel getNodeLabel() {
        throw new RuntimeException("Not implemented for postcode");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeLocation that = (PostcodeLocation) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public GraphPropertyKey getProp() {
        throw new RuntimeException("not graph property key for PostcodeLocation");
    }

    @Override
    public String toString() {
        return "PostcodeLocation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", gridPosition=" + gridPosition +
                '}';
    }

}
