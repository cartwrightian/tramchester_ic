package com.tramchester.dataexport;

import java.util.stream.Stream;

public class HasDataSaver<T>  {

    private final DataSaver<T> saver;

    public HasDataSaver(DataSaver<T> saver) {
        this.saver = saver;
    }

    public ClosableDataSaver<T> get() {
        saver.open();
        return new ClosableDataSaver<>(saver);
    }

    public static class ClosableDataSaver<T> implements AutoCloseable {

        private final DataSaver<T> contained;

        public ClosableDataSaver(DataSaver<T> saver) {
            contained = saver;
        }

        @Override
        public void close() throws Exception {
            contained.close();
        }

        public void write(T item) {
            contained.write(item);
        }

        public void write(Stream<T> item) {
            contained.write(item);
        }
    }
}
