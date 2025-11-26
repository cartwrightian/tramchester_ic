package com.tramchester.graph.core.neo4j;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphEntityProperties;
import org.neo4j.graphdb.Entity;

import java.time.LocalTime;
import java.util.Map;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;
import static com.tramchester.graph.GraphPropertyKey.TIME;

public class GraphPropsNeo4J implements GraphEntityProperties.GraphProps {
    private final Entity entity;

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
}
