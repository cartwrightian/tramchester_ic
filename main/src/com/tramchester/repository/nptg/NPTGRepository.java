package com.tramchester.repository.nptg;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import com.tramchester.dataimport.nptg.NPTGXMLDataLoader;
import com.tramchester.dataimport.nptg.xml.NPTGLocalityXMLData;
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
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// National Public Transport Gazetteer
// https://data.gov.uk/dataset/3b1766bf-04a3-44f5-bea9-5c74cf002e1d/national-public-transport-gazetteer-nptg
// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf
// Cross-referenced by naptan data via the nptgLocalityCode

@LazySingleton
public class NPTGRepository {
    private static final Logger logger = LoggerFactory.getLogger(NPTGRepository.class);

    // some localities referenced from station/stops loaded within bounds might themselves lie outside
    private static final int MARGIN_IN_METERS = 3000;

    private final NPTGXMLDataLoader dataLoader;
    private final TramchesterConfig config;

    private final IdMap<NPTGLocality> nptgDataMap;

    @Inject
    public NPTGRepository(NPTGXMLDataLoader dataLoader, TramchesterConfig config) {
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
        final BoundingBox bounds = config.getBounds();


        logger.info("Starting for " + bounds);
        loadData(bounds);
        if (nptgDataMap.isEmpty()) {
            logger.error("Failed to load any data.");
        } else {
            logger.info("Loaded " + nptgDataMap.size() + " items ");
        }
        logger.info("started");
    }

    private void loadData(final BoundingBox bounds) {

        final MarginInMeters initialMargin = MarginInMeters.ofMeters(MARGIN_IN_METERS*2);

        final MarginInMeters loadMargin = MarginInMeters.ofMeters(MARGIN_IN_METERS);

        // todo use the callback mechanism instead
        List<NPTGLocalityXMLData> inBoundsRecords = new ArrayList<>();

        dataLoader.loadData(new ElementsFromXMLFile.XmlElementConsumer<>() {
            @Override
            public void process(NPTGLocalityXMLData element) {
                if (filterBy(bounds, initialMargin, element)) {
                    inBoundsRecords.add(element);
                }
            }

            @Override
            public Class<NPTGLocalityXMLData> getElementType() {
                return NPTGLocalityXMLData.class;
            }
        });

        logger.info("Initially loaded " + inBoundsRecords.size() + " in bounds records");

        // names has wider margin from initial load so we still (mostly) find parent localities which would otherwise
        // be out of bounds
        final Map<IdFor<NPTGLocality>, String> names = inBoundsRecords.stream().
                collect(Collectors.toMap(item -> NPTGLocality.createId(item.getNptgLocalityCode()), NPTGLocalityXMLData::getLocalityName));

        // apply narrower margins here
        inBoundsRecords.stream().
                filter(item -> filterBy(bounds, loadMargin, item)).
                map(item -> new NPTGLocality(item, getParentName(names, item))).
                forEach(nptgDataMap::add);

        names.clear();

    }

    private String getParentName(Map<IdFor<NPTGLocality>, String> names, NPTGLocalityXMLData item) {
        String ref = item.getParentLocalityRef();
        if (ref ==null) {
            return "";
        }
        if (ref.isEmpty()) {
            return "";
        }
        final IdFor<NPTGLocality> id = NPTGLocality.createId(ref);
        if (!id.isValid()) {
            logger.warn("Could not create valid id for " + ref + " from " + item);
            return "";
        }
        if (!names.containsKey(id)) {
            logger.warn("name is missing for id " + id);
            return "";
        }

        String result = names.get(id);
        if (result==null) {
            throw new RuntimeException("Problem with name for " + id + " from " + item);
        }
        return result;
    }


    private boolean filterBy(final BoundingBox bounds, final MarginInMeters margin, final NPTGLocalityXMLData item) {
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
