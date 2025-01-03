package com.tramchester.testSupport;

import com.tramchester.caching.CachableData;
import com.tramchester.caching.DataCache;
import com.tramchester.caching.FileDataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataexport.HasDataSaver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class InMemoryDataCache implements DataCache {

    private final Map<Class<?>, InMemoryDataSaver<? extends CachableData>> savers;

    public InMemoryDataCache() {
        savers = new HashMap<>();
    }

    @Override
    public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> boolean has(T cachesData) {
        return savers.containsKey(cachesData.getClass());
    }

    @Override
    public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> void save(T cachesData, Class<CACHETYPE> dataClass) {
        InMemoryDataSaver<CACHETYPE> dataSaver = new InMemoryDataSaver<>();
        cachesData.cacheTo(new HasDataSaver<>(dataSaver));
        savers.put(cachesData.getClass(), dataSaver);
    }

    @Override
    public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> void loadInto(T cachesData, Class<CACHETYPE> dataClass) {

        if (!has(cachesData)) {
            throw new RuntimeException("No data for type " + dataClass);
        }

        final InMemoryDataSaver<? extends CachableData> inMemoryDataSaver = savers.get(cachesData.getClass());
        final InMemoryDataSaver<CACHETYPE> dataSaver = (InMemoryDataSaver<CACHETYPE>) inMemoryDataSaver;
        try {
            cachesData.loadFrom(dataSaver.getData());
        } catch (FileDataCache.CacheLoadException e) {
            throw new RuntimeException("Failed",e);
        }
    }

    @Override
    public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> Path getPathFor(T data) {
        throw new RuntimeException("not implemented");
    }

    public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> boolean hasData(Class<T> cachesDataType) {
        return savers.containsKey(cachesDataType);
    }

    public <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> Stream<CACHETYPE> getDataFor(Class<T> cachesDataType) {
        InMemoryDataSaver<? extends CachableData> saver = savers.get(cachesDataType);
        return saver.getData().map(item -> (CACHETYPE) item);
    }

    private static class InMemoryDataSaver<CACHETYPE> implements DataSaver<CACHETYPE> {

        private List<CACHETYPE> items;
        private boolean closed;

        public InMemoryDataSaver() {
            closed = false;
        }

        @Override
        public void write(CACHETYPE itemToSave) {
            guardNotClosed();
            items.add(itemToSave);
        }

        @Override
        public void open() {
            guardNotClosed();
            items = new ArrayList<>();
        }

        private void guardNotClosed() {
            if (closed) {
                throw new RuntimeException("closed");
            }
        }

        @Override
        public void close() {
            guardNotClosed();
            closed = true;
        }

        public Stream<CACHETYPE> getData() {
            return items.stream();
        }
    }
}
