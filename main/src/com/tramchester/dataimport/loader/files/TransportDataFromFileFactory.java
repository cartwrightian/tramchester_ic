package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.dataimport.loader.TransportDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static java.lang.String.format;

public class TransportDataFromFileFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFileFactory.class);

    private final static String extension = ".txt";

    private final Path path;
    private final CsvMapper mapper;

    public TransportDataFromFileFactory(Path path, CsvMapper mapper) {
        this.path = path;
        this.mapper = mapper;
    }

    public <T> TransportDataFromCSVFile<T,T> getLoaderFor(final TransportDataReader.InputFiles inputFileType, final Class<T> targetType) {
        final Path filePath = formPath(inputFileType);

        logger.info(format("Create TransportDataFromCSVFile for %s from file %s", targetType.getSimpleName(), filePath));
        return new TransportDataFromCSVFile<>(filePath, targetType, mapper);
    }

    private Path formPath(final TransportDataReader.InputFiles theType) {
        final String filename = theType.name() + extension;
        return path.resolve(filename);
    }
}
