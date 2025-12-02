package com.tramchester.graph.core.neo4j;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphEntityProperties;
import org.neo4j.graphdb.Entity;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphPropsNeo4J implements GraphEntityProperties.GraphProps {
    private final Entity entity;

    private static final String tripIdListProperty = TRIP_ID_LIST.getText();

    private GraphPropsNeo4J(final Entity entity) {
        this.entity = entity;
    }

    public static GraphPropsNeo4J wrap(final Entity entity) {
        return new GraphPropsNeo4J(entity);
    }

    @Override
    public void setProperty(final String key, final Object value) {
        entity.setProperty(key, value);
    }

    @Override
    public Object getProperty(final String key) {
        return entity.getProperty(key);
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return entity.getAllProperties();
    }

    @Override
    public boolean hasProperty(final String key) {
        return entity.hasProperty(key);
    }

    @Override
    public void removeProperty(String key) {
        entity.removeProperty(key);
    }

    @Override
    public void setTime(TramTime tramTime) {
        setProperty(TIME.getText(), tramTime.asLocalTime());
        if (tramTime.isNextDay()) {
            setProperty(DAY_OFFSET.getText(), tramTime.isNextDay());
        }
    }

    @Override
    public TramTime getTime() {
        final LocalTime localTime = (LocalTime) getProperty(TIME.getText());
        final boolean nextDay = hasProperty(DAY_OFFSET.getText());
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }

    @Override
    public void addTripId(IdFor<Trip> tripId) {
        final String text = tripId.getGraphId();
        if (!(hasProperty(tripIdListProperty))) {
            setProperty(tripIdListProperty, new String[]{text});
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
        setProperty(tripIdListProperty, replacement);
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
        return (String[]) getProperty(tripIdListProperty);
    }

    @Override
    public IdSet<Trip> getTripIds() {
        if (!hasProperty(tripIdListProperty)) {
            return IdSet.emptySet();
        }
        final String[] existing = getTripIdList();

        return Arrays.stream(existing).map(Trip::createId).collect(IdSet.idCollector());
    }

    @Override
    public void setCost(final Duration cost) {
        final long seconds = cost.toSeconds();
        setProperty(COST.getText(), seconds);
    }

    @Override
    public Duration getCost() {
        if (hasProperty(COST)) {
            final long seconds = (long) getProperty(COST);
            return Duration.ofSeconds(seconds);
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
        final String key = TRANSPORT_MODES.getText();

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
        if (!hasProperty(TRANSPORT_MODES.getText())) {
            return EnumSet.noneOf(TransportMode.class);
        }

        final short[] existing = (short[]) getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }
}
