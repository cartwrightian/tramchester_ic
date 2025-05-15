package com.tramchester.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/***
 * Stations grouped together as in same nptg locality, use as starting or end point for a journey
 * see also class: com.tramchester.graph.GraphQuery::getGroupedNode
 */
public class StationLocalityGroup implements Location<StationLocalityGroup> {
    private final LocationId<StationLocalityGroup> id;
    private final IdFor<NPTGLocality> localityId;
    private final LocationSet<Station> groupedStations;
    private final String name;

    private final LatLong latLong;
    private final DataSourceID dataSourceId;
    private final IdFor<StationLocalityGroup> parentId;

    public StationLocalityGroup(final Set<Station> groupedStations, final IdFor<NPTGLocality> localityId, final String name,
                                final IdFor<NPTGLocality> parentId, final LatLong latLong) {
        if (groupedStations.isEmpty()) {
            throw new RuntimeException("Attempt to create empty group for " + localityId + " name name " +name);
        }
        this.id = LocationId.wrap(createId(localityId));
        if (parentId.isValid()) {
            this.parentId = createId(parentId);
        } else {
            this.parentId = StringIdFor.invalid(StationLocalityGroup.class);
        }
        this.latLong = latLong;
        this.dataSourceId = computeDataSourceId(groupedStations);
        this.groupedStations = LocationSet.of(groupedStations);
        this.localityId = localityId;
        this.name = name;
    }

    @NotNull
    public static IdFor<StationLocalityGroup> createId(final IdFor<NPTGLocality> localityId) {
        return StringIdFor.convert(localityId, StationLocalityGroup.class);
    }

    public static IdFor<StationLocalityGroup> createId(String text) {
        return StringIdFor.createId(text, StationLocalityGroup.class);
    }

    @Override
    public LocationSet<Station> getAllContained() {
        return new LocationSet<>(groupedStations);
    }

    @Override
    public boolean containsOthers() {
        return !groupedStations.isEmpty();
    }

    @Override
    public Set<Platform> getPlatforms() {
        return flatten(Station::getPlatforms);
    }

    @Override
    public IdFor<StationLocalityGroup> getId() {
        return id.getId();
    }

    @Override
    public LocationId<StationLocalityGroup> getLocationId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public IdFor<NPTGLocality> getLocalityId() {
        return localityId;
    }

    @Override
    public boolean hasPlatforms() {
        return anyMatch(Station::hasPlatforms);
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return flatten(Station::getDropoffRoutes);
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return flatten(Station::getPickupRoutes);
    }

    @Override
    public boolean servesRoutePickup(final Route route) {
        return anyMatch(station -> station.servesRoutePickup(route));
    }

    @Override
    public boolean servesRouteDropOff(final Route route) {
        return anyMatch(station -> station.servesRouteDropOff(route));
    }

    @Override
    public boolean isMarkedInterchange() {
        return anyMatch(Station::isMarkedInterchange);
    }

    @Override
    public GridPosition getGridPosition() {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.STATION_GROUP_ID;
    }

    @Override
    public GraphLabel getNodeLabel() {
        return GraphLabel.GROUPED;
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.StationGroup;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return dataSourceId;
    }

    @Override
    public boolean hasPickup() {
        return anyMatch(Location::hasPickup);
    }

    @Override
    public boolean hasDropoff() {
        return anyMatch(Location::hasDropoff);
    }

    @Override
    public boolean isActive() {
        return anyMatch(Location::isActive);
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        final Set<TransportMode> transportModes = flatten(Station::getTransportModes);
        return EnumSet.copyOf(transportModes);
    }

    @Override
    public boolean anyOverlapWith(final EnumSet<TransportMode> modes) {
        return anyMatch(station -> station.anyOverlapWith(modes));
    }

    private boolean anyMatch(final Predicate<Station> predicate) {
        return groupedStations.stream().anyMatch(predicate);
    }

    private <R> Set<R> flatten(Function<Station, Collection<R>> map) {
        return groupedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
    }


    private static DataSourceID computeDataSourceId(Set<Station> stations) {
        Set<DataSourceID> sourceIds = stations.stream().map(Station::getDataSourceID).collect(Collectors.toSet());
        if (sourceIds.size()!=1) {
            throw new RuntimeException("Composite stations must call come from same datasource, stations: " + stations);
        }
        return sourceIds.iterator().next();
    }

    @Override
    public String toString() {
        return "GroupedStations{" +
                " id=" + id +
                ", areaId=" + localityId +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", dataSourceId=" + dataSourceId +
                ", parentId=" + parentId +
                ", groupedStations=" + HasId.asIds(groupedStations) +
                '}';
    }

    public IdFor<StationLocalityGroup> getParentId() {
        return parentId;
    }

    public boolean hasParent() {
        return parentId.isValid();
    }
}
