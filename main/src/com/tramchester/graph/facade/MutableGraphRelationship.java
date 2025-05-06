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
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.HaveGraphProperties;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.SharedRelationshipCache;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class MutableGraphRelationship extends HaveGraphProperties implements GraphRelationship {

    private static final EnumSet<TransportRelationshipTypes> HAS_STATION_ID = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    private final Relationship relationship;
    private final GraphRelationshipId id;
    private final SharedRelationshipCache.InvalidatesCacheFor invalidatesCacheFor;

    private static final String tripIdListProperty = TRIP_ID_LIST.getText();

    private ImmutableGraphNode endNode;

    MutableGraphRelationship(final Relationship relationship, final GraphRelationshipId id,
                             SharedRelationshipCache.InvalidatesCacheFor invalidatesCacheFor) {
        this.relationship = relationship;
        this.id = id;
        this.invalidatesCacheFor = invalidatesCacheFor;
    }

    ///// MUTATE ////////////////////////////////////////////////////////////

    public void delete() {
        invalidateCache();
        relationship.delete();
    }

    private void invalidateCache() {
        invalidatesCacheFor.remove();
    }

    public void setTransportMode(final TransportMode transportMode) {
        relationship.setProperty(TRANSPORT_MODE.getText(), transportMode.getNumber());
        invalidateCache();
    }


    public void setTime(final TramTime tramTime) {
        setTime(tramTime, relationship);
        invalidateCache();
    }

    public void setHour(final int hour) {
        relationship.setProperty(HOUR.getText(), hour);
        invalidateCache();
    }


    public void setCost(final Duration cost) {
        final long seconds = cost.toSeconds();
        relationship.setProperty(COST.getText(), seconds);
        invalidateCache();
    }

    public <C extends GraphProperty & CoreDomain & HasId<C>> void set(final C domainItem) {
        super.set(domainItem, relationship);
        invalidateCache();
    }

    public void setRouteStationId(final IdFor<RouteStation> routeStationId) {
        relationship.setProperty(ROUTE_STATION_ID.getText(), routeStationId.getGraphId());
        invalidateCache();
    }

    public void setStopSeqNum(final int sequenceNumber) {
        relationship.setProperty(STOP_SEQ_NUM.getText(), sequenceNumber);
        invalidateCache();
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
        invalidateCache();
    }

    public void setStartTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for start time next");
        }
        relationship.setProperty(START_TIME.getText(), tramTime.asLocalTime());
        invalidateCache();
    }

    public void setEndTime(final TramTime tramTime) {
        if (tramTime.isNextDay()) {
            throw new RuntimeException("Not supported for end time next");
        }
        relationship.setProperty(END_TIME.getText(), tramTime.asLocalTime());
        invalidateCache();
    }

    public void setEndDate(final LocalDate localDate) {
        relationship.setProperty(END_DATE.getText(), localDate);
        invalidateCache();
    }

    public void setStartDate(final LocalDate localDate) {
        relationship.setProperty(START_DATE.getText(), localDate);
        invalidateCache();
    }

    public void addTransportMode(final TransportMode mode) {
        invalidateCache();

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
        invalidateCache();

        final String text = tripId.getGraphId();
        if (!(relationship.hasProperty(tripIdListProperty))) {
            relationship.setProperty(tripIdListProperty, new String[]{text});
            return;
        }
        // else
        final String[] existing = getTripIdList();

        // depends on the sort below
        // TODO use the output here to find place to split array, vs copy below?
        if (Arrays.binarySearch(existing, text)>=0) {
            return;
        }

        final int length = existing.length;
        final String[] replacement = Arrays.copyOf(existing, length + 1);
        replacement[length] = text;
        // keep array sorted, so can use BinarySearch
        Arrays.sort(replacement);
        relationship.setProperty(tripIdListProperty, replacement);

    }

    ////////////////////////////////////////////////////////////////////////

    @Override
    public Duration getCost() {
        final TransportRelationshipTypes relationshipType = getType();
        if (TransportRelationshipTypes.hasCost(relationshipType)) {
            final long seconds = (long) relationship.getProperty(COST.getText());
            return Duration.ofSeconds(seconds);
        } else {
            return Duration.ZERO;
        }
    }

    @Override
    public DateRange getDateRange() {
        final LocalDate start = getStartDate();
        final LocalDate end = getEndDate();
        return new DateRange(TramDate.of(start), TramDate.of(end));
    }

    @Override
    public GraphRelationshipId getId() {
        return id;
    }

    @Override
    public int getHour() {
        return (int) relationship.getProperty(HOUR.getText());
    }

    @Override
    public TramTime getTime() {
        return getTime(relationship);
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


    public IdSet<Trip> getTripIds() {
        if (!relationship.hasProperty(tripIdListProperty)) {
            return IdSet.emptySet();
        }
        final String[] existing = getTripIdList();

        return Arrays.stream(existing).map(Trip::createId).collect(IdSet.idCollector());
    }

    /***
     * Note: Assumes only called for relationships having TRIP_ID_LIST property, i.e. SERVICE_TO relationship type
     * @param tripId The id for a trip
     * @return true if trip id is contained in the list
     */
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        final String text = tripId.getGraphId();

        final String[] existing = getTripIdList();
        // NOTE: array MUST be sorted, see above addTripId
        // conversion to a list or similar much slower
        return Arrays.binarySearch(existing, text) >= 0;
    }

    private String[] getTripIdList() {
        return (String[]) relationship.getProperty(tripIdListProperty);
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
    public GraphNodeId getStartNodeId(final ImmutableGraphTransaction txn) {
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
        return getId(Route.class);
    }

    public IdFor<Service> getServiceId() {
        return getId(Service.class);
    }

    public IdFor<Trip> getTripId() {
        return getId(Trip.class);
    }

    public IdFor<Station> getStationId() {
        return getId(Station.class);
    }

    <T extends CoreDomain> IdFor<T> getId(Class<T> theClass) {
        return getIdFor(theClass, relationship);
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

    @Override
    public LocationId<?> getLocationId() {
        final TransportRelationshipTypes transportRelationshipTypes = getType();
        if (HAS_STATION_ID.contains(transportRelationshipTypes)) {
            return LocationId.wrap(getStationId());
        } else if (transportRelationshipTypes==GROUPED_TO_PARENT) {
            return LocationId.wrap(getStationGroupId());
        } else {
            throw new RuntimeException("Unsupported relationship type " + transportRelationshipTypes);
        }
    }

    public boolean hasProperty(final GraphPropertyKey propertyKey) {
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
            extra = " serviceId=" + getServiceId()
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
