package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;

import java.util.List;

public class JourneyDiagnostics {
    private final List<StationDiagnosticsDTO> dtoList;
    private final List<LocationRefWithPosition> destinations;
    private final int maxNodeReasons;
    private final int maxEdgeReasons;

    public JourneyDiagnostics(List<StationDiagnosticsDTO> dtoList, List<LocationRefWithPosition> destinations, int maxNodeReasons, int maxEdgeReasons) {
        this.dtoList = dtoList;
        this.destinations = destinations;
        this.maxNodeReasons = maxNodeReasons;
        this.maxEdgeReasons = maxEdgeReasons;
    }

    public List<StationDiagnosticsDTO> getDtoList() {
        return dtoList;
    }

    public int getMaxNodeReasons() {
        return maxNodeReasons;
    }

    public int getMaxEdgeReasons() {
        return maxEdgeReasons;
    }

    public List<LocationRefWithPosition> getDestinations() {
        return destinations;
    }
}
