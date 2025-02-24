package com.tramchester.graph.facade;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.HaveGraphProperties;
import com.tramchester.graph.TransportRelationshipTypes;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.graph.GraphPropertyKey.*;

public class MutableGraphRelationship extends HaveGraphProperties implements GraphRelationship {

    private final Relationship relationship;
    private final GraphRelationshipId id;

    private static final String tripIdProperty = TRIP_ID_LIST.getText();

    private ImmutableGraphNode endNode;

    MutableGraphRelationship(final Relationship relationship, final GraphRelationshipId id) {
        this.relationship = relationship;
        this.id = id;
    }

    public GraphRelationshipId getId() {
        return id;
    }

    public void setTransportMode(final TransportMode transportMode) {
        relationship.setProperty(TRANSPORT_MODE.getText(), transportMode.getNumber());
    }

    public void setCost(final Duration cost) {
        final long seconds = cost.toSeconds();
        relationship.setProperty(COST.getText(), seconds);
    }

    public Duration getCost() {
        final long seconds = (long) relationship.getProperty(COST.getText());
        return Duration.ofSeconds(seconds);
    }

    public void setTime(final TramTime tramTime) {
        setTime(tramTime, relationship);
    }

    public void setHour(final int hour) {
        relationship.setProperty(HOUR.getText(), hour);
    }

    public int getHour() {
        return (int) relationship.getProperty(HOUR.getText());
    }

    public <C extends GraphProperty & CoreDomain & HasId<C>> void set(final C domainItem) {
        super.set(domainItem, relationship);
    }

    public TramTime getTime() {
        return getTime(relationship);
    }

    public void setRouteStationId(final IdFor<RouteStation> routeStationId) {
        relationship.setProperty(ROUTE_STATION_ID.getText(), routeStationId.getGraphId());
    }

    public void setStopSeqNum(final int sequenceNumber) {
        relationship.setProperty(STOP_SEQ_NUM.getText(), sequenceNumber);
    }

    public void setDateTimeRange(final DateTimeRange dateTimeRange) {
        final DateRange dateRange = dateTimeRange.getDateRange();
        final TimeRange timeRange = dateTimeRange.getTimeRange();

        setDateRange(dateRange);
        setTimeRange(timeRange);
    }

    public void setDateRange(final DateRange range) {
        setStartDate(range.getStartDate().toLocalDate());
        setEndDate(range.getEndDate().toLocalDate());
    }

    public DateRange getDateRange() {
        final LocalDate start = getStartDate();
        final LocalDate end = getEndDate();
        return new DateRange(TramDate.of(start), TramDate.of(end));
    }

    public void setTimeRange(final TimeRange timeRange) {
        if (timeRange.allDay()) {
            relationship.setProperty(ALL_DAY.getText(), "");
            relationship.removeProperty(START_TIME.getText());
            relationship.removeProperty(END_TIME.getText());
        } else {
            setStartTime(timeRange.getStart());
            setEndTime(timeRange.getEnd());
            relationship.removeProperty(ALL_DAY.getText());
        }
    }

    @Override
    public TimeRange getTimeRange() {
        if (relationship.hasProperty(ALL_DAY.getText())) {
            return TimeRange.AllDay();
        } else {
            final TramTime start = getStartTime();
            final TramTime end = getEndTime();
            return TimeRange.of(start, end);
        }
    }

    @Override
    public DateTimeRange getDateTimeRange() {
        final DateRange dateRange = getDateRange();
        final TimeRange timeRange = getTimeRange();
        return DateTimeRange.of(dateRange, timeRange);
    }

    public void setStartTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for start time next");
        }
        relationship.setProperty(START_TIME.getText(), tramTime.asLocalTime());
    }

    public void setEndTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for end time next");
        }
        relationship.setProperty(END_TIME.getText(), tramTime.asLocalTime());
    }

    public void setEndDate(final LocalDate localDate) {
        relationship.setProperty(END_DATE.getText(), localDate);
    }

    public void setStartDate(final LocalDate localDate) {
        relationship.setProperty(START_DATE.getText(), localDate);
    }

    public void addTransportMode(final TransportMode mode) {
        final short modeNumber = mode.getNumber();
        if (!(relationship.hasProperty(TRANSPORT_MODES.getText()))) {
            relationship.setProperty(TRANSPORT_MODES.getText(), new short[]{modeNumber});
            return;
        }

        final short[] existing = (short[]) relationship.getProperty(TRANSPORT_MODES.getText());
        // note: not sorted, hence not binary search here
        for (short value : existing) {
            if (value == modeNumber) {
                return;
            }
        }

        final short[] replacement = Arrays.copyOf(existing, existing.length + 1);
        replacement[existing.length] = modeNumber;
        relationship.setProperty(TRANSPORT_MODES.getText(), replacement);
    }


    public void addTripId(final IdFor<Trip> tripId) {
        final String text = tripId.getGraphId();
        if (!(relationship.hasProperty(tripIdProperty))) {
            relationship.setProperty(tripIdProperty, new String[]{text});
            return;
        }

        final String[] existing = getTripIdProperty();
        final List<String> existingList = Arrays.asList(existing);
        if (existingList.contains(text)) {
            return;
        }

        final String[] replacement = Arrays.copyOf(existing, existing.length + 1);
        replacement[existing.length] = text;
        Arrays.sort(existing);
        relationship.setProperty(tripIdProperty, replacement);
    }


    public IdSet<Trip> getTripIds() {
        if (!relationship.hasProperty(tripIdProperty)) {
            return IdSet.emptySet();
        }
        final String[] existing = getTripIdProperty();

        return Arrays.stream(existing).map(Trip::createId).collect(IdSet.idCollector());
    }

    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        final String text = tripId.getGraphId();

        if (!relationship.hasProperty(tripIdProperty)) {
            throw new RuntimeException("Unexpected for this relationship " + this);
        }
        final String[] existing = getTripIdProperty();
        // NOTE: assumed sorted
