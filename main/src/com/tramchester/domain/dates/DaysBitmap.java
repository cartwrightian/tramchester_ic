package com.tramchester.domain.dates;

import java.util.BitSet;
import java.util.stream.IntStream;

public interface DaysBitmap {
    boolean isSet(TramDate date);

    boolean contains(DaysBitmap other);

    BitSet createOverlapWith(DaysBitmap other);

    long getBeginningEpochDay();

    IntStream streamDays();

    int size();
}
