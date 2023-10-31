package com.tramchester.graph;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphRelationship extends HaveGraphProperties {
    private final Relationship relationship;

    GraphRelationship(Relationship relationship) {
        this.relationship = relationship;
    }

    // TODO facade Path?
    public static GraphRelationship lastFrom(Path path) {
        Relationship relationship = path.lastRelationship();
        if (relationship==null) {
            return null;
        }
        return new GraphRelationship(relationship);
    }

//    public static Iterable<Relationship> toRelationshipIterable(Stream<GraphRelationship> stream) {
//        Iterator<Relationship> iterator = stream.map(graphRelationship -> graphRelationship.relationship).iterator();
//        return new IteratorIterable<>(iterator);
//    }

    public static ResourceIterable<Relationship> convertIterable(Stream<GraphRelationship> resourceIterable) {
        Iterator<Relationship> mapped = resourceIterable.map(graphRelationship -> graphRelationship.relationship).iterator();
        return new ResourceIterable<>() {
            @Override
            public ResourceIterator<Relationship> iterator() {
                return new ResourceIterator<>() {
                    @Override
                    public void close() {
                        // no-op
                    }

                    @Override
                    public boolean hasNext() {
                        return mapped.hasNext();
                    }

                    @Override
                    public Relationship next() {
                        return mapped.next();
                    }
                };
            }

            @Override
            public void close() {
                //
            }
        };
    }

    @Deprecated
    public long getIdOLD() {
        return relationship.getId();
    }

    public GraphRelationshipId getId() {
        return new GraphRelationshipId(relationship.getId());
    }

    public void setCost(Duration cost) {
        int minutes = GraphProps.roundUpNearestMinute(cost);
        relationship.setProperty(COST.getText(), minutes);
    }

    public void setTransportMode(TransportMode transportMode) {
        relationship.setProperty(TRANSPORT_MODE.getText(), transportMode.getNumber());
    }

    public void setMaxCost(Duration duration) {
        int minutes = GraphProps.roundUpNearestMinute(duration);
        relationship.setProperty(MAX_COST.getText(), minutes);
    }

    public void setTime(TramTime tramTime) {
        setTime(tramTime, relationship);
    }

    public TramTime getTime() {
        return getTime(relationship);
    }

    public <C extends GraphProperty & CoreDomain & HasId<C>> void set(C domainItem) {
        GraphProps.setProperty(relationship, domainItem);
    }

    public void setHour(int hour) {
        relationship.setProperty(HOUR.getText(), hour);
    }

    public void setRouteStationId(IdFor<RouteStation> routeStationId) {
        relationship.setProperty(ROUTE_STATION_ID.getText(), routeStationId.getGraphId());
    }

    public void setStopSeqNum(int sequenceNumber) {
        relationship.setProperty(STOP_SEQ_NUM.getText(), sequenceNumber);
    }

    public void setDateRange(DateRange range) {
        setStartDate(range.getStartDate().toLocalDate());
        setEndDate(range.getEndDate().toLocalDate());
    }

    public void setEndDate(LocalDate localDate) {
        relationship.setProperty(END_DATE.getText(), localDate);
    }

    public void setStartDate(LocalDate localDate) {
        relationship.setProperty(START_DATE.getText(), localDate);
    }

    public void addTransportMode(TransportMode mode) {
        short modeNumber = mode.getNumber();
        if (!(relationship.hasProperty(TRANSPORT_MODES.getText()))) {
            relationship.setProperty(TRANSPORT_MODES.getText(), new short[]{modeNumber});
            return;
        }

        short[] existing = (short[]) relationship.getProperty(TRANSPORT_MODES.getText());
        for (short value : existing) {
            if (value == modeNumber) {
                return;
            }
        }

        short[] replacement = Arrays.copyOf(existing, existing.length + 1);
        replacement[existing.length] = modeNumber;
        relationship.setProperty(TRANSPORT_MODES.getText(), replacement);
    }

    public Duration getCost() {
        final int value = (int) getProperty(relationship, COST);
        return Duration.ofMinutes(value);
    }

    private static Object getProperty(Entity entity, GraphPropertyKey key) {
        return entity.getProperty(key.getText());
    }

    public GraphNode getEndNode() {
        return new GraphNode(relationship.getEndNode());
    }

    public void delete() {
        relationship.delete();
    }

    public EnumSet<TransportMode> getTransportModes() {
        if (!relationship.hasProperty(TRANSPORT_MODES.getText())) {
            return EnumSet.noneOf(TransportMode.class);
        }

        short[] existing = (short[]) relationship.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    public TransportRelationshipTypes getType() {
        return TransportRelationshipTypes.valueOf(relationship.getType().name());
    }

    public GraphNode getStartNode() {
        return new GraphNode(relationship.getStartNode());
    }

    public IdFor<Route> getRouteId() {
        return getIdFor(Route.class, relationship);
    }

    public IdFor<Service> getServiceId() {
        return getIdFor(Service.class, relationship);
    }

    public IdFor<Trip> getTripId() {
        return getIdFor(Trip.class, relationship);
    }

    public boolean isType(TransportRelationshipTypes transportRelationshipType) {
        return relationship.isType(transportRelationshipType);
    }

    public IdFor<RouteStation> getRouteStationId() {
        return getRouteStationId(relationship);
    }

    public Map<String,Object> getAllProperties() {
        return getAllProperties(relationship);
    }

    public boolean isDayOffset() {
        // todo should this be checking if set instead?
        return (Boolean) relationship.getProperty(DAY_OFFSET.getText());
    }

    public boolean validOn(TramDate tramDate) {
        LocalDate localDate = tramDate.toLocalDate();
        LocalDate startDate = getStartDate();
        if (localDate.isBefore(startDate)) {
            return false;
        }
        LocalDate endDate = getEndDate();
        if (localDate.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private LocalDate getEndDate() {
        return (LocalDate) relationship.getProperty(END_DATE.getText());
    }

    private LocalDate getStartDate() {
        return (LocalDate) relationship.getProperty(START_DATE.getText());
    }

    public IdFor<Station> getStationId() {
        return getIdFor(Station.class, relationship);
    }

    public boolean hasProperty(GraphPropertyKey propertyKey) {
        return relationship.hasProperty(propertyKey.getText());
    }

    public int getStopSeqNumber() {
        return (int) relationship.getProperty(STOP_SEQ_NUM.getText());
    }
}
