package com.tramchester.domain.presentation.DTO;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigDTO {
    private List<TransportMode> modes;
    private boolean postcodesEnabled;
    private int numberJourneysToDisplay;
    private String environmentName;

    public ConfigDTO(Collection<TransportMode> modes, TramchesterConfig config) {
        this.modes = new ArrayList<>(modes);
        this.postcodesEnabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);;
        this.numberJourneysToDisplay = config.getMaxNumResults();
        this.environmentName = config.getEnvironmentName();
    }

    public ConfigDTO() {
        // deserialisation
    }

    public List<TransportMode> getModes() {
        return modes;
    }

    public boolean getPostcodesEnabled() {
        return postcodesEnabled;
    }

    public int getNumberJourneysToDisplay() {
        return numberJourneysToDisplay;
    }

    @Override
    public String toString() {
        return "ConfigDTO{" +
                "modes=" + modes +
                ", postcodesEnabled=" + postcodesEnabled +
                ", numberJourneysToDisplay=" + numberJourneysToDisplay +
                ", environmentName='" + environmentName + '\'' +
                '}';
    }
}
