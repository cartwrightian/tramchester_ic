package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.download.AuthenticateOpenRailData;
import com.tramchester.domain.DataSourceID;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@LazySingleton
public class HeaderForDatasourceFactory {
    private final AuthenticateOpenRailData authenticateOpenRailData;

    @Inject
    public HeaderForDatasourceFactory(AuthenticateOpenRailData authenticateOpenRailData) {
        this.authenticateOpenRailData = authenticateOpenRailData;
    }

    public List<Pair<String, String>> getFor(final DataSourceID dataSourceID) {
        if (dataSourceID==DataSourceID.openRailData) {
            final String token = authenticateOpenRailData.getToken();
            if (token.isEmpty()) {
                return Collections.emptyList();
            }
            ArrayList<Pair<String,String>> headers = new ArrayList<>();
            headers.add(Pair.of("X-Auth-Token", token));
            return headers;
        } else {
            return Collections.emptyList();
        }
    }
}
