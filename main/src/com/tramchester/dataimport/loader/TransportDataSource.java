package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.loader.files.StringStreamReader;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TransportDataSource {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataSource.class);

    private final TransportDataReader transportDataReader;
    private final GTFSSourceConfig config;
    final private DataSourceInfo dataSourceInfo;
    final private TransportEntityFactory entityFactory;

    public TransportDataSource(DataSourceInfo dataSourceInfo, TransportDataReader transportDataReader, GTFSSourceConfig config,
                               TransportEntityFactory entityFactory) {
        this.dataSourceInfo = dataSourceInfo;
        this.transportDataReader = transportDataReader;
        this.config = config;
        this.entityFactory = entityFactory;
    }

    // TODO Move update/create of data source info
    public Stream<FeedInfo> getFeedInfoStream() {
        if (config.getHasFeedInfo()) {
            return transportDataReader.getFeedInfo();
        } else {
            return Stream.empty();
        }
    }

    public DataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    public GTFSSourceConfig getConfig() {
        return config;
    }

    public TransportEntityFactory getEntityFactory() {
        return entityFactory;
    }

    public Stream<StopData> getStops() {
        return transportDataReader.getStops();
    }

    public Stream<AgencyData> getAgencies() {
        return transportDataReader.getAgencies();
    }

    public Stream<RouteData> getRoutes() {
        return transportDataReader.getRoutes();
    }

    public Stream<TripData> getTrips() {
        return transportDataReader.getTrips();
    }

    public Stream<StopTimeData> getStopTimes() {
        return transportDataReader.getStopTimes();
    }

    public Stream<CalendarData> getCalendars() {
        return transportDataReader.getCalendar();
    }

    public Stream<CalendarDateData> getCalendarsDates() {
        return transportDataReader.getCalendarDates();
    }

    public Stream<StopTimeData> getStopTimesFiltered(final ChecksForTripId checksForTripId) {
        final TransportDataFromCSVFile.ReaderFactory readerFactory = new StopTimeFilteredReader(checksForTripId);

        return transportDataReader.getStopTimes(readerFactory);
    }

    public static class StopTimeFilteredReader implements TransportDataFromCSVFile.ReaderFactory {

        private final ChecksForTripId checksForTripId;

        public StopTimeFilteredReader(final ChecksForTripId checksForTripId) {
            this.checksForTripId = checksForTripId;
        }

        @Override
        public Reader getReaderFor(final Path filePath) throws IOException {
            final Reader reader = new FileReader(filePath.toString());
            StringStreamReader filteredReader = createReaderFor(reader);
            if (filteredReader==null) {
                logger.warn("Not using filtered reader, revert to file reader for " + filePath);
                return reader;
            } else {
                logger.info("Using filtered reader for " + filePath);
                return filteredReader;
            }
        }

        private StringStreamReader createReaderFor(final Reader reader) throws IOException {
            final BufferedReader bufferedReader = new BufferedReader(reader);

            final String header = bufferedReader.readLine();
            final int firstDelimit = header.indexOf(',');
            final String column = header.substring(0, firstDelimit);

            if (!"trip_id".equals(column)) {
                logger.error("Did not find trip_id column in expected place, got " + column);
                return null;
            }

            final Stream<String> headerStream = Stream.of(header);

            final Stream<String> filteredLines = bufferedReader.lines().filter(this::matches);

            return new StringStreamReader(Stream.concat(headerStream, filteredLines));
        }

        private boolean matches(final String line) {
            final int firstDelimit = line.indexOf(',');
            final String column = line.substring(0, firstDelimit);
            return checksForTripId.hasId(column);
        }
    }


}
