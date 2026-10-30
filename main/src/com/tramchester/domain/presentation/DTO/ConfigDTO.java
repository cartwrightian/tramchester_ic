package com.tramchester.domain.presentation.DTO;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

import java.util.List;

public class ConfigDTO {
    private List<TransportMode> modes;
    private List<TransportMode> liveDataModes;
    private boolean postcodesEnabled;
    private boolean modeSelectionEnabled;
    private int numberJourneysToDisplay;
    private String environmentName;
    private int maxNumberChanges;

    public ConfigDTO(final ImmutableEnumSet<TransportMode> modes, final TramchesterConfig config,
                     final boolean modeSelectionEnabled, final ImmutableEnumSet<TransportMode> liveDataModes) {
        this.modes = modes.createList();
        this.postcodesEnabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);
        this.numberJourneysToDisplay = config.getMaxNumberResults();
        this.environmentName = config.getEnvironmentName();
        this.maxNumberChanges = config.getMaxNumberChanges();
        this.modeSelectionEnabled = modeSelectionEnabled;
        this.liveDataModes = liveDataModes.createList();
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
                ", liveDataModes=" + liveDataModes +
                ", postcodesEnabled=" + postcodesEnabled +
                ", modeSelectionEnabled=" + modeSelectionEnabled +
                ", numberJourneysToDisplay=" + numberJourneysToDisplay +
                ", environmentName='" + environmentName + '\'' +
                ", maxNumberChanges=" + maxNumberChanges +
                '}';
    }

    public int getMaxNumberChanges() {
        return maxNumberChanges;
    }

    public boolean getModeSelectionEnabled() {
        return modeSelectionEnabled;
    }

    public List<TransportMode> getLiveDataModes() {
        return liveDataModes;
    }
}
