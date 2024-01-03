package com.tramchester.repository.nptg;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.dataimport.nptg.NPTGDataLoader;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanRecord;
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

    // acto code
    private final Map<IdFor<NaptanRecord>, NPTGData> nptgDataMap;

    @Inject
    public NPTGRepository(NPTGDataLoader dataLoader) {
        this.dataLoader = dataLoader;
        nptgDataMap = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        if (!dataLoader.isEnabled()) {
            logger.warn("Disabled");
            return;
        }
        logger.info("Starting");
        loadData();
        if (nptgDataMap.isEmpty()) {
            logger.error("Failed to load any data.");
        } else {
            logger.info("Loaded " + nptgDataMap.size() + " items ");
        }
        logger.info("started");
    }

    private void loadData() {
        dataLoader.getData().forEach(item -> nptgDataMap.put(getActoCodeFor(item), item));
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
