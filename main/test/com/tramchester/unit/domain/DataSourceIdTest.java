package com.tramchester.unit.domain;

import com.tramchester.domain.DataSourceID;
import com.tramchester.graph.GraphPropertyKey;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.DataSourceID.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataSourceIdTest {


    @Test
    void shouldHaveMappingDataSourceIdToGraphPropKeys() {
        DataSourceID.InDatabase().forEach(DataSourceID::getGraphKey);

        assertEquals(GraphPropertyKey.NAPTAN_VERSION, DataSourceID.getGraphKey(naptanxml));
        assertEquals(GraphPropertyKey.NPTG_VERSION, DataSourceID.getGraphKey(nptg));
        assertEquals(GraphPropertyKey.TFGM_VERSION, DataSourceID.getGraphKey(tfgm));
        assertEquals(GraphPropertyKey.OPENRAILDATA_VERSION, DataSourceID.getGraphKey(openRailData));
        assertEquals(GraphPropertyKey.POSTCODE_VERSION, DataSourceID.getGraphKey(postcode));
    }

    @Test
    void shouldThrowForUndefined() {
        Set<DataSourceID> badValues = Arrays.stream(values()).filter(item -> !DataSourceID.InDatabase().contains(item)).collect(Collectors.toSet());

        for (DataSourceID badValue : badValues) {
            assertThrows(RuntimeException.class, () -> DataSourceID.getGraphKey(badValue));
        }

    }
}
