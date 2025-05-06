package com.tramchester.dataimport.loader;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.WriteableTransportData;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class PopulateTransportDataFromSources implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(PopulateTransportDataFromSources.class);

    private final TransportDataSourceFactory transportDataSourceFactory;
    private final DirectDataSourceFactory directDataSourceFactory;
    private final TramchesterConfig tramchesterConfig;
    private final ProvidesNow providesNow;

    private final TransportDataContainer dataContainer;

    // NOTE: cannot inject GraphFilter here as circular dependency on being able to find routes which
    // needs transport data to be loaded....
    @Inject
    public PopulateTransportDataFromSources(TransportDataSourceFactory transportDataSourceFactory,
                                            DirectDataSourceFactory directDataSourceFactory,
                                            TramchesterConfig tramchesterConfig, ProvidesNow providesNow) {
        this.transportDataSourceFactory = transportDataSourceFactory;
        this.directDataSourceFactory = directDataSourceFactory;
        this.tramchesterConfig = tramchesterConfig;
        this.providesNow = providesNow;
        dataContainer = new TransportDataContainer(providesNow, "TransportDataFromFiles");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        dataContainer.dispose();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (transportDataSourceFactory.hasDataSources()) {
            logger.info("Load for gtfs sources");
            transportDataSourceFactory.forEach(transportDataSource -> load(transportDataSource, dataContainer));
        }
        logger.info("Load for direct sources"); // for now this is just for rail data loading
        directDataSourceFactory.forEach(directDataSource -> {
            directDataSource.loadInto(dataContainer);
            dataContainer.addDataSourceInfo(directDataSource.getDataSourceInfo());
            dataContainer.reportNumbers();
        } );
        logger.info("started");
    }

    public TransportData getData() {
        return dataContainer;
    }

    private void load(final TransportDataSource dataSource, final WriteableTransportData writeableTransportData) {
        final DataSourceInfo dataSourceInfo = dataSource.getDataSourceInfo();

        final GTFSSourceConfig sourceConfig = dataSource.getConfig();

        logger.info("Loading data for " + dataSourceInfo);

        updateDataSourceInfo(dataSource, writeableTransportData, dataSourceInfo, sourceConfig);

        final TransportEntityFactory entityFactory = dataSource.getEntityFactory();

        // create loaders
        final StopDataLoader stopDataLoader = new StopDataLoader(entityFactory, tramchesterConfig);
        final AgencyDataLoader agencyDataLoader = new AgencyDataLoader(dataSourceInfo, entityFactory);
        final RouteDataLoader routeDataLoader = new RouteDataLoader(writeableTransportData, sourceConfig, entityFactory);
        final TripLoader tripLoader = new TripLoader(writeableTransportData, entityFactory);
        final GTFSStopTimeLoader stopTimeLoader = new GTFSStopTimeLoader(writeableTransportData, entityFactory, sourceConfig);
        final CalendarLoader calendarLoader = new CalendarLoader(writeableTransportData, entityFactory);
        final CalendarDateLoader calendarDateLoader = new CalendarDateLoader(writeableTransportData, providesNow, sourceConfig);

        final PreloadedStationsAndPlatforms interimStations = stopDataLoader.load(dataSource.getStops());
        final CompositeIdMap<Agency, MutableAgency> interimAgencies = agencyDataLoader.load(dataSource.getAgencies());
        final RouteDataLoader.LoadedRoutesCache loadedRoutesCache = routeDataLoader.load(dataSource.getRoutes(), interimAgencies);

        interimAgencies.clear();

        final PreloadTripAndServices interimTripsAndServices = tripLoader.load(dataSource.getTrips(), loadedRoutesCache);
        final Stream<StopTimeData> stopTimes = dataSource.getStopTimesFiltered(interimTripsAndServices);
        final IdMap<Service> interimServices = stopTimeLoader.load(stopTimes, interimStations, interimTripsAndServices);

        loadedRoutesCache.clear();
        interimStations.clear();

        calendarLoader.load(dataSource.getCalendars(), interimServices);
        calendarDateLoader.load(dataSource.getCalendarsDates(), interimServices);

        interimTripsAndServices.clear();
        interimServices.clear();

        writeableTransportData.reportNumbers();

        writeableTransportData.getServicesWithoutCalendar().
                forEach(svc -> logger.warn(format("source %s Service %s has missing calendar", dataSourceInfo, svc.getId()))
        );
        reportZeroDaysServices(writeableTransportData);

        entityFactory.logDiagnostics(writeableTransportData);

        logger.info("Finishing Loading data for " + dataSourceInfo);
    }

    private void updateDataSourceInfo(TransportDataSource dataSource, WriteableTransportData writeableTransportData,
                                      DataSourceInfo dataSourceInfo, GTFSSourceConfig sourceConfig) {
        if(sourceConfig.getHasFeedInfo()) {
            DataSourceID dataSourceInfoID = dataSourceInfo.getID();

            Optional<FeedInfo> maybeFeedinfo = dataSource.getFeedInfoStream().findFirst();
            if (maybeFeedinfo.isEmpty()) {
                throw new RuntimeException("config returned feedinfo expected but not actually present");
            }
            maybeFeedinfo.ifPresent(feedInfo -> {
                DataSourceInfo replacementDataSourceInfo = DataSourceInfo.updatedVersion(dataSourceInfo, feedInfo);
                writeableTransportData.addDataSourceInfo(replacementDataSourceInfo);
                writeableTransportData.addDateRangeAndVersionFor(dataSourceInfoID, feedInfo);
            });

        } else {
            logger.warn("No feedinfo for " + dataSourceInfo);
            writeableTransportData.addDataSourceInfo(dataSourceInfo);
        }
    }

    private void reportZeroDaysServices(WriteableTransportData buildable) {
        IdSet<Service> noDayServices = buildable.getServicesWithZeroDays();
        if (!noDayServices.isEmpty()) {
            logger.warn("The following services do no operate on any days per calendar.txt file " + noDayServices);
        }
    }


















}
