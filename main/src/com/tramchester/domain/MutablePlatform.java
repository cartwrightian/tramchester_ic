package com.tramchester.domain;

import com.google.common.collect.Streams;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class MutablePlatform implements Platform {

    private final LocationId<Platform> id;
    private final String name;
    private final String platformNumber;
    private final LatLong latLong;
    private final Set<Route> servesRoutesPickup;
    private final Set<Route> servesRoutesDropoff;
    private final DataSourceID dataSourceId;
    private final IdFor<NPTGLocality> localityId;
    private final boolean isMarkedInterchange;
    private final GridPosition gridPosition;
    private final Station station;

    public MutablePlatform(IdFor<Platform> id, Station station, String platformName, DataSourceID dataSourceId, String platformNumber,
                           IdFor<NPTGLocality> localityId, LatLong latLong, GridPosition gridPosition, boolean isMarkedInterchange) {
        this.id = LocationId.wrap(id);
        this.station = station;
        this.dataSourceId = dataSourceId;
        this.platformNumber = platformNumber;
        this.localityId = localityId;
        this.gridPosition = gridPosition;
        this.isMarkedInterchange = isMarkedInterchange;
        this.name = format("%s platform %s", platformName, platformNumber);
        this.latLong = latLong;
        servesRoutesPickup = new HashSet<>();
        servesRoutesDropoff = new HashSet<>();

    }

    /***
     * For testing ONLY
     * @param id the platform id
     * @param station the parent station
     * @param latLong the position
     * @param dataSourceId the source
     * @param localityId the nptg locality code
     * @return Platform for testing only
     */
    public static MutablePlatform buildForTFGMTram(PlatformId id, Station station, LatLong latLong, DataSourceID dataSourceId,
                                            IdFor<NPTGLocality> localityId) {
        String platformNumber = id.getNumber();
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);
        boolean isMarkedInterchange = false;
        return new MutablePlatform(id, station, station.getName(), dataSourceId, platformNumber,
                localityId, latLong, gridPosition, isMarkedInterchange);
    }

    public static Platform buildForTFGMTram(String platformNummber, Station station, LatLong latLong, DataSourceID dataSourceId,
                                            IdFor<NPTGLocality> areaId) {
        PlatformId platformId = PlatformId.createId(station, platformNummber);
        return buildForTFGMTram(platformId, station, latLong, dataSourceId, areaId);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LocationId<Platform> getLocationId() {
        return id;
    }

    @Override
    public IdFor<Platform> getId() {
        return id.getId();
    }

    @Override
    public String toString() {
        return "Platform{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", platformNumber='" + platformNumber + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutablePlatform platform = (MutablePlatform) o;

        return id.equals(platform.id);
    }

    @Override
    public String getPlatformNumber() {
        return platformNumber;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public Station getStation() {
        return station;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.PLATFORM_ID;
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }

    @Override
    public IdFor<NPTGLocality> getLocalityId() {
        return localityId;
    }

    @Override
    public boolean hasPlatforms() {
        return false;
    }

    @Override
    public Set<Platform> getPlatforms() {
        return Collections.emptySet();
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.Platform;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return dataSourceId;
    }

    @Override
    public boolean hasPickup() {
        return !servesRoutesPickup.isEmpty();
    }

    @Override
    public boolean hasDropoff() {
        return !servesRoutesDropoff.isEmpty();
    }

    @Override
    public boolean isActive() {
        return hasDropoff() || hasPickup();
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return servesRoutesDropoff;
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return servesRoutesPickup;
    }

    @Override
    public boolean servesRoutePickup(final Route route) {
        return servesRoutesPickup.contains(route);
    }

    @Override
    public boolean servesRouteDropOff(Route route) {
        return servesRoutesDropoff.contains(route);
    }

    @Override
    public boolean isMarkedInterchange() {
        return isMarkedInterchange;
    }

    @Override
    public boolean containsOthers() {
        return false;
    }

    @Override
    public LocationSet<Station> getAllContained() {
        throw new RuntimeException("Not implemented");
    }

    public MutablePlatform addRouteDropOff(final Route route) {
        servesRoutesDropoff.add(route);
        return this;
    }

    public void addRoutePickUp(Route route) {
        servesRoutesPickup.add(route);
    }

    @Override
    public GraphLabel getNodeLabel() {
        return GraphLabel.PLATFORM;
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        Set<TransportMode> modes = Streams.concat(servesRoutesDropoff.stream(), servesRoutesPickup.stream()).
                map(Route::getTransportMode).collect(Collectors.toSet());
        if (modes.isEmpty()) {
            return EnumSet.noneOf(TransportMode.class);
        }
        return EnumSet.copyOf(modes);
    }

}
