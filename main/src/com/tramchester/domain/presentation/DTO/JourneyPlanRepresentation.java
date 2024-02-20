package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;

import java.util.Set;

public class JourneyPlanRepresentation {

    private Set<JourneyDTO> journeys;
    private JourneyDiagnostics diagnostics;

    @SuppressWarnings("unused")
    public JourneyPlanRepresentation() {
        // deserialisation
    }

    public JourneyPlanRepresentation(Set<JourneyDTO> journeys) {
        this.journeys = journeys;
    }

    public Set<JourneyDTO> getJourneys() {
        return journeys;
    }

    public void addDiag(JourneyDiagnostics diagnostics) {

        this.diagnostics = diagnostics;
    }

    public JourneyDiagnostics getDiagnostics() {
        return diagnostics;
    }
}
