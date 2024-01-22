package com.tramchester.domain.time;

@FunctionalInterface
public interface ToHourFunction<T> {
    int applyAsHour(T item);
}
