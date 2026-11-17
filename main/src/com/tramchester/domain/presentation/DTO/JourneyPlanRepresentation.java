package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;

import java.util.Set;

public class JourneyPlanRepresentation {

    private Set<IdForDTO> allChangeStations;
    private Set<JourneyDTO> journeys;
    private JourneyDiagnostics diagnostics;

    @SuppressWarnings("unused")
    public JourneyPlanRepresentation() {
        // deserialisation
    }

    public JourneyPlanRepresentation(final Set<JourneyDTO> journeys,
                                     final Set<IdForDTO> allChangeStations) {
        this.journeys = journeys;
        this.allChangeStations = allChangeStations;
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

    public Set<IdForDTO> getAllChangeStations() {
        return allChangeStations;
    }
}