//        return (Arrays.binarySearch(existing, text)>=0);
        final List<String> existingList = Arrays.asList(existing);
        return existingList.contains(text);
    }

    private String[] getTripIdProperty() {
        return (String[]) relationship.getProperty(tripIdProperty);
    }

    public void delete() {
        relationship.delete();
    }

    @Override
    public GraphNode getEndNode(final GraphTransaction txn) {
        if (endNode==null) {
            endNode = txn.getEndNode(relationship);
        }
        return endNode;
    }

    public GraphNode getStartNode(final GraphTransaction txn) {
        return txn.getStartNode(relationship);
    }

    @Override
    public GraphNodeId getStartNodeId(final GraphTransaction txn) {
        return getStartNode(txn).getId();
    }

    @Override
    public GraphNodeId getEndNodeId(final GraphTransaction txn) {
        return getEndNode(txn).getId();
    }

    public EnumSet<TransportMode> getTransportModes() {
        // todo can this be stored direct now?
        if (!relationship.hasProperty(TRANSPORT_MODES.getText())) {
            return EnumSet.noneOf(TransportMode.class);
        }

        final short[] existing = (short[]) relationship.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    public TransportRelationshipTypes getType() {
        return TransportRelationshipTypes.from(relationship);
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

    public boolean validOn(final TramDate tramDate) {
        final LocalDate localDate = tramDate.toLocalDate();
        final LocalDate startDate = getStartDate();
        if (localDate.isBefore(startDate)) {
            return false;
        }
        final LocalDate endDate = getEndDate();
        return !localDate.isAfter(endDate);
    }

    private LocalDate getEndDate() {
        return (LocalDate) relationship.getProperty(END_DATE.getText());
    }

    private LocalDate getStartDate() {
        return (LocalDate) relationship.getProperty(START_DATE.getText());
    }

    @Override
    public TramTime getStartTime() {
        final LocalTime localTime = (LocalTime) relationship.getProperty(START_TIME.getText());
        return TramTime.ofHourMins(localTime);
    }

    @Override
    public TramTime getEndTime() {
        final LocalTime localTime = (LocalTime) relationship.getProperty(END_TIME.getText());
        return TramTime.ofHourMins(localTime);
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

    @Override
    public IdFor<Station> getEndStationId() {
        return getIdFor(Station.class, relationship.getEndNode());
    }

    @Override
    public IdFor<Station> getStartStationId() {
        return getIdFor(Station.class, relationship.getStartNode());
    }

    @Override
    public IdFor<StationLocalityGroup> getStationGroupId() {
        return getIdFor(StationLocalityGroup.class, relationship.getEndNode());
    }

    Relationship getRelationship() {
        return relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableGraphRelationship that = (MutableGraphRelationship) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final TransportRelationshipTypes relationshipType = getType();
        final String key = getExtraDiagnostics(relationshipType);
        return "MutableGraphRelationship{" +
                "type=" + relationshipType +
                key +
                " id=" + id +
                "} ";
    }

    @NotNull
    private String getExtraDiagnostics(TransportRelationshipTypes relationshipType) {
        final String extra;
        // TODO Include more types here, aids with debug etc
        if (relationshipType==TransportRelationshipTypes.TO_SERVICE) {
            extra = " serviceId=" + getServiceId().toString()
                    + " routeId=" + getRouteId();
        }
        else if (relationshipType==TransportRelationshipTypes.INTERCHANGE_DEPART || relationshipType==TransportRelationshipTypes.DEPART) {
            extra = " routeStationId=" + getRouteStationId();
        } else {
            extra = "";
        }
        return extra;
    }

}
