package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public class TransportDataSource {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataSource.class);

//    private final Stream<StopData> stops;
//    private final Stream<RouteData> routes;
//    private final Stream<TripData> trips;
//    private final Stream<StopTimeData> stopTimes;
//    private final Stream<CalendarData> calendars;
//    private final Stream<CalendarDateData> calendarsDates;
//    private final Stream<AgencyData> agencies;
//    private final Stream<FeedInfo> feedInfo;

    private final TransportDataReader transportDataReader;
    private final GTFSSourceConfig config;
    final private DataSourceInfo dataSourceInfo;
    final private TransportEntityFactory entityFactory;

//    public TransportDataSource(DataSourceInfo dataSourceInfo, Stream<AgencyData> agencies, Stream<StopData> stops,
//                               Stream<RouteData> routes, Stream<TripData> trips, Stream<StopTimeData> stopTimes,
//                               Stream<CalendarData> calendars,
//                               Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates,
//                               GTFSSourceConfig config, TransportEntityFactory entityFactory) {
//        this.dataSourceInfo = dataSourceInfo;
//        this.agencies = agencies;
//        this.stops = stops;
//        this.routes = routes;
//        this.trips = trips;
//        this.stopTimes = stopTimes;
//        this.calendars = calendars;
//        this.feedInfo = feedInfo;
//        this.calendarsDates = calendarsDates;
//        this.config = config;
//        this.entityFactory = entityFactory;
//    }

    public TransportDataSource(DataSourceInfo dataSourceInfo, TransportDataReader transportDataReader, GTFSSourceConfig config,
                               TransportEntityFactory entityFactory) {
        this.dataSourceInfo = dataSourceInfo;
        this.transportDataReader = transportDataReader;
        this.config = config;
        this.entityFactory = entityFactory;
    }

    @Deprecated
    public void closeAll() {
        logger.info("Close all");
//        stops.close();
//        routes.close();
//        trips.close();
//        stopTimes.close();
//        calendars.close();
//        feedInfo.close();
//        calendarsDates.close();
//        agencies.close();
        logger.info("Closed");
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
}
