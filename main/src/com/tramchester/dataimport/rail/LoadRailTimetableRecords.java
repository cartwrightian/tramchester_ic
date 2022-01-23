package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.SkippedRecord;
import com.tramchester.dataimport.rail.records.UnknownRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.stream.Stream;


public class LoadRailTimetableRecords {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailTimetableRecords.class);

    private final Path filePath;
    private final RailDataRecordFactory factory;

    public LoadRailTimetableRecords(Path filePath, RailDataRecordFactory factory) {
        this.filePath = filePath.toAbsolutePath();
        this.factory = factory;
    }

    public Stream<RailTimetableRecord> load() {
        logger.info("Load from " + filePath.toAbsolutePath());
        try {
            Reader reader = new FileReader(filePath.toString());
            return load(reader);
        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + filePath.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Stream<RailTimetableRecord> load(Reader in) {
        logger.info("Loading lines");
        BufferedReader bufferedReader = new BufferedReader(in);
        return bufferedReader.lines().map(this::processLine);
    }

    private RailTimetableRecord processLine(String line) {
        RailRecordType recordType = getRecordTypeFor(line);
        return switch (recordType) {
            case TiplocInsert -> factory.createTIPLOC(line);
            case BasicSchedule -> factory.createBasicSchedule(line);
            case OriginLocation -> factory.createOrigin(line);
            case IntermediateLocation -> factory.createIntermediate(line);
            case TerminatingLocation -> factory.createTerminating(line);
            case BasicScheduleExtra -> factory.createBasicScheduleExtraDetails(line);
            case Header -> logHeader(line);
            case Association, ChangesEnRoute, Trailer
                    -> skipRecord(recordType, line);
            default -> throw new RuntimeException("Missing record type for " + line);
        };
    }

    private RailTimetableRecord skipRecord(RailRecordType recordType, String line) {
        // Record that for now we choose to ignore
        return new SkippedRecord(recordType, line);
    }

    private RailTimetableRecord logHeader(String line) {
        logger.info("Header: '" + line + "'");
        return new UnknownRecord(line);
    }

    private RailRecordType getRecordTypeFor(String line) {
        return RailRecordType.parse(line.substring(0, 2));
    }

}