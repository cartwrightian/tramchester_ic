package com.tramchester.dataimport.loader.files;

import java.io.Reader;
import java.util.stream.Stream;

public interface TransportDataFromFile<T> {
    Stream<T> load();

    // public, test support inject of reader
    Stream<T> load(Reader in);
}
