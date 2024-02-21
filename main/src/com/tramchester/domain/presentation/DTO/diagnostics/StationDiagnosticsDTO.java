package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;
import java.util.List;

public class StationDiagnosticsDTO {
    private final LocationRefWithPosition begin;
    private final List<DiagnosticReasonDTO> reasons;
    private final List<StationDiagnosticsLinkDTO> links;
    private final boolean timeoutSeen;
    private final boolean pathTooLong;

    public StationDiagnosticsDTO(LocationRefWithPosition begin, List<DiagnosticReasonDTO> reasons,
                                 List<StationDiagnosticsLinkDTO> links, EnumSet<ReasonCode> codes) {
        this.begin = begin;
        this.reasons = reasons;
        this.links = links;
        this.timeoutSeen = codes.contains(ReasonCode.TimedOut);
        this.pathTooLong = codes.contains(ReasonCode.PathTooLong);

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

    public boolean isTimeoutSeen() {
        return timeoutSeen;
    }

    public boolean isPathTooLong() {
        return pathTooLong;
    }
}
