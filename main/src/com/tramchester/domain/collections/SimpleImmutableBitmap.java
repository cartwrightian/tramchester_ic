package com.tramchester.domain.collections;

import java.util.stream.Stream;

public interface SimpleImmutableBitmap {

    boolean get(int position);

    boolean isEmpty();

    int size();

    /***
     * @return number of bits set
     */
    long cardinality();

    SimpleImmutableBitmap getSubmap(int start, int end);

    Stream<Short> getBitIndexes();
}
