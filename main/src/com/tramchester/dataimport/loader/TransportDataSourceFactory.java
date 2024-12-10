package com.tramchester.dataimport.loader;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.repository.naptan.NaptanRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@LazySingleton
public class TransportDataSourceFactory implements Iterable<TransportDataSource> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataSourceFactory.class);

    private final List<TransportDataSource> transportDataSources;
    private final TransportDataReaderFactory readerFactory;
    private final NaptanRepository naptanRepository;

    @Inject
    public TransportDataSourceFactory(TransportDataReaderFactory readerFactory, NaptanRepository naptanRepository,
                                      @SuppressWarnings("unused") UnzipFetchedData.Ready dataIsDownloadedAndUnzipped) {
        this.readerFactory = readerFactory;
        this.naptanRepository = naptanRepository;
        transportDataSources = new ArrayList<>();
    }


    public boolean hasDataSources() {
        return readerFactory.hasReaders();
    }

    // Note: feedinfo is not mandatory in the standard
    @PostConstruct
    public void start() {
        logger.info("start");

        final List<TransportDataReader> transportDataReaders = readerFactory.getReaders();

        logger.info("Loading for " + transportDataReaders.size() + " readers ");

        // streams, so no data read yet

        transportDataReaders.forEach(transportDataReader -> {
            GTFSSourceConfig sourceConfig = transportDataReader.getConfig();

            final TransportEntityFactory entityFactory = getEntityFactoryFor(sourceConfig);

            final DataSourceInfo dataSourceInfo = transportDataReader.getDataSourceInfo();

            final TransportDataSource transportDataSource = new TransportDataSource(dataSourceInfo, transportDataReader, sourceConfig, entityFactory);

            transportDataSources.add(transportDataSource);

        });

        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        transportDataSources.clear();
        logger.info("Stopped");
    }

    private TransportEntityFactory getEntityFactoryFor(final GTFSSourceConfig sourceConfig) {
        final DataSourceID sourceID = DataSourceID.valueOf(sourceConfig.getName());
        if (DataSourceID.tfgm == sourceID) {
            return new TransportEntityFactoryForTFGM(naptanRepository);
        } else {
            throw new RuntimeException("No entity factory is defined for " + sourceConfig.getName());
        }
    }

    // test support
    public TransportDataSource getFor(final DataSourceID dataSourceID) {
        Optional<TransportDataSource> maybe = transportDataSources.stream().
                filter(dataSource -> dataSource.getDataSourceInfo().getID().equals(dataSourceID)).findFirst();
        if (maybe.isEmpty()) {
            throw new RuntimeException("Could not find data source for " + dataSourceID);
        }
        return maybe.get();
    }

    @NotNull
    @Override
    public Iterator<TransportDataSource> iterator() {
        return transportDataSources.iterator();
    }

}
