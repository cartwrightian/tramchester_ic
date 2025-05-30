package com.tramchester.testSupport.reference;

import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

public enum KnownLocations implements HasGridPosition {
    nearAltrincham(53.387483D, -2.351463D),
    nearAltrinchamInterchange(53.3873279D,-2.3498573D),
    nearPiccGardens(53.4805248D, -2.2394929D),
    nearShudehill(53.485846, -2.239472),
    atMancArena(53.4871468,-2.2445687),
    nearStockportBus(53.408735,-2.1656593),
    nearGreenwichLondon(51.477928, -0.001545),
    nearKnutsfordBusStation(53.3026112D,-2.3774635D),
    nearStPetersSquare(53.4776898D,-2.2432105D),
    nearWythenshaweHosp(53.3874309,-2.2945628),
    nearBury(53.5913661,-2.3006942),
    atRoundthornTram(53.389264, -2.2971255);

    private final LatLong latLong;
    private final GridPosition gridPosition;

    KnownLocations(double lat, double lon) {
        latLong = new LatLong(lat,lon);
        gridPosition = CoordinateTransforms.getGridPosition(latLong);
    }

    public MyLocation location() {
        return new MyLocation(latLong);
    }

    public LatLong latLong() {
        return latLong;
    }

    public GridPosition grid() {
        return gridPosition;
//        return CoordinateTransforms.getGridPosition(latLong);
    }

    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }
}
