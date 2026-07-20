package com.tramchester.caching;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.filters.GraphFilterActive;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class FileDataCache implements DataCache {
    private static final Logger logger = LoggerFactory.getLogger(FileDataCache.class);

    private final Path cacheFolder;
    private final RemoteDataAvailable remoteDataRefreshed;
    private final LoaderSaverFactory loaderSaverFactory;
    private final boolean cachingDisabled;
    private final boolean graphFilterActive;
    private final Map<Class<?>, ImmutableEnumSet<DataSourceID>> depsForType;
    private boolean ready;

    @Inject
    public FileDataCache(TramchesterConfig config, RemoteDataAvailable remoteDataRefreshed, LoaderSaverFactory loaderSaverFactory,
                         GraphFilterActive graphFilterActive) {
        this.cacheFolder = config.getCacheFolder().toAbsolutePath();
        this.remoteDataRefreshed = remoteDataRefreshed;
        this.loaderSaverFactory = loaderSaverFactory;
        this.cachingDisabled = config.getCachingDisabled();
        this.graphFilterActive = graphFilterActive.isActive();
        depsForType = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        if (cachingDisabled) {
            logger.warn("Disabled in config");
            return;
        }
        if (graphFilterActive) {
            logger.warn("Graph filter is active, disabling");
            return;
        }

        logger.info("Starting");
        ready = false;

        final File cacheDir = cacheFolder.toFile();
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

        //clearCacheIfDataRefreshed();
    }

    @PreDestroy
    public void stop() {
        if (cachingDisabled || graphFilterActive) {
            logger.warn("Disabled");
            return;
        }

        ready = false;
        logger.info("Stopping");
        try {
            final List<Path> filesInCacheDir = filesInCache();
            filesInCacheDir.forEach(file -> logger.info("Cache file: " + file));
        } catch (IOException e) {
            logger.error("Could not list files in " + cacheFolder);
        }
        logger.info("Stopped");
    }

    @NotNull
    private List<Path> filesInCache() throws IOException {
        return Files.list(cacheFolder).
                filter(Files::isRegularFile).
                toList();
    }

    @Override
    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> void save(T data, Class<CACHETYPE> theClass) {
        if (cachingDisabled || graphFilterActive) {
            logger.error("NOT saving cache data for " + theClass.getSimpleName() + " as caching is disabled");
            return;
        }

        final Path path = getPathFor(data);

        if (ready) {
            logger.info("Saving " + theClass.getSimpleName() + " to " + path);
            HasDataSaver<CACHETYPE> hasDataSaver = loaderSaverFactory.getSaverFor(theClass, path);
            data.cacheTo(hasDataSaver);
        } else {
            logger.error("Not ready, no data saved to " + path);
        }
    }

    @Override
    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> boolean has(final T cachesData) {
        if (cachingDisabled || graphFilterActive) {
            return false;
        }

        if (Files.exists(getPathFor(cachesData))) {

            final Class<CACHETYPE> typeOfData = cachesData.getDataType();
            if (!depsForType.containsKey(typeOfData)) {
                throw new RuntimeException("Not registered " + typeOfData + " missing from " + depsForType.keySet());
            }

            final ImmutableEnumSet<DataSourceID> deps = depsForType.get(typeOfData);

            final Set<DataSourceID> outdated = deps.stream().filter(remoteDataRefreshed::refreshed).collect(Collectors.toSet());
            if (outdated.isEmpty()) {
                logger.info("Dependencies " + deps + " up to date for " + typeOfData.getSimpleName());
                return true;
            } else {
                logger.warn("Outdated dependencies " + outdated + " for " + typeOfData.getSimpleName());
                deleteFor(cachesData);
                return false;
            }
        }

        return false;
    }

    private <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> void deleteFor(final T cachesData) {
        final Path path = getPathFor(cachesData);
        try {
            Files.delete(path);
        } catch (IOException e) {
            String msg = format("Error while attempting to delete %s for %s", path.toAbsolutePath(), cachesData.getClass().getSimpleName());
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @NotNull
    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> Path getPathFor(final T cachesData) {
        guardCorrectState();

        final String filename = cachesData.getFilename();
        return cacheFolder.resolve(filename).toAbsolutePath();
    }

    @Override
    public <CACHETYPE extends CachableData> void register(final Class<CACHETYPE> itemType, final ImmutableEnumSet<DataSourceID> dependsOn) {
        logger.info("Registering " + itemType.getSimpleName() + " with dependencies " + dependsOn);
        depsForType.put(itemType, dependsOn);
    }

    public void clearFiles() {
        if (cachingDisabled || graphFilterActive) {
            logger.error("Will not clear files when disabled");
            return;
        }

        if (!Files.exists(cacheFolder)) {
            logger.error("Not clearing cache, folder not present: " + cacheFolder);
            return;
        }

        logger.warn("Clearing cache " + cacheFolder.toAbsolutePath());
        try {
            final List<Path> files = filesInCache();
            for (final Path file : files) {
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

    @Override
    public <CACHETYPE extends CachableData, T extends CachesData<CACHETYPE>> void loadInto(T cachesData, Class<CACHETYPE> theClass)  {
        guardCorrectState();

        if (ready) {
            final Path cacheFile = getPathFor(cachesData);
            logger.info("Loading " + cacheFile.toAbsolutePath()  + " to " + theClass.getSimpleName());

            final TransportDataFromFile<CACHETYPE> loader = loaderSaverFactory.getDataLoaderFor(theClass, cacheFile);

            final Stream<CACHETYPE> data = loader.load();
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

    private void guardCorrectState() {
        if (cachingDisabled) {
            String msg = "Caching is disabled";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public static class CacheLoadException extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;

        public CacheLoadException(String msg) {
            super(msg);
        }
    }

    public interface CachesData<T extends CachableData> {
        void cacheTo(HasDataSaver<T> saver);
        String getFilename();
        void loadFrom(Stream<T> stream) throws CacheLoadException;
        Class<T> getDataType();
    }
}
