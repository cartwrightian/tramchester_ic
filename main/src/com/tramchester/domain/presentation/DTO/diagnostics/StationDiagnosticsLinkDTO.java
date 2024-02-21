package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;
import java.util.List;

public class StationDiagnosticsLinkDTO {
    private final LocationRefWithPosition towards;
    private final List<DiagnosticReasonDTO> reasons;
    private final boolean timeoutSeen;
    private final boolean pathTooLong;

    public StationDiagnosticsLinkDTO(LocationRefWithPosition towards, List<DiagnosticReasonDTO> reasons, EnumSet<ReasonCode> codes) {
        this.towards = towards;
        this.reasons = reasons;
        this.timeoutSeen = codes.contains(ReasonCode.TimedOut);
        pathTooLong = codes.contains(ReasonCode.PathTooLong);
    }

    public LocationRefWithPosition getTowards() {
        return towards;
    }

    public List<DiagnosticReasonDTO> getReasons() {
        return reasons;
    }

    public boolean isTimeoutSeen() {
        return timeoutSeen;
    }

    public boolean isPathTooLong() {
        return pathTooLong;
    }
}
