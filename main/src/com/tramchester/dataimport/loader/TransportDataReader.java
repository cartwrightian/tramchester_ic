package com.tramchester.dataimport.loader;


import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.loader.files.TransportDataFromFileFactory;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.reference.GTFSTransportationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class TransportDataReader {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReader.class);

    private final GTFSSourceConfig config;
    private final TransportDataReaderFactory.GetModTimeFor getsFileModTime;
    private final TransportDataFromFileFactory factory;

    public enum InputFiles {
        trips, stops, routes, feed_info, calendar, stop_times, calendar_dates, agency
    }

    public TransportDataReader(TransportDataFromFileFactory factory, GTFSSourceConfig config, TransportDataReaderFactory.GetModTimeFor getsFileModTime) {
        this.factory = factory;
        this.config = config;
        this.getsFileModTime = getsFileModTime;
    }

    public GTFSSourceConfig getConfig() {
        return config;
    }

    public Stream<CalendarData> getCalendar() {
        return factory.getLoaderFor(InputFiles.calendar, CalendarData.class).load();
    }

    public Stream<CalendarDateData> getCalendarDates() {
        return factory.getLoaderFor(InputFiles.calendar_dates, CalendarDateData.class).load();
    }

    public Stream<StopTimeData> getStopTimes() {
        return factory.getLoaderFor(InputFiles.stop_times, StopTimeData.class).load();
    }

    public Stream<TripData> getTrips() {
        return factory.getLoaderFor(InputFiles.trips, TripData.class).load();
    }

    public Stream<StopData> getStops() {
        return factory.getLoaderFor(InputFiles.stops, StopData.class).load();
    }

    public Stream<RouteData> getRoutes() {
        return factory.getLoaderFor(InputFiles.routes, RouteData.class).load();
    }

    public Stream<FeedInfo> getFeedInfo() {
        return factory.getLoaderFor(InputFiles.feed_info, FeedInfo.class).load();
    }

    public Stream<AgencyData> getAgencies() {
        return factory.getLoaderFor(InputFiles.agency, AgencyData.class).load();
    }

    /// Need to make sure don't get mod time for the source files until the get is called, which ought to be after download,
    /// otherwise get invalid mod time
    public DataSourceInfo getDataSourceInfo() {
        DataSourceID dataSourceId = config.getDataSourceId();
        ZonedDateTime modTime = getsFileModTime.getModTime();
        String version = modTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        DataSourceInfo dataSourceInfo = new DataSourceInfo(dataSourceId, version, modTime,
                GTFSTransportationType.toTransportMode(config.getTransportGTFSModes()));
        logger.info("Create datasource info for " + config + " " + dataSourceInfo);
        return dataSourceInfo;
    }

    @Override
    public String toString() {
        return "TransportDataReader{" +
                ", source name=" + config.getName() +
                '}';
    }


}
