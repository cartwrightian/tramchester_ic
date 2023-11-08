package com.tramchester.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ComponentThatCaches<D extends CachableData, T extends FileDataCache.CachesData<D> > {
    private static final Logger logger = LoggerFactory.getLogger(ComponentThatCaches.class);

    private final DataCache fileDataCache;
    private final Class<D> itemType;

    public ComponentThatCaches(DataCache fileDataCache, Class<D> itemType) {
        this.fileDataCache = fileDataCache;
        this.itemType = itemType;
    }

    protected boolean cachePresent(T cachesDate) {
        return fileDataCache.has(cachesDate);
    }

    protected boolean loadFromCache(T cachesData) {
        String filename = cachesData.getFilename();
        if (cachePresent(cachesData)) {
            logger.info(format("Loading data from %s of type %s", filename, itemType.getSimpleName()));
            fileDataCache.loadInto(cachesData, itemType);
            return true;
        }

        logger.info(format("Cache %s not present, not loading", filename));
        return false;
    }

    protected void saveCacheIfNeeded(T cachesData) {
        if (!cachePresent(cachesData)) {
            logger.info(format("Saving data to %s of type %s", cachesData.getFilename(), itemType.getSimpleName()));
            fileDataCache.save(cachesData, itemType);
        } else {
            logger.info("Cache file was present, not saving");
        }
    }
}
