package com.tramchester.domain.presentation.DTO;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

import java.util.ArrayList;
import java.util.List;

public class ConfigDTO {
    private List<TransportMode> modes;
    private boolean postcodesEnabled;
    private int numberJourneysToDisplay;
    private String environmentName;
    private int maxNumberChanges;

    public ConfigDTO(final ImmutableEnumSet<TransportMode> modes, final TramchesterConfig config) {
        this.modes = new ArrayList<>(ImmutableEnumSet.createEnumSet(modes));
        this.postcodesEnabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);
        this.numberJourneysToDisplay = config.getMaxNumberResults();
        this.environmentName = config.getEnvironmentName();
        this.maxNumberChanges = config.getMaxNumberChanges();
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

    public int getMaxNumberChanges() {
        return maxNumberChanges;
    }
}
