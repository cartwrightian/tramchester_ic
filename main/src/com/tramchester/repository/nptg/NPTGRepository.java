package com.tramchester.repository.nptg;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.dataimport.nptg.NPTGDataLoader;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

// National Public Transport Gazetteer
// https://data.gov.uk/dataset/3b1766bf-04a3-44f5-bea9-5c74cf002e1d/national-public-transport-gazetteer-nptg
//Cross-referenced by naptan data via the nptgLocalityCode

@LazySingleton
public class NPTGRepository {
    private static final Logger logger = LoggerFactory.getLogger(NPTGRepository.class);

    private final NPTGDataLoader dataLoader;
    private final TramchesterConfig config;

    // acto code
    private final Map<IdFor<NaptanRecord>, NPTGData> nptgDataMap;

    @Inject
    public NPTGRepository(NPTGDataLoader dataLoader, TramchesterConfig config) {
        this.dataLoader = dataLoader;
        this.config = config;
        nptgDataMap = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        if (!dataLoader.isEnabled()) {
            logger.warn("Disabled");
            return;
        }
        BoundingBox bounds = config.getBounds();
        final Double range = config.getNearestStopForWalkingRangeKM();
        final MarginInMeters margin = MarginInMeters.of(range);
        logger.info("Starting for " + bounds + " and margin " + margin);
        loadData(bounds, margin);
        if (nptgDataMap.isEmpty()) {
            logger.error("Failed to load any data.");
        } else {
            logger.info("Loaded " + nptgDataMap.size() + " items ");
        }
        logger.info("started");
    }

    private void loadData(BoundingBox bounds, MarginInMeters margin) {
        dataLoader.getData().filter(nptgData -> filterBy(bounds, margin, nptgData)).
                forEach(item -> nptgDataMap.put(getActoCodeFor(item), item));
    }

    private boolean filterBy(final BoundingBox bounds, final MarginInMeters margin, final NPTGData item) {
        final GridPosition gridPosition = item.getGridPosition();
        if (!gridPosition.isValid()) {
            return false;
        }
        return bounds.within(margin, gridPosition);
    }

    private IdFor<NaptanRecord> getActoCodeFor(NPTGData nptgData) {
        return NaptanRecord.createId(nptgData.getActoCode());
    }

    @PreDestroy
    private void stop() {
        nptgDataMap.clear();
    }

    public NPTGData getByActoCode(IdFor<NaptanRecord> atcoCode) {
        return nptgDataMap.get(atcoCode);
    }

    public boolean hasActoCode(IdFor<NaptanRecord> actoCode) {
        return nptgDataMap.containsKey(actoCode);
    }
}
