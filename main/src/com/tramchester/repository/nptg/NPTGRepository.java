package com.tramchester.repository.nptg;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.dataimport.nptg.NPTGDataLoader;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Set;

// National Public Transport Gazetteer
// https://data.gov.uk/dataset/3b1766bf-04a3-44f5-bea9-5c74cf002e1d/national-public-transport-gazetteer-nptg
// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf
// Cross-referenced by naptan data via the nptgLocalityCode

@LazySingleton
public class NPTGRepository {
    private static final Logger logger = LoggerFactory.getLogger(NPTGRepository.class);

    // some localities referenced from station/stops loaded within bounds might themselves lie outside
    private static final long MARGIN_IN_METERS = 3000;

    private final NPTGDataLoader dataLoader;
    private final TramchesterConfig config;

    private final IdMap<NPTGLocality> nptgDataMap;

    @Inject
    public NPTGRepository(NPTGDataLoader dataLoader, TramchesterConfig config) {
        this.dataLoader = dataLoader;
        this.config = config;
        nptgDataMap = new IdMap<>();
    }

    @PostConstruct
    private void start() {
        if (!dataLoader.isEnabled()) {
            logger.warn("Disabled");
            return;
        }
        BoundingBox bounds = config.getBounds();

        final MarginInMeters margin = MarginInMeters.of(MARGIN_IN_METERS);

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
                map(NPTGLocality::new).
                forEach(nptgDataMap::add);
    }

    private boolean filterBy(final BoundingBox bounds, final MarginInMeters margin, final NPTGData item) {
        final GridPosition gridPosition = item.getGridPosition();
        if (!gridPosition.isValid()) {
            return false;
        }
        return bounds.within(margin, gridPosition);
    }

    @PreDestroy
    private void stop() {
        nptgDataMap.clear();
    }

    public boolean hasLocaility(IdFor<NPTGLocality> localityCode) {
        return nptgDataMap.hasId(localityCode);
    }

    public NPTGLocality get(IdFor<NPTGLocality> localityCode) {
        return nptgDataMap.get(localityCode);
    }

    public Set<NPTGLocality> getAll() {
        return nptgDataMap.getValues();
    }
}
