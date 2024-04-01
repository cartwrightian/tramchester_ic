package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.EnumSet;

public class CommonDiagnosticsDTO {
    private final EnumSet<ReasonCode> codes;

    public CommonDiagnosticsDTO(final EnumSet<ReasonCode> codes) {
        this.codes = codes;
    }

    public EnumSet<ReasonCode> getCodes() {
        return codes;
    }
}
