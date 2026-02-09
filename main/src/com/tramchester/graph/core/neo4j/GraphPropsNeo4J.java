package com.tramchester.graph.core.neo4j;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphEntityProperties;
import org.neo4j.graphdb.Entity;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphPropsNeo4J implements GraphEntityProperties.GraphProps<GraphPropsNeo4J> {
    private final Entity entity;

    private GraphPropsNeo4J(final Entity entity) {
        this.entity = entity;
    }

    public static GraphPropsNeo4J wrap(final Entity entity) {
        return new GraphPropsNeo4J(entity);
    }

    @Override
    public void setProperty(final GraphPropertyKey key, final Object value) {
        entity.setProperty(key.getText(), value);
    }

    @Override
    public Object getProperty(final GraphPropertyKey key) {
        return entity.getProperty(key.getText());
    }

    @Override
    public Map<GraphPropertyKey, Object> getAllProperties() {
        return entity.getAllProperties().entrySet().stream().
                collect(Collectors.toMap(entry ->  GraphPropertyKey.parse(entry.getKey()), Map.Entry::getValue));
    }

    @Override
    public boolean hasProperty(final GraphPropertyKey key) {
        return entity.hasProperty(key.getText());
    }

    @Override
    public void removeProperty(final GraphPropertyKey key) {
        entity.removeProperty(key.getText());
    }

    @Override
    public void setTime(TramTime tramTime) {
        setProperty(TIME, tramTime.asLocalTime());
        if (tramTime.isNextDay()) {
            setProperty(DAY_OFFSET, tramTime.isNextDay());
        }
    }

    @Override
    public TramTime getTime() {
        final LocalTime localTime = (LocalTime) getProperty(TIME);
        final boolean nextDay = hasProperty(DAY_OFFSET);
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }

    @Override
    public void addTripId(final IdFor<Trip> tripId) {
        final String text = tripId.getGraphId();
        if (!(hasProperty(TRIP_ID_LIST))) {
            setProperty(TRIP_ID_LIST, new String[]{text});
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
        setProperty(TRIP_ID_LIST, replacement);
    }

    @Override
    public boolean hasTripIdInList(final IdFor<Trip> tripId) {
        final String text = tripId.getGraphId();

        final String[] existing = getTripIdList();
        // NOTE: array MUST be sorted, see above addTripId
        // conversion to a list or similar much slower
        return Arrays.binarySearch(existing, text) >= 0;
    }

    private String[] getTripIdList() {
        return (String[]) getProperty(TRIP_ID_LIST);
    }

    @Override
    public ImmutableIdSet<Trip> getTripIds() {
        if (!hasProperty(TRIP_ID_LIST)) {
            return IdSet.emptySet();
        }
        final String[] existing = getTripIdList();

        return Arrays.stream(existing).map(Trip::createId).collect(IdSet.idCollector());
    }

    @Override
    public void setCost(final TramDuration cost) {
        final long seconds = cost.toSeconds();
        setProperty(COST, seconds);
    }

    @Override
    public TramDuration getCost() {
        if (hasProperty(COST)) {
            final long seconds = (long) getProperty(COST);
            return TramDuration.ofSeconds(seconds);
        }
        throw new RuntimeException("Cost is missing for " + this);
    }

    @Override
    public void setTransportMode(final TransportMode transportMode) {
        setProperty(TRANSPORT_MODE, transportMode.getNumber());
    }

    @Override
    public TransportMode getTransportMode() {
        short number = (short) getProperty(TRANSPORT_MODE);
        return TransportMode.fromNumber(number);
    }

    @Override
    public void addTransportMode(final TransportMode mode) {

        final short modeNumber = mode.getNumber();
        final GraphPropertyKey key = TRANSPORT_MODES;

        if (!hasProperty(key)) {
            // INIT
            setProperty(key, new short[]{modeNumber});
            return;
        } //else UPDATE

        final short[] existing = (short[]) getProperty(key);
        // note: not sorted, hence not binary search here
        for (short value : existing) {
            if (value == modeNumber) {
                return;
            }
        }

        final short[] replacement = Arrays.copyOf(existing, existing.length + 1);
        replacement[existing.length] = modeNumber;
        //Arrays.sort(replacement);
        setProperty(key, replacement);
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        if (!hasProperty(TRANSPORT_MODES)) {
            return EnumSet.noneOf(TransportMode.class);
        }

        final short[] existing = (short[]) getProperty(TRANSPORT_MODES);
        return TransportMode.fromNumbers(existing);
    }

    @Override
    public GraphPropsNeo4J copy() {
        throw new RuntimeException("Not implemented");
    }

}
