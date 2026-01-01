package com.tramchester.domain.time;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.serialisation.TramDurationDeserializer;
import com.tramchester.mappers.serialisation.TramDurationSerializer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@JsonSerialize(using = TramDurationSerializer.class)
@JsonDeserialize(using = TramDurationDeserializer.class)
public class TramDuration implements Comparable<TramDuration> {

    private static final Factory factory = new Factory();
    private final Duration duration;

    public static final TramDuration ZERO = factory.fromSeconds(0);
    public static TramDuration Invalid = ofMinutes(-999);

    private TramDuration(Duration duration) {
        this.duration = duration;
    }

    private static TramDuration from(Duration contained) {
        return new TramDuration(contained);
    }

    public static TramDuration ofMinutes(long minutes) {
        return factory.fromSeconds(minutes*60L);
    }

    public static TramDuration ofSeconds(final long seconds) {
        return factory.fromSeconds(seconds);
    }

    public static TramDuration ofHours(long hours) {
        return factory.fromSeconds(hours*60*60);
    }

    public long toSeconds() {
        return duration.toSeconds();
    }

    @Deprecated
    public long getSeconds() {
        // TODO suspect call?
        return duration.getSeconds();
    }

    public int getMinutesSafe() {
        final long seconds = duration.getSeconds();
        final int mod = Math.floorMod(seconds, 60);
        if (mod!=0) {
            throw new RuntimeException("Accuracy lost attempting to convert " + duration + " to minutes");
        }
        return (int) Math.floorDiv(seconds, 60);
    }

    public boolean isZero() {
        return duration.isZero();
    }

    public boolean isValid() {
        return duration.isPositive() || duration.isZero();
    }

    public boolean invalid() {
        return !isValid();
    }

    @Override
    public int compareTo(@NotNull TramDuration other) {
        return duration.compareTo(other.duration);
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
        return "TramDuration{" +
                "duration=" + duration +
                '}';
    }

    public TramDuration truncateToMinutes() {
        return new TramDuration(duration.truncatedTo(ChronoUnit.MINUTES));
    }

    public long toMinutes() {
        return duration.toMinutes();
    }

    // MATH

    public TramDuration minus(final TramDuration other) {

        Duration result = duration.minus(other.duration);
        return factory.fromDuration(result);
    }

    public TramDuration plusMinutes(final int minutes) {
        return factory.fromDuration(duration.plusMinutes(minutes));
    }

    public TramDuration plus(TramDuration other) {
        return factory.fromDuration(duration.plus(other.duration));
    }

    public TramDuration plusSeconds(final int seconds) {
        return factory.fromDuration(duration.plusSeconds(seconds));
    }

    // Factory

    private static class Factory {
        private final ConcurrentMap<Long, Duration> secondsToDuration;

        private Factory() {
            secondsToDuration = new ConcurrentHashMap<>();
            secondsToDuration.put(0L, Duration.ZERO);
        }

        public TramDuration fromSeconds(final long seconds) {
            final Duration contained = secondsToDuration.computeIfAbsent(seconds, Duration::ofSeconds);
            return from(contained);
        }

        public TramDuration fromDuration(final Duration duration) {
            final Duration actual = duration.truncatedTo(ChronoUnit.SECONDS);
            final long seconds = actual.toSeconds();
            return from(secondsToDuration.computeIfAbsent(seconds, item -> actual));
        }
    }

}
