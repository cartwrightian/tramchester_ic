package com.tramchester.integration.testSupport.tram;

import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.reference.TramStations;

import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;

public class CentralStationsSubGraph {
    private static final List<TramStations> CentralStations = Arrays.asList(
            Cornbrook,
            Deansgate,
            StPetersSquare,
            ExchangeSquare,
            Victoria,
            PiccadillyGardens,
            Piccadilly,
            MarketStreet,
            Shudehill);


    public static void configureFilter(ConfigurableGraphFilter graphFilter, TransportData transportData) {
        CentralStations.forEach(station -> graphFilter.addStation(station.getId()));
    }
}
