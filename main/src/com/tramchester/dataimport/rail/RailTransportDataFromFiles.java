package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.DirectDataSourceFactory;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.WriteableTransportData;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@LazySingleton
public class RailTransportDataFromFiles implements DirectDataSourceFactory.PopulatesContainer {
    private static final Logger logger = LoggerFactory.getLogger(RailTransportDataFromFiles.class);

    private final RailConfig railConfig;
    private final RemoteDataAvailable remoteDataRefreshed;

    private final boolean enabled;
    private final BoundingBox bounds;
    private final RailStationRecordsRepository stationRecordsRepository;
    private final RailDataFilenameRepository filenameRepository;
    private final Loader loader;

    @Inject
    public RailTransportDataFromFiles(ProvidesRailTimetableRecords loadRailTimetableRecords,
                                      TramchesterConfig config,
                                      GraphFilterActive graphFilterActive, RemoteDataAvailable remoteDataRefreshed,
                                      RailRouteIdRepository railRouteRepository,
                                      RailStationRecordsRepository stationRecordsRepository,
                                      RailDataFilenameRepository filenameRepository) {

        this.remoteDataRefreshed = remoteDataRefreshed;
        this.enabled = config.hasRailConfig();
        this.railConfig = config.getRailConfig();
        this.bounds = config.getBounds();
        this.stationRecordsRepository = stationRecordsRepository;
        this.filenameRepository = filenameRepository;

        loader = new Loader(loadRailTimetableRecords, railRouteRepository, railConfig, graphFilterActive);
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (enabled) {
            logger.info("Enabled");
        } else {
            logger.info("Disabled");
        }
        logger.info("started");
    }

    @Override
    public void loadInto(TransportDataContainer dataContainer) {
        if (!enabled) {
            logger.info("Disabled");
            return;
        }

        loader.loadInto(dataContainer, bounds, stationRecordsRepository);
    }

    @Override
    public DataSourceInfo getDataSourceInfo() {
        if (!enabled) {
            throw new RuntimeException("Not enabled");
        }

        if (!remoteDataRefreshed.hasFileFor(DataSourceID.openRailData)) {
            String message = "Missing data source file for " + DataSourceID.openRailData;
            logger.error(message);
            throw new RuntimeException(message);
        }
        final Path downloadedZip = remoteDataRefreshed.fileFor(DataSourceID.openRailData);

        final GetsFileModTime fileModTime = new GetsFileModTime();
        final ZonedDateTime modTime = fileModTime.getFor(downloadedZip);
        final DataSourceInfo dataSourceInfo = new DataSourceInfo(railConfig.getDataSourceId(),
                filenameRepository.getCurrentVersion(), modTime, railConfig.getModes());
        logger.info("Generated  " + dataSourceInfo);
        return dataSourceInfo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static class Loader {

        private final ProvidesRailTimetableRecords providesRailTimetableRecords;
        private final RailRouteIdRepository railRouteRepository;
        private final RailConfig railConfig;
        private final GraphFilterActive graphFilterActive;

        public Loader(ProvidesRailTimetableRecords providesRailTimetableRecords,
                      RailRouteIdRepository railRouteRepository,
                      RailConfig railConfig,
                      GraphFilterActive graphFilterActive) {
            this.providesRailTimetableRecords = providesRailTimetableRecords;
            this.railRouteRepository = railRouteRepository;
            this.railConfig = railConfig;
            this.graphFilterActive = graphFilterActive;
        }

        public void loadInto(final TransportDataContainer dataContainer, final BoundingBox bounds,
                             final RailStationRecordsRepository stationRecords) {

            logger.info("Load timetable");

            final Stream<RailTimetableRecord> timetableRecords = providesRailTimetableRecords.load();
            processTimetableRecords(stationRecords, dataContainer, timetableRecords, bounds, railRouteRepository);

            stationRecords.getInUse().forEach(dataContainer::addStation);

            logger.info("Retained " + stationRecords.countNeeded() + " stations of " + stationRecords.count());

        }

        private void processTimetableRecords(RailStationRecordsRepository stationRecords, WriteableTransportData dataContainer,
                                             Stream<RailTimetableRecord> recordStream, BoundingBox bounds, RailRouteIdRepository railRouteRepository) {
            logger.info("Process timetable stream");
            final RailTimetableMapper mapper = new RailTimetableMapper(stationRecords, dataContainer, railConfig,
                    graphFilterActive, bounds, railRouteRepository);
            recordStream.forEach(mapper::seen);
            mapper.reportDiagnostics();

        }

    }

}
