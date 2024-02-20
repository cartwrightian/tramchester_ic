package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;

import java.util.List;

public class StationDiagnosticsLinkDTO {
    private final LocationRefWithPosition towards;
    private final List<DiagnosticReasonDTO> reasons;

    public StationDiagnosticsLinkDTO(LocationRefWithPosition towards, List<DiagnosticReasonDTO> reasons) {
        this.towards = towards;
        this.reasons = reasons;
    }

    public LocationRefWithPosition getTowards() {
        return towards;
    }

    public List<DiagnosticReasonDTO> getReasons() {
        return reasons;
    }
}
