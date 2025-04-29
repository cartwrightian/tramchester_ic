package com.tramchester.dataimport.loader.files;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.stream.Stream;

public class StringStreamReader extends Reader {
    private final Iterator<String> iterator;
    private final Stream<String> stream;
    private StringReader buffer;

    private final String lineSeparator = System.lineSeparator();

    public StringStreamReader(final Stream<String> stream) {
        this.stream = stream;
        this.iterator = stream.iterator();
        buffer = null;
    }

    @Override
    public int read(char @NotNull [] characterBuffer, int offset, int requestedLen) throws IOException {
        if (buffer==null) {
            buffer = getNext(requestedLen);
        }
        if (buffer==null) {
            return -1;
        }

        final int fromBuffer = buffer.read(characterBuffer, offset, requestedLen);
        if (fromBuffer<0) {
            buffer.close();
            buffer = null;
            return read(characterBuffer, offset, requestedLen);
        } else {
            return fromBuffer;
        }
    }

    private StringReader getNext(final int requestedLen) {
        if (!iterator.hasNext()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(requestedLen);
        while (iterator.hasNext() && builder.length()<=requestedLen) {
            builder.append(iterator.next()).append(lineSeparator);
        }
        return new StringReader(builder.toString());

    }

    @Override
    public void close() {
        stream.close();
    }
}
