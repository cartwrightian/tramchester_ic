package com.tramchester.graph.core;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
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
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;

import java.util.UUID;

public interface MutableGraphNode extends GraphNode {
    void delete(MutableGraphTransaction txn);

    MutableGraphRelationship createRelationshipTo(MutableGraphTransaction txn, MutableGraphNode end,
                                                  TransportRelationshipTypes relationshipType);

    void addLabel(MutableGraphTransaction tx, GraphLabel label);

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

    MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction tx,
                                                          TransportRelationshipTypes transportRelationshipTypes, GraphDirection graphDirection);

    MutableGraphRelationship getSingleRelationshipMutable(MutableGraphTransaction tx,
                                                          TransportRelationshipTypes transportRelationshipTypes,
                                                          GraphDirection graphDirection, GraphNode end);

    void setStartDate(TramDate date);
}
