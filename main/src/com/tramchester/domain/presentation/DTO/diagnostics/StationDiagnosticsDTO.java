package com.tramchester.domain.presentation.DTO.diagnostics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class StationDiagnosticsDTO extends CommonDiagnosticsDTO{
    private final LocationRefWithPosition begin;
    private final List<DiagnosticReasonDTO> reasons;
    private final List<StationDiagnosticsLinkDTO> links;
    private final Set<GraphNodeId> associatedNodeIds;

    public StationDiagnosticsDTO(LocationRefWithPosition begin, List<DiagnosticReasonDTO> reasons,
                                 List<StationDiagnosticsLinkDTO> links, EnumSet<ReasonCode> codes, Set<GraphNodeId> associatedNodeIds) {
        super(codes);
        this.begin = begin;
        this.reasons = reasons;
        this.links = links;
        this.associatedNodeIds = associatedNodeIds;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDiagnosticsDTO that = (StationDiagnosticsDTO) o;
        return Objects.equals(getBegin(), that.getBegin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBegin());
    }

    @JsonIgnore
    public Set<GraphNodeId> getAssociatedNodeIds() {
        return associatedNodeIds;
    }
}
