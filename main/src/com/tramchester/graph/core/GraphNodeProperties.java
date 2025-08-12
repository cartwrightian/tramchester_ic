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

    private final T graphProps;

    public GraphNodeProperties(T graphProps) {
        this.graphProps = graphProps;
    }

    @Override
    public void setHourProp(final Integer hour) {
        graphProps.setProperty(HOUR.getText(), hour);
        invalidateCache();
    }

    protected abstract void invalidateCache();

    @Override
    public void setTime(final TramTime tramTime) {
        setTime(tramTime, graphProps);
        invalidateCache();
    }

    @Override
    public void set(final Station station) {
        set(station, graphProps);
        invalidateCache();
    }

    @Override
    public void set(final Platform platform) {
        set(platform, graphProps);
        invalidateCache();
    }

    @Override
    public void set(final Route route) {
        set(route, graphProps);
        invalidateCache();
    }

    @Override
    public void set(final Service service) {
        set(service, graphProps);
        invalidateCache();
    }

    @Override
    public void set(final StationLocalityGroup stationGroup) {
        set(stationGroup, graphProps);
        invalidateCache();
    }

    @Override
    public void set(final RouteStation routeStation) {
        set(routeStation, graphProps);
        invalidateCache();
    }

    @Override
    public void setTransportMode(final TransportMode first) {
        graphProps.setProperty(TRANSPORT_MODE.getText(), first.getNumber());
        invalidateCache();
    }

    @Override
    public void set(final DataSourceInfo nameAndVersion) {
        final DataSourceID sourceID = nameAndVersion.getID();
        graphProps.setProperty(sourceID.name(), nameAndVersion.getVersion());
        invalidateCache();
    }

    @Override
    public void setLatLong(final LatLong latLong) {
        graphProps.setProperty(LATITUDE.getText(), latLong.getLat());
        graphProps.setProperty(LONGITUDE.getText(), latLong.getLon());
        invalidateCache();
    }

    @Override
    public void setBounds(final BoundingBox bounds) {
        graphProps.setProperty(MAX_EASTING.getText(), bounds.getMaxEasting());
        graphProps.setProperty(MAX_NORTHING.getText(), bounds.getMaxNorthings());
        graphProps.setProperty(MIN_EASTING.getText(), bounds.getMinEastings());
        graphProps.setProperty(MIN_NORTHING.getText(), bounds.getMinNorthings());
        invalidateCache();
    }

    @Override
    public void setWalkId(final LatLong origin, final UUID uid) {
        graphProps.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
        invalidateCache();
    }

    @Override
    public void setPlatformNumber(final Platform platform) {
        graphProps.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
        invalidateCache();
    }

    @Override
    public void setSourceName(final String sourceName) {
        graphProps.setProperty(SOURCE_NAME_PROP.getText(), sourceName);
        invalidateCache();
    }

    @Override
    public void setAreaId(final IdFor<NPTGLocality> localityId) {
        graphProps.setProperty(AREA_ID.getText(), localityId.getGraphId());
        invalidateCache();
    }

    @Override
    public void setTowards(final IdFor<Station> stationId) {
        graphProps.setProperty(TOWARDS_STATION_ID.getText(), stationId.getGraphId());
        invalidateCache();
    }

    ///// GET //////////////////////////////////////////////////

    // NOTE: Transaction closed exceptions will occur if keep reference to node beyond lifetime of the original transaction


    @Override
    public TransportMode getTransportMode() {
        short number = (short) getProperty(TRANSPORT_MODE, graphProps);
        return TransportMode.fromNumber(number);
    }

    @Override
    public int getHour() {
        return GraphLabel.getHourFrom(getLabels());
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, graphProps);
    }

    @Override
    public void set(final Trip trip) {
        set(trip, graphProps);
    }

    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(graphProps);
    }

    public IdFor<Service> getServiceId() {
        return getIdFor(Service.class, graphProps);
    }

    @Override
    public IdFor<Route> getRouteId() {
        return getIdFor(Route.class, graphProps);
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return getIdFor(StationLocalityGroup.class, graphProps);
    }

    @Override
    public IdFor<NPTGLocality> getAreaId() {
        return getIdFor(NPTGLocality.class, graphProps);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, graphProps);
    }

    public TramTime getTime() {
        return getTime(graphProps);
    }

    public LatLong getLatLong() {
        final double lat = (double) getProperty(LATITUDE, graphProps);
        final double lon = (double) getProperty(LONGITUDE, graphProps);
        return new LatLong(lat, lon);
    }

    public boolean hasTripId() {
        return hasIdFor(Trip.class);
    }

    public PlatformId getPlatformId() {
        final IdFor<Station> stationId = getStationId();
        final String platformNumber =  graphProps.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public boolean hasStationId() {
        return hasIdFor(Station.class);
    }

    <DT extends CoreDomain> Boolean hasIdFor(Class<DT> theClass) {
        return graphProps.hasProperty(GraphPropertyKey.getFor(theClass).getText());
    }

    @Override
    public IdFor<Station> getTowardsStationId() {
        String text = (String) graphProps.getProperty(TOWARDS_STATION_ID.getText());
        if (text==null) {
            return Station.InvalidId();
        }
        return Station.createId(text);
    }

    @Override
    public BoundingBox getBounds() {
        int minEasting = (int) graphProps.getProperty(MIN_EASTING.getText());
        int minNorthing = (int) graphProps.getProperty(MIN_NORTHING.getText());

        int maxEasting = (int) graphProps.getProperty(MAX_EASTING.getText());
        int maxNorthing = (int) graphProps.getProperty(MAX_NORTHING.getText());
        return new BoundingBox(minEasting, minNorthing, maxEasting, maxNorthing);
    }

    @Override
    public boolean hasOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE).
                anyMatch(relationship -> relationship.hasTripIdInList(tripId));
    }

    @Override
    public Stream<GraphRelationship> getOutgoingServiceMatching(final GraphTransaction txn, final IdFor<Trip> tripId) {
        return getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE).
                filter(relationship -> relationship.hasTripIdInList(tripId));
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(graphProps);
    }

    public boolean hasProperty(final GraphPropertyKey propertyKey) {
        return graphProps.hasProperty(propertyKey.getText());
    }

    public Object getProperty(final GraphPropertyKey propertyKey) {
        return graphProps.getProperty(propertyKey.getText());
    }
}
