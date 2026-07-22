package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.rail.records.Line;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.SkippedRecord;
import com.tramchester.dataimport.rail.records.UnknownRecord;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;


@LazySingleton
public class LoadRailTimetableRecords implements ProvidesRailTimetableRecords {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailTimetableRecords.class);

    private final Path filePath;
    private final RailDataRecordFactory factory;
    private final boolean enabled;

    @Inject
    public LoadRailTimetableRecords(TramchesterConfig config, RailDataRecordFactory factory,
                                    UnzipFetchedData.Ready ready, RailDataFilenameRepository filenameRepository) {
        final RailConfig railConfig = config.getRail();
        enabled = (railConfig != null);
        this.factory = factory;

        if (enabled) {
            filePath = filenameRepository.getTimetable();
        } else {
            filePath = null;
        }
    }

    @Override
    public Stream<RailTimetableRecord> load() {
        if (!enabled) {
            throw new RuntimeException("Not enabled");
        }

        logger.info("Load from " + filePath.toAbsolutePath());
        try {
            final Reader reader = new FileReader(filePath.toString(), StandardCharsets.US_ASCII);
            return load(reader);
        } catch (IOException e) {
            String msg = "Unable to load from file " + filePath.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Stream<RailTimetableRecord> load(final Reader in) {
        logger.info("Loading lines");
        final BufferedReader bufferedReader = new BufferedReader(in);
        return bufferedReader.lines().map(text -> processLine(new Line(text)));
    }

    private RailTimetableRecord processLine(final Line line) {
        final RailRecordType recordType = getRecordTypeFor(line);
        try {
            return switch (recordType) {
                case TiplocInsert -> factory.createTIPLOC(line);
                case BasicSchedule -> factory.createBasicSchedule(line);
                case OriginLocation -> factory.createOrigin(line);
                case IntermediateLocation -> factory.createIntermediate(line);
                case TerminatingLocation -> factory.createTerminating(line);
                case BasicScheduleExtra -> factory.createBasicScheduleExtraDetails(line);
                case Header -> logHeader(line);
                case Association, ChangesEnRoute, Trailer -> skipRecord(recordType, line);
                default -> throw new RuntimeException("Missing record type for " + line);
            };
        } catch (IndexOutOfBoundsException outOfBoundsException) {
            String msg = "Got index out of bounds for recordType " + recordType + " line " + line;
            logger.error(msg, outOfBoundsException);
            throw new RuntimeException(msg, outOfBoundsException);
        }
    }

    private RailTimetableRecord skipRecord(final RailRecordType recordType, final Line line) {
        // Record that for now we choose to ignore
        return new SkippedRecord(recordType, line);
    }

    private RailTimetableRecord logHeader(final Line line) {
        logger.info("Header: '" + line + "'");
        return new UnknownRecord(line);
    }

    private RailRecordType getRecordTypeFor(final Line line) {
        //return RailRecordType.parse(line.extractToString(0,1));
        return RailRecordType.parse(line.subLine(0,2));
    }

    @Override
    public String toString() {
        return "LoadRailTimetableRecords{" +
                "filePath=" + filePath +
                ", enabled=" + enabled +
                '}';
    }
}
