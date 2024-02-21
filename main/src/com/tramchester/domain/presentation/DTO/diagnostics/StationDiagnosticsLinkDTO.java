package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;
import java.util.List;

public class StationDiagnosticsLinkDTO extends CommonDiagnosticsDTO {
    private final LocationRefWithPosition towards;
    private final List<DiagnosticReasonDTO> reasons;

    public StationDiagnosticsLinkDTO(LocationRefWithPosition towards, List<DiagnosticReasonDTO> reasons, EnumSet<ReasonCode> codes) {
        super(codes);
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
