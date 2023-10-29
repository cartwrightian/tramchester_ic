package com.tramchester.caching;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.DataSourceID;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class FileDataCache implements DataCache {
    private static final Logger logger = LoggerFactory.getLogger(FileDataCache.class);

    private final Path cacheFolder;
    private final RemoteDataAvailable remoteDataRefreshed;
    private final TramchesterConfig config;
    private final LoaderSaverFactory loaderSaverFactory;
    private boolean ready;

    @Inject
    public FileDataCache(TramchesterConfig config, RemoteDataAvailable remoteDataRefreshed, LoaderSaverFactory loaderSaverFactory) {
        this.config = config;
        this.cacheFolder = config.getCacheFolder().toAbsolutePath();
        this.remoteDataRefreshed = remoteDataRefreshed;
        this.loaderSaverFactory = loaderSaverFactory;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        ready = false;

        File cacheDir = cacheFolder.toFile();
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            logger.info("Cached folder exists at " + cacheFolder);
            ready = true;
        } else {
            logger.info("Creating folder at " + cacheFolder);
            try {
                Files.createDirectories(cacheFolder);
                ready = true;
            } catch (IOException exception) {
                logger.warn("Could not create cache folder ", exception);
            }
        }

        clearCacheIfDataRefreshed();
    }

    private void clearCacheIfDataRefreshed() {

        // TODO Currently clear cache if any data source has refreshed, in future maybe link to Ready dependency chain??

        Set<DataSourceID> refreshedSources = config.getRemoteDataSourceConfig().stream().
                map(RemoteDataSourceConfig::getName).
                map(DataSourceID::findOrUnknown).
                filter(remoteDataRefreshed::refreshed).collect(Collectors.toSet());

        if (refreshedSources.isEmpty()) {
            logger.info("Found no updated data sources");
            return;
        }

        logger.warn("Some data sources (" + refreshedSources+ ") have refreshed, clearing cache " + cacheFolder);
        clearFiles();
    }

    @PreDestroy
    public void stop() {
        ready = false;
        logger.info("Stopping");
        try {
            List<Path> filesInCacheDir = filesInCache();
            filesInCacheDir.forEach(file -> logger.info("Cache file: " + file));
        } catch (IOException e) {
            logger.error("Could not list files in " + cacheFolder);
        }
        logger.info("Stopped");
    }

    @NotNull
    private List<Path> filesInCache() throws IOException {
        return Files.list(cacheFolder).filter(Files::isRegularFile).collect(Collectors.toList());
    }

    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> void save(T data, Class<CACHETYPE> theClass) {
        final Path path = getPathFor(data);

        if (ready) {
            logger.info("Saving " + theClass.getSimpleName() + " to " + path);
            DataSaver<CACHETYPE> saver = loaderSaverFactory.getDataSaverFor(theClass, path);
            data.cacheTo(saver);
        } else {
            logger.error("Not ready, no data saved to " + path);
        }
    }

    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> boolean has(T cachesData) {
        return Files.exists(getPathFor(cachesData));
    }

    @NotNull
    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> Path getPathFor(T data) {
        String filename = data.getFilename();
        return cacheFolder.resolve(filename).toAbsolutePath();
    }

    public void clearFiles() {
        if (!Files.exists(cacheFolder)) {
            logger.error("Not clearing cache, folder not present: " + cacheFolder);
            return;
        }

        logger.warn("Clearing cache " + cacheFolder.toAbsolutePath());
        try {
            List<Path> files = filesInCache();
            for (Path file : files) {
                if (Files.deleteIfExists(file)) {
                    logger.info("Removed " + file.toAbsolutePath());
                }
            }
        } catch (IOException exception) {
            final String msg = "Unable to clear cache";
            logger.error(msg, exception);
            throw new RuntimeException(msg, exception);
        }
    }

    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> void loadInto(T cachesData, Class<CACHETYPE> theClass)  {
        if (ready) {
            Path cacheFile = getPathFor(cachesData);
            logger.info("Loading " + cacheFile.toAbsolutePath()  + " to " + theClass.getSimpleName());

            TransportDataFromFile<CACHETYPE> loader = loaderSaverFactory.getDataLoaderFor(theClass, cacheFile);

            Stream<CACHETYPE> data = loader.load();
            try {
                cachesData.loadFrom(data);
            }
            catch (CacheLoadException exception) {
                final String message = format("Failed to load %s from cache file %s ", theClass.getSimpleName(), cacheFile);
                logger.error(message);
                throw new RuntimeException(message, exception);
            }
            data.close();
        } else {
            throw new RuntimeException("Attempt to load from " + cachesData.getFilename() + " for " + theClass.getSimpleName()
                    + " when not ready");
        }
    }



    public static class CacheLoadException extends Exception {

        public CacheLoadException(String msg) {
            super(msg);
        }
    }

    public interface CachesData<T extends CachableData> {
        void cacheTo(DataSaver<T> saver);
        String getFilename();
        void loadFrom(Stream<T> stream) throws CacheLoadException;
    }
}
