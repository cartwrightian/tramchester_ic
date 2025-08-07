package com.tramchester.graph.core;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.GraphLabel;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.reference.TransportRelationshipTypes.TO_SERVICE;

public abstract class GraphNodeProperties<T extends GraphEntityProperties.GraphProps> extends GraphEntityProperties<T> implements MutableGraphNode {

    private final T node;

    public GraphNodeProperties(T node) {
        this.node = node;
    }

    @Override
    public void setHourProp(final Integer hour) {
        node.setProperty(HOUR.getText(), hour);
        invalidateCache();
    }

    protected abstract void invalidateCache();

    @Override
    public void setTime(final TramTime tramTime) {
        setTime(tramTime, node);
        invalidateCache();
    }

    @Override
    public void set(final Station station) {
        set(station, node);
        invalidateCache();
    }

    @Override
    public void set(final Platform platform) {
        set(platform, node);
        invalidateCache();
    }

    @Override
    public void set(final Route route) {
        set(route, node);
        invalidateCache();
    }

    @Override
    public void set(final Service service) {
        set(service, node);
        invalidateCache();
    }

    @Override
    public void set(final StationLocalityGroup stationGroup) {
        set(stationGroup, node);
        invalidateCache();
    }

    @Override
    public void set(final RouteStation routeStation) {
        set(routeStation, node);
        invalidateCache();
    }

    @Override
    public void setTransportMode(final TransportMode first) {
        node.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
        invalidateCache();
    }

    @Override
    public void set(final DataSourceInfo nameAndVersion) {
        final DataSourceID sourceID = nameAndVersion.getID();
        node.setProperty(sourceID.name(), nameAndVersion.getVersion());
        invalidateCache();
    }

    @Override
    public void setLatLong(final LatLong latLong) {
        node.setProperty(LATITUDE.getText(), latLong.getLat());
        node.setProperty(LONGITUDE.getText(), latLong.getLon());
        invalidateCache();
    }

    @Override
    public void setBounds(final BoundingBox bounds) {
        node.setProperty(MAX_EASTING.getText(), bounds.getMaxEasting());
        node.setProperty(MAX_NORTHING.getText(), bounds.getMaxNorthings());
        node.setProperty(MIN_EASTING.getText(), bounds.getMinEastings());
        node.setProperty(MIN_NORTHING.getText(), bounds.getMinNorthings());
        invalidateCache();
    }

    @Override
    public void setWalkId(final LatLong origin, final UUID uid) {
        node.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
        invalidateCache();
    }

    @Override
    public void setPlatformNumber(final Platform platform) {
        node.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
        invalidateCache();
    }

    @Override
    public void setSourceName(final String sourceName) {
        node.setProperty(SOURCE_NAME_PROP.getText(), sourceName);
        invalidateCache();
    }

    @Override
    public void setAreaId(final IdFor<NPTGLocality> localityId) {
        node.setProperty(AREA_ID.getText(), localityId.getGraphId());
        invalidateCache();
    }

    @Override
    public void setTowards(final IdFor<Station> stationId) {
        node.setProperty(TOWARDS_STATION_ID.getText(), stationId.getGraphId());
        invalidateCache();
    }

    ///// GET //////////////////////////////////////////////////

    // NOTE: Transaction closed exceptions will occur if keep reference to node beyond lifetime of the original transaction


    @Override
    public TransportMode getTransportMode() {
        short number = (short) getProperty(TRANSPORT_MODE, node);
        return TransportMode.fromNumber(number);
    }

    @Override
    public int getHour() {
        return GraphLabel.getHourFrom(getLabels());
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, node);
    }

    @Override
    public void set(final Trip trip) {
        set(trip, node);
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(node);
    }

    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(node);
    }

    public IdFor<Service> getServiceId() {
        return getIdFor(Service.class, node);
    }

    @Override
    public IdFor<Route> getRouteId() {
        return getIdFor(Route.class, node);
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return getIdFor(StationLocalityGroup.class, node);
    }

    @Override
    public IdFor<NPTGLocality> getAreaId() {
        return getIdFor(NPTGLocality.class, node);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, node);
    }

    public TramTime getTime() {
        return getTime(node);
    }

    public LatLong getLatLong() {
        final double lat = (double) getProperty(LATITUDE, node);
        final double lon = (double) getProperty(LONGITUDE, node);
        return new LatLong(lat, lon);
    }

    public boolean hasTripId() {
        return hasIdFor(Trip.class);
    }

    public PlatformId getPlatformId() {
        final IdFor<Station> stationId = getStationId();
        final String platformNumber =  node.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public boolean hasStationId() {
        return hasIdFor(Station.class);
    }

    <DT extends CoreDomain> Boolean hasIdFor(Class<DT> theClass) {
        return node.hasProperty(GraphPropertyKey.getFor(theClass).getText());
    }

    @Override
    public IdFor<Station> getTowardsStationId() {
        String text = (String) node.getProperty(TOWARDS_STATION_ID.getText());
        if (text==null) {
            return Station.InvalidId();
        }
        return Station.createId(text);
    }

    @Override
    public BoundingBox getBounds() {
        int minEasting = (int) node.getProperty(MIN_EASTING.getText());
        int minNorthing = (int) node.getProperty(MIN_NORTHING.getText());

        int maxEasting = (int) node.getProperty(MAX_EASTING.getText());
        int maxNorthing = (int) node.getProperty(MAX_NORTHING.getText());
        return new BoundingBox(minEasting, minNorthing, maxEasting, maxNorthing);
    }

    @Override
    public boolean hasOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE).
                anyMatch(relationship -> relationship.hasTripIdInList(tripId));
    }

    //public abstract Stream<ImmutableGraphRelationship> getRelationships(GraphTransaction txn, GraphDirection graphDirection, TransportRelationshipTypes transportRelationshipTypes);

    @Override
    public Stream<ImmutableGraphRelationship> getOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE).
                filter(relationship -> relationship.hasTripIdInList(tripId));
    }
}
