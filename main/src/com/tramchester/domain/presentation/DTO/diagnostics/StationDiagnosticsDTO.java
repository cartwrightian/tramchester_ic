package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;
import java.util.List;

public class StationDiagnosticsDTO extends CommonDiagnosticsDTO{
    private final LocationRefWithPosition begin;
    private final List<DiagnosticReasonDTO> reasons;
    private final List<StationDiagnosticsLinkDTO> links;

    public StationDiagnosticsDTO(LocationRefWithPosition begin, List<DiagnosticReasonDTO> reasons,
                                 List<StationDiagnosticsLinkDTO> links, EnumSet<ReasonCode> codes) {
        super(codes);
        this.begin = begin;
        this.reasons = reasons;
        this.links = links;
    }

    public LocationRefWithPosition getBegin() {
        return begin;
    }

    public List<DiagnosticReasonDTO> getReasons() {
        return reasons;
    }

    public List<StationDiagnosticsLinkDTO> getLinks() {
        return links;
    }

}
