package com.tramchester.graph.core;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.reference.GraphLabel;

import java.util.UUID;
import java.util.stream.Stream;

public interface MutableGraphNode extends GraphNode {
    void delete();

    MutableGraphRelationship createRelationshipTo(MutableGraphTransaction txn, MutableGraphNode end,
                                                  TransportRelationshipTypes relationshipType);

    void addLabel(GraphLabel label);

    void setHourProp(Integer hour);

    void setTime(TramTime tramTime);

    void set(Station station);

    void set(Platform platform);

    void set(Route route);

    void set(Service service);

    void set(StationLocalityGroup stationGroup);

    void set(RouteStation routeStation);

    void setTransportMode(TransportMode first);

    void set(DataSourceInfo nameAndVersion);

    void setLatLong(LatLong latLong);

    void setBounds(BoundingBox bounds);

    void setWalkId(LatLong origin, UUID uid);

    void setPlatformNumber(Platform platform);

    void setSourceName(String sourceName);

    void setAreaId(IdFor<NPTGLocality> localityId);

    void setTowards(IdFor<Station> stationId);

    void set(Trip trip);

    Stream<MutableGraphRelationship> getRelationshipsMutable(MutableGraphTransaction txn, GraphDirection direction,
                                                             TransportRelationshipTypes relationshipType);

    MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction tx,
                                                          TransportRelationshipTypes transportRelationshipTypes, GraphDirection graphDirection);
}
