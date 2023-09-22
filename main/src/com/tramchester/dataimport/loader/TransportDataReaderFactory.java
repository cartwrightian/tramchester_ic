package com.tramchester.dataimport.loader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.loader.files.TransportDataFromFileFactory;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.GTFSTransportationType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@LazySingleton
public class TransportDataReaderFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReaderFactory.class);

    private final TramchesterConfig tramchesterConfig;
    private final List<TransportDataReader> dataReaders;
    private final FetchFileModTime fetchFileModTime;
    private final CsvMapper mapper;

    @Inject
    public TransportDataReaderFactory(TramchesterConfig tramchesterConfig, FetchFileModTime fetchFileModTime) {
        this.fetchFileModTime = fetchFileModTime;
        this.mapper = CsvMapper.builder().
                addModule(new BlackbirdModule()).
                build();
        dataReaders = new ArrayList<>();
        this.tramchesterConfig = tramchesterConfig;
    }
    
    @PostConstruct
    public void start() {
        logger.info("start");
        tramchesterConfig.getGTFSDataSource().forEach(sourceConfig -> {
            logger.info("Creating reader for config " + sourceConfig.getName());
            Path path = sourceConfig.getDataPath();

            // sanity check on remote data source being path matching up
            final DataSourceID dataSourceId = sourceConfig.getDataSourceId();
            if (tramchesterConfig.hasRemoteDataSourceConfig(dataSourceId)) {
                RemoteDataSourceConfig dataRemoteSourceConfig = tramchesterConfig.getDataRemoteSourceConfig(dataSourceId);
                Path remoteLoadPath = dataRemoteSourceConfig.getDataPath();
                logger.info("Got remote data source config for " + dataSourceId + " from config " + dataRemoteSourceConfig);
                if (!remoteLoadPath.equals(path)) {
                    throw new RuntimeException("Pass mismatch for gtfs and remote source configs: " + dataSourceId);
                }
            } else {
                logger.warn("No remote source config found for " + dataSourceId);
            }

            TransportDataFromFileFactory factory = new TransportDataFromFileFactory(path, mapper);
            TransportDataReader transportLoader = new TransportDataReader(factory, sourceConfig, fetchFileModTime);

            dataReaders.add(transportLoader);
        });
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        dataReaders.clear();
        logger.info("Stopped");
    }

    public List<TransportDataReader> getReaders() {
        return dataReaders;
    }

    // moved into readers, cannot popupate this until after download etc of remote data sources
//    @NotNull
//    private DataSourceInfo createSourceInfoFrom(GTFSSourceConfig config) {
//        LocalDateTime modTime = fetchFileModTime.getFor(config);
//        DataSourceID dataSourceId = config.getDataSourceId();
//        String version = modTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//        DataSourceInfo dataSourceInfo = new DataSourceInfo(dataSourceId, version, modTime,
//                GTFSTransportationType.toTransportMode(config.getTransportGTFSModes()));
//        logger.info("Create datasource info for " + config + " " + dataSourceInfo);
//        return dataSourceInfo;
//    }

    public boolean hasReaders() {
        return !dataReaders.isEmpty();
    }
}
