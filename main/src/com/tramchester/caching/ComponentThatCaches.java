package com.tramchester.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ComponentThatCaches<DATA extends CachableData, CACHES extends FileDataCache.CachesData<DATA> > {
    private static final Logger logger = LoggerFactory.getLogger(ComponentThatCaches.class);

    private final DataCache dataCache;
    private final Class<DATA> itemType;

    public ComponentThatCaches(DataCache dataCache, Class<DATA> itemType) {
        this.dataCache = dataCache;
        this.itemType = itemType;
    }

    protected boolean cachePresent(CACHES cachesDate) {
        return dataCache.has(cachesDate);
    }

    protected boolean loadFromCache(CACHES cachesData) {
        String filename = cachesData.getFilename();
        if (cachePresent(cachesData)) {
            logger.info(format("Loading data from %s of type %s", filename, itemType.getSimpleName()));
            dataCache.loadInto(cachesData, itemType);
            return true;
        }

        logger.info(format("Cache %s not present, not loading", filename));
        return false;
    }

    protected void saveCacheIfNeeded(CACHES cachesData) {
        if (!cachePresent(cachesData)) {
            logger.info(format("Saving data to %s of type %s", cachesData.getFilename(), itemType.getSimpleName()));
            dataCache.save(cachesData, itemType);
        } else {
            logger.info("Cache file was present, not saving");
        }
    }
}
