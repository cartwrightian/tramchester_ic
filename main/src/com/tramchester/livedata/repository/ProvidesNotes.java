package com.tramchester.livedata.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;

import java.util.List;
import java.util.Set;

@ImplementedBy(ProvidesTramNotes.class)
public interface ProvidesNotes {
    List<Note> createNotesForStations(Set<Station> stations, TramDate queryDate, TramTime time);
}
