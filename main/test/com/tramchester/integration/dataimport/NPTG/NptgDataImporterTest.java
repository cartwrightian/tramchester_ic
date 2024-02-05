package com.tramchester.integration.dataimport.NPTG;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import com.tramchester.dataimport.nptg.NPTGXMLDataLoader;
import com.tramchester.dataimport.nptg.xml.NPTGLocalityXMLData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NptgDataImporterTest {

    private static GuiceContainerDependencies componentContainer;
    private static List<NPTGLocalityXMLData> loadedLocalities;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan(
                EnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        NPTGXMLDataLoader dataImporter = componentContainer.get(NPTGXMLDataLoader.class);

        loadedLocalities = new ArrayList<>();
        dataImporter.loadData(new ElementsFromXMLFile.XmlElementConsumer<>() {
            @Override
            public void process(NPTGLocalityXMLData element) {
                loadedLocalities.add(element);
            }

            @Override
            public Class<NPTGLocalityXMLData> getElementType() {
                return NPTGLocalityXMLData.class;
            }
        });
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        loadedLocalities.clear();
        componentContainer.close();
    }

    @Test
    void shouldLoadKnownLocality() {

        IdFor<NPTGLocality> shudehillId = KnownLocality.Shudehill.getLocalityId();

        Optional<NPTGLocalityXMLData> foundKnown = loadedLocalities.stream().
                filter(localityData -> NPTGLocality.createId(localityData.getNptgLocalityCode()).equals(shudehillId)).
                findFirst();

        assertFalse(foundKnown.isEmpty());

        NPTGLocalityXMLData localityData = foundKnown.get();

        assertEquals("Shudehill", localityData.getLocalityName());
        assertEquals(KnownLocality.ManchesterCityCentre.getLocalityId(), NPTGLocality.createId(localityData.getParentLocalityRef()));

    }


}
