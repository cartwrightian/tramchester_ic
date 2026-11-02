package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.LocationType;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializerAsMillis;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializeAsMillis;

import java.time.LocalDateTime;
import java.util.Objects;

/***
 * Support for cookies on front-end
 */
@SuppressWarnings("unused")
public class Timestamped  {
    private LocalDateTime when; // Serialised as Millis since epoch
    private IdForDTO id;

    // support for previous format where location type not used
    private LocationType locationType = LocationType.Station;

    public Timestamped() {
        // deserialisation
    }

    public Timestamped(final LocationId<?> id, final LocalDateTime when, final LocationType locationType) {
        this(IdForDTO.createFor(id), when, locationType);
    }

    public Timestamped(final Location<?> location, final LocalDateTime when) {
        this(IdForDTO.createFor(location), when, location.getLocationType());
    }

    public Timestamped(final IdForDTO id, final LocalDateTime when, final LocationType locationType) {
        this.id = id;
        this.when = when;
        this.locationType = locationType;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializeAsMillis.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializerAsMillis.class)
    public LocalDateTime getWhen() {
        return when;
    }

    public IdForDTO getId() {
        return id;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setId(IdForDTO id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        /// NOTE : ignores the timestamp
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timestamped that = (Timestamped) o;
        return Objects.equals(id, that.id) && locationType == that.locationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, locationType);
    }

    // not using comparable as that causes equality issues, we just want ordering by time and NOT equality by time
    public static int compare(final Timestamped first, final Timestamped second) {
        return first.when.compareTo(second.when);
    }

    @Override
    public String toString() {
        return "Timestamped{" +
                "when=" + when +
                ", id=" + id +
                ", locationType=" + locationType +
                '}';
    }
}
