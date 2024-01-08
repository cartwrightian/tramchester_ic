package com.tramchester.testSupport;

import com.tramchester.caching.CachableData;
import com.tramchester.caching.DataCache;
import com.tramchester.caching.FileDataCache;

import java.nio.file.Path;

public class FakeDataCache implements DataCache {

        @Override
        public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> boolean has(T cachesData) {
            return false;
        }

        @Override
        public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> void save(T data, Class<CACHETYPE> theClass) {
            // noop
        }

        @Override
        public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> void loadInto(T cachesData, Class<CACHETYPE> theClass) {
            // no op
        }

        @Override
        public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> Path getPathFor(T data) {
            throw new RuntimeException("not implemented");
        }

}
