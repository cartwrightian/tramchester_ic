package com.tramchester.dataexport;

import java.util.stream.Stream;

public interface DataSaver<T> {
    void write(T itemToSave);

    default void write(Stream<T> items) {
        items.forEach(this::write);
    }

    void open();

    void close();
}
