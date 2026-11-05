package com.tramchester.domain.time;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.serialisation.TramDurationDeserializer;
import com.tramchester.mappers.serialisation.TramDurationSerializer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@JsonSerialize(using = TramDurationSerializer.class)
@JsonDeserialize(using = TramDurationDeserializer.class)
public class TramDuration implements Comparable<TramDuration> {

    private static final Factory factory;

    public static final TramDuration ZERO;

    static {
        factory = new Factory();
        ZERO = factory.fromSeconds(0L);
    }

    private final Long duration;

    private TramDuration(final Long duration) {
        this.duration = duration;
    }

    public static TramDuration ofMinutes(final long minutes) {
        return factory.fromSeconds(minutes*60L);
    }

    public static TramDuration ofSeconds(final Long seconds) {
        return factory.fromSeconds(seconds);
    }

    public static TramDuration ofHours(final long hours) {
        return factory.fromSeconds(hours*60L*60L);
    }

    public static TramDuration getMax() {
        return factory.getMax();
    }

    public static TramDuration getInvalid() {
        return factory.getInvalid();
    }

    public Long toSeconds() {
        return duration;
    }

    public Duration toDuration() {
        return Duration.ofSeconds(duration);
    }

    public int getMinutesSafe() {
        final int mod = Math.floorMod(duration, 60);
        if (mod!=0) {
            throw new RuntimeException("Accuracy lost attempting to convert " + duration + " to minutes");
        }
        return (int) Math.floorDiv(duration, 60);
    }

    public boolean isZero() {
        return this==ZERO;
    }

    public boolean isValid() {
        return duration>=0L;
    }

    @Override
    public int compareTo(@NotNull TramDuration other) {
        return Long.compare(duration, other.duration);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TramDuration that = (TramDuration) o;
        return Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(duration);
    }

    @Override
    public String toString() {
        if (!isValid()) {
            return "TramDuration{INVALID}";
        }
        if (this.duration==Long.MAX_VALUE) {
            return "TramDuration{MAX}";
        }
        return "TramDuration{" + duration + '}';
    }

    public TramDuration truncateToMinutes() {
        long minutes = Math.floorDivExact(duration, 60L);
        return ofMinutes(minutes);
        //return new TramDuration(duration.truncatedTo(ChronoUnit.MINUTES));
    }

    public long toMinutes() {
        return Math.floorDiv(duration, 60L);
    }

    // MATH

    public TramDuration minus(final TramDuration other) {

        //final Duration result = duration.minus(other.duration);
        long result = duration-other.duration;
        return factory.fromSeconds(result);
    }

    public TramDuration plusMinutes(final int minutes) {
        return factory.fromSeconds(duration+(60L*minutes));
    }

    public TramDuration plus(final TramDuration other) {
        if (other.isMax()) {
            throw new RuntimeException("other is Max");
        }
        if (this.isMax()) {
            throw new RuntimeException("This is Max");
        }
        return factory.fromSeconds(duration+other.duration);
    }

    public boolean isMax() {
        return this==factory.getMax();
    }

    public TramDuration plusSeconds(final int seconds) {
        return factory.fromSeconds(duration+seconds);
    }

    public boolean lessThan(final TramDuration other) {
//        return duration < other.duration;
        return duration.compareTo(other.duration) < 0;
    }

    public boolean moreThan(final TramDuration other) {
//        return duration > other.duration;
        return duration.compareTo(other.duration) > 0;
    }

    // Factory

    private static class Factory {

        private final ConcurrentMap<Long, TramDuration> secondsToDuration;

        private Factory() {
            secondsToDuration = new ConcurrentHashMap<>();
            secondsToDuration.put(0L, new TramDuration(0L));
        }

        public TramDuration fromSeconds(final Long seconds) {
           return secondsToDuration.computeIfAbsent(seconds, key -> new TramDuration(seconds));
        }

        public TramDuration getMax() {
            return fromSeconds(Long.MAX_VALUE);
        }

        public TramDuration getInvalid() {
            return fromSeconds(-999L);
        }
    }

}
