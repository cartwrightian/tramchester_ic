package com.tramchester.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.GraphLabel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StationGroup implements Location<StationGroup> {
    private final LocationSet<Station> groupedStations;

    private final LatLong latLong;
    private final DataSourceID dataSourceId;
    private final String name;
    private final IdFor<NPTGLocality> localityId;
    private final LocationId<StationGroup> id;


    public StationGroup(final LocationSet<Station> groupedStations, final String name, final LatLong latLong,
                        final IdFor<NPTGLocality> localityId) {
        this.localityId = localityId;
        this.id = LocationId.wrap(createId(localityId));

        if (groupedStations.isEmpty()) {
            throw new RuntimeException("Attempt to create empty group for name: " +name);
        }
        this.groupedStations = groupedStations;
        this.latLong = latLong;
        this.name = name;
        this.dataSourceId = computeDataSourceId(groupedStations);
    }

    @NotNull
    public static IdFor<StationGroup> createId(final IdFor<NPTGLocality> localityId) {
        return StringIdFor.convert(localityId, StationGroup.class);
    }

    private static DataSourceID computeDataSourceId(final LocationSet<Station> stations) {
        final Set<DataSourceID> sourceIds = stations.stream().map(Station::getDataSourceID).collect(Collectors.toSet());
        if (sourceIds.size()!=1) {
            return DataSourceID.mixed;
        }
        return sourceIds.iterator().next();
    }

    @Override
    public IdFor<StationGroup> getId() {
        return id.getId();
    }

    @Override
    public LocationId<StationGroup> getLocationId() {
        return id;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public IdFor<NPTGLocality> getLocalityId() {
        return localityId;
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
    public LatLong getLatLong() {
        return latLong;
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
    public ImmutableEnumSet<TransportMode> getTransportModes() {
        return flattenEnum(Station::getTransportModes);
    }

    @Override
    public boolean anyOverlapWith(final ImmutableEnumSet<TransportMode> modes) {
        return anyMatch(station -> station.anyOverlapWith(modes));
    }

    private boolean anyMatch(final Predicate<Station> predicate) {
        return groupedStations.stream().anyMatch(predicate);
    }

    private <R> Set<R> flatten(final Function<Station, Collection<R>> map) {
        return groupedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
    }

    private <R extends Enum<R>> ImmutableEnumSet<R> flattenEnum(Function<Station, ImmutableEnumSet<R>> map) {
        final Set<R> collect = groupedStations.stream().
                flatMap(station -> map.apply(station).stream()).
                collect(Collectors.toUnmodifiableSet());
        return ImmutableEnumSet.copyOf(collect);
    }

    @Override
    public String toString() {
        return "StationGroup{" +
                "groupedStations=" + HasId.asIds(groupedStations) +
                ", latLong=" + latLong +
                ", dataSourceId=" + dataSourceId +
                ", name='" + name + '\'' +
                ", localityId=" + localityId +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StationGroup that = (StationGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
