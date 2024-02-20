package com.tramchester.domain.presentation.DTO.diagnostics;

import java.util.List;

public class JourneyDiagnostics {
    private final List<StationDiagnosticsDTO> dtoList;
    private final int maxNodeReasons;
    private final int maxEdgeReasons;

    public JourneyDiagnostics(List<StationDiagnosticsDTO> dtoList, int maxNodeReasons, int maxEdgeReasons) {
        this.dtoList = dtoList;
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
}
