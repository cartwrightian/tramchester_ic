package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;

public class CommonDiagnosticsDTO {
    private final boolean pathTooLong;
    private final boolean arrived;

    public CommonDiagnosticsDTO(EnumSet<ReasonCode> codes) {
        pathTooLong = codes.contains(ReasonCode.PathTooLong);
        arrived = codes.contains(ReasonCode.Arrived);
    }

    public boolean isPathTooLong() {
        return pathTooLong;
    }

    public boolean isArrived() {
        return arrived;
    }
}
