package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class TransportDataFromCSVFile<T,R extends T> implements TransportDataFromFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromCSVFile.class);

    protected final Path filePath;
    private final ObjectReader objectReader;

    public TransportDataFromCSVFile(final Path filePath, final Class<R> readerType, final CsvMapper mapper) {
        this(filePath, readerType, Collections.emptyList(), mapper);
    }

    public TransportDataFromCSVFile(final Path filePath, final Class<R> readerType, final String cvsHeader, final CsvMapper mapper) {
        this(filePath, readerType, Arrays.asList(cvsHeader.split(",")), mapper);
    }

    private TransportDataFromCSVFile(final Path filePath, final Class<R> readerType, final List<String> columns,
                                     final CsvMapper mapper) {

        // TODO Set file encoding explicitly here?

        this.filePath = filePath.toAbsolutePath();

        final CsvSchema schema;
        if (columns.isEmpty()) {
            schema = CsvSchema.emptySchema().withHeader();
        } else {
            final CsvSchema.Builder builder = CsvSchema.builder();
            columns.forEach(builder::addColumn);
            schema = builder.build();
        }

        // create a reader ahead of time, helps with performance
        objectReader = mapper.readerFor(readerType).
                with(schema).
                //without(CsvParser.Feature.TRIM_SPACES). // default
                without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Stream<T> load() {
        try {
            final Reader reader = new FileReader(filePath.toString());
            return load(reader);
        } catch (IOException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    // public, test support inject of reader
    @Override
    public Stream<T> load(final Reader in) {

        try {
            // there is a text buffer inside CsvDecoder

            final MappingIterator<T> readerIter = objectReader.readValues(in);

            final Iterable<T> iterable = () -> readerIter;
            return StreamSupport.stream(iterable.spliterator(), false);

        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (IOException e) {
            logger.error("Unable to parse file " + filePath, e);
            return Stream.empty();
        }

    }

}
