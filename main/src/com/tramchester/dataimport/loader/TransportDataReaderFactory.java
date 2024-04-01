package com.tramchester.dataimport.loader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DownloadedConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.loader.files.TransportDataFromFileFactory;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@LazySingleton
public class TransportDataReaderFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReaderFactory.class);

    private final TramchesterConfig tramchesterConfig;
    private final List<TransportDataReader> dataReaders;
    private final GetsFileModTime getsFileModTime;
    private final CsvMapper mapper;

    @Inject
    public TransportDataReaderFactory(TramchesterConfig tramchesterConfig, GetsFileModTime getsFileModTime) {
        this.getsFileModTime = getsFileModTime;
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

            // sanity check on remote data source being present, get data path from here
            final DataSourceID dataSourceId = sourceConfig.getDataSourceId();
            if (!tramchesterConfig.hasRemoteDataSourceConfig(dataSourceId)) {
                String msg = "No remote source config found for " + dataSourceId;
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            RemoteDataSourceConfig dataRemoteSourceConfig = tramchesterConfig.getDataRemoteSourceConfig(dataSourceId);
            Path dataLoadLocation = dataRemoteSourceConfig.getDataPath();
            logger.info("Got remote data source config for " + dataSourceId + " from config " + dataRemoteSourceConfig);

            TransportDataFromFileFactory factory = new TransportDataFromFileFactory(dataLoadLocation, mapper);
            GetModTimeFor getModTimeFor = new GetModTimeFor(dataRemoteSourceConfig, getsFileModTime);
            TransportDataReader transportLoader = new TransportDataReader(factory, sourceConfig, getModTimeFor);

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

    public boolean hasReaders() {
        return !dataReaders.isEmpty();
    }

    public static class GetModTimeFor {
        private final DownloadedConfig downloadConfig;
        private final GetsFileModTime getsFileModTime;

        public GetModTimeFor(DownloadedConfig downloadConfig, GetsFileModTime getsFileModTime) {
            this.downloadConfig = downloadConfig;
            this.getsFileModTime = getsFileModTime;
        }

        LocalDateTime getModTime() {
            return getsFileModTime.getFor(downloadConfig);
        }
    }
}
