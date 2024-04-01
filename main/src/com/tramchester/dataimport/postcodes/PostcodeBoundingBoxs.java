package com.tramchester.dataimport.postcodes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.ComponentThatCaches;
import com.tramchester.caching.FileDataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class PostcodeBoundingBoxs extends ComponentThatCaches<PostcodeHintData, PostcodeBoundingBoxs.PostcodeBounds> {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeBoundingBoxs.class);

    public final static String POSTCODE_HINTS_CSV = "postcode_hints.csv";

    private final PostcodeBounds postcodeBounds;
    private final boolean enabled;
    private boolean hadCacheAtStart;

    @Inject
    public PostcodeBoundingBoxs(TramchesterConfig config, FileDataCache dataCache) {
        super(dataCache, PostcodeHintData.class);
        postcodeBounds = new PostcodeBounds();
        enabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Postcode load disabled in config");
            return;
        }

        hadCacheAtStart = super.loadFromCache(postcodeBounds);
        if (hadCacheAtStart) {
            logger.info("loaded from cache");
        } else {
            logger.info("No cached data, in record mode");
        }
    }

    @PreDestroy
    public void stop() {
        if (!enabled) {
            logger.info("Postcode load disabled in config");
            return;
        }

        logger.info("stopping");
        if (!hadCacheAtStart) {
            super.saveCacheIfNeeded(postcodeBounds);
        }
        postcodeBounds.clear();
        logger.info("stopped");
    }

    public boolean checkOrRecord(Path sourceFilePath, PostcodeData postcode) {
        if (!postcode.getGridPosition().isValid()) {
            logger.warn("Bad position for " + postcode);
            return false;
        }

        String code = convertPathToCode(sourceFilePath);

        if (super.cachePresent(postcodeBounds)) {
            if (postcodeBounds.contains(code)) {
                return postcodeBounds.get(code).contained(postcode.getGridPosition());
            }
            logger.warn("Missing file when in playback mode: " + sourceFilePath);
        } else {
            if (postcodeBounds.contains(code)) {
                BoundingBox boundingBox = postcodeBounds.get(code);
                if (!boundingBox.contained(postcode.getGridPosition())) {
                    updateFor(code, postcode, boundingBox);
                }
            } else {
                // initially just the first one
                GridPosition gridPosition = postcode.getGridPosition();
                postcodeBounds.put(code, new BoundingBox(gridPosition.getEastings(), gridPosition.getNorthings(),
                        gridPosition.getEastings(), gridPosition.getNorthings()));
            }
        }
        return true;
    }

    public String convertPathToCode(Path sourceFilePath) {
        String name = sourceFilePath.getFileName().toString().toLowerCase();
        return name.replace(PostcodeDataImporter.CSV, "");
    }

    private void updateFor(String code, PostcodeData postcode, BoundingBox boundingBox) {
        logger.debug("Upadating bounds for " + code + " from " + postcode.getId());
        GridPosition gridPosition = postcode.getGridPosition();
        int postcodeEastings = gridPosition.getEastings();
        int postcodeNorthings = gridPosition.getNorthings();

        int newMinEasting = Math.min(postcodeEastings, boundingBox.getMinEastings());
        int newMinNorthing = Math.min(postcodeNorthings, boundingBox.getMinNorthings());
        int newMaxEasting = Math.max(postcodeEastings, boundingBox.getMaxEasting());
        int newMaxNorthing = Math.max(postcodeNorthings, boundingBox.getMaxNorthings());

        postcodeBounds.put(code, new BoundingBox(newMinEasting, newMinNorthing, newMaxEasting, newMaxNorthing));
    }

    public BoundingBox getBoundsFor(Path file) {
        return postcodeBounds.get(convertPathToCode(file));
    }

    public boolean isLoaded() {
        return super.cachePresent(postcodeBounds);
    }

    public boolean hasBoundsFor(Path file) {
        return postcodeBounds.contains(convertPathToCode(file));
    }

    /***
     * Uses bounded boxes and not the actual postcode area, so can produce some unexpected results as bounding boxes
     * cover significantly more area and overlap, which postcodes themselves don't
     */
    public Set<String> getCodesFor(GridPosition location, MarginInMeters margin) {
        return postcodeBounds.entrySet().stream().
                filter(entry -> entry.getValue().within(margin, location)).
                map(Map.Entry::getKey).
                collect(Collectors.toSet());
    }

    public static class PostcodeBounds implements FileDataCache.CachesData<PostcodeHintData> {
        private final Map<String, BoundingBox> theMap;

        public PostcodeBounds() {
            theMap = new HashMap<>();
        }

        @Override
        public String getFilename() {
            return POSTCODE_HINTS_CSV;
        }

        @Override
        public void cacheTo(HasDataSaver<PostcodeHintData> hasDataSaver) {
            Stream<PostcodeHintData> toCache = theMap.entrySet().stream().
                    map((entry) -> new PostcodeHintData(entry.getKey(), entry.getValue()));
            hasDataSaver.cacheStream(toCache);
        }

        @Override
        public void loadFrom(Stream<PostcodeHintData> data) {
            logger.info("Loading bounds from cache");
            data.forEach(item -> theMap.put(item.getCode(),
                    new BoundingBox(item.getMinEasting(), item.getMinNorthing(), item.getMaxEasting(), item.getMaxNorthing())));
        }

        public void clear() {
            theMap.clear();
        }

        public Set<Map.Entry<String, BoundingBox>> entrySet() {
            return theMap.entrySet();
        }

        public boolean contains(String code) {
            return theMap.containsKey(code);
        }

        public BoundingBox get(String code) {
            return theMap.get(code);
        }

        public void put(String code, BoundingBox box) {
            theMap.put(code, box);
        }
    }
}
