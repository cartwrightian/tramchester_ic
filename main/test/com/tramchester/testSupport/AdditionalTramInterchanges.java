package com.tramchester.testSupport;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

public class AdditionalTramInterchanges {

//    private enum Interchanges {
//
//        // official interchange points not auto-detected by InterchangeRepository, see config for tram routing also
//        Piccadilly("9400ZZMAPIC");
//        //Deansgate("9400ZZMAGMX") // detected auto during York Street closures
//        //MediacityUK("9400ZZMAMCU"),
//
//        private final String stationId;
//
//        Interchanges(String stationId) {
//            this.stationId = stationId;
//        }
//    }

    public static IdSet<Station> stations() {
        return IdSet.emptySet();
        //return Arrays.stream(Interchanges.values()).map(id -> Station.createId(id.stationId)).collect(IdSet.idCollector());
    }

}
