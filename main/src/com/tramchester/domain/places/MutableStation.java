package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MutableStation implements Station {

    // TODO into config?
    public static final int DEFAULT_MIN_CHANGE_TIME = 1;

    private final IdFor<NPTGLocality> localityId;
    private final IdFor<Station> id;
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private final Set<Platform> platforms;
    private final Set<Route> servesRoutesPickup;
    private final Set<Route> servesRoutesDropoff;
    private final Set<Route> passedByRoute; // i.e. a station being passed by a train, but the train does not stop
    private final DataSourceID dataSourceID;
    private final boolean isMarkedInterchange;
    private final EnumSet<TransportMode> modes;
    private final Duration changeTimeNeeded;
    private final boolean isCentral;

    public MutableStation(IdFor<Station> id, IdFor<NPTGLocality> localityId, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID, boolean isCentral) {
        // todo default change duration from config for the data source?
        this(id, localityId, stationName, latLong, gridPosition, dataSourceID, false,
                Duration.ofMinutes(DEFAULT_MIN_CHANGE_TIME), isCentral);
    }

    // for some data sources we know if station is an interchange
    public MutableStation(IdFor<Station> id, IdFor<NPTGLocality> localityId, String stationName, LatLong latLong, GridPosition gridPosition,
                          DataSourceID dataSourceID, boolean isMarkedInterchange, Duration changeTimeNeeded, boolean isCentral) {
        this.localityId = localityId;
        this.gridPosition = gridPosition;
        this.dataSourceID = dataSourceID;
        this.isMarkedInterchange = isMarkedInterchange;
        this.changeTimeNeeded = changeTimeNeeded;
        platforms = new HashSet<>();
        servesRoutesPickup = new HashSet<>();
        servesRoutesDropoff = new HashSet<>();
        passedByRoute = new HashSet<>();

        this.id = id;
        this.name = stationName;
        this.latLong = latLong;
        this.isCentral = isCentral;
        modes = EnumSet.noneOf(TransportMode.class);
    }

    public static Station Unknown(final DataSourceID dataSourceID) {
        return new MutableStation(StringIdFor.createId("unknown", Station.class), NPTGLocality.InvalidId(), "Unknown",
                LatLong.Invalid, GridPosition.Invalid, dataSourceID, false, Duration.ZERO, false);
    }

    @Override
    public IdFor<Station> getId() {
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
        return !platforms.isEmpty();
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return modes;
    }

    @Override
    public boolean servesMode(final TransportMode mode) {
        return hasMode(mode, servesRoutesPickup) || hasMode(mode, servesRoutesDropoff);
    }

    private boolean hasMode(final TransportMode mode, final Set<Route> routes) {
        return routes.stream().anyMatch(route -> route.getTransportMode().equals(mode));
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.Station;
    }

    @Override
    public DataSourceID getDataSourceID() {
        return dataSourceID;
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
        return hasPickup() || hasDropoff();
    }

    @Override
    public Set<Platform> getPlatformsForRoute(final Route route) {
        return platforms.stream().
                filter(platform -> platform.getDropoffRoutes().contains(route) || platform.getPickupRoutes().contains(route)).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasPlatform(final IdFor<Platform> platformId) {
        return platforms.stream().map(Platform::getId).anyMatch(id -> id.equals(platformId));
    }

    @Override
    public boolean isCentral() {
        return isCentral;
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
    public Set<Platform> getPlatforms() {
        return  Collections.unmodifiableSet(platforms);
    }

    @Override
    public boolean servesRoutePickup(final Route route) {
        return servesRoutesPickup.contains(route);
    }

    @Override
    public boolean servesRouteDropOff(final Route route) {
        return servesRoutesDropoff.contains(route);
    }

    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.STATION_ID;
    }

    @Override
    public GraphLabel getNodeLabel() {
        return GraphLabel.STATION;
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

    @Override
    public Duration getMinChangeDuration() {
        return changeTimeNeeded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (!(o instanceof MutableStation)) return false;

        MutableStation station = (MutableStation) o;

        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Station{" +
                "areaId='" + localityId + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", mode=" + getTransportModes() +
                ", platforms=" + HasId.asIds(platforms) +
                ", servesRoutesPickup=" + servesRoutesPickup +
                ", servesRoutesDropoff=" + servesRoutesDropoff +
                ", passedByRoute=" + HasId.asIds(passedByRoute) +
                ", isCentral=" + isCentral +
                ", isMarkedInterchange=" + isMarkedInterchange +
                '}';
    }

    public MutableStation addPlatform(Platform platform) {
        platforms.add(platform);
        return this;
    }

    public void addRouteDropOff(final Route dropoffFromRoute) {
        modes.add(dropoffFromRoute.getTransportMode());
        servesRoutesDropoff.add(dropoffFromRoute);
    }

    public void addRoutePickUp(final Route pickupFromRoute) {
        modes.add(pickupFromRoute.getTransportMode());
        servesRoutesPickup.add(pickupFromRoute);
    }

    public void addMode(TransportMode mode) {
        modes.add(mode);
    }

}
