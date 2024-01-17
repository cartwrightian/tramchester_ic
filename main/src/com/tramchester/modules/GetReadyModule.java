package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.graph.AddNeighboursGraphBuilder;
import com.tramchester.graph.AddDiversionsForClosedGraphBuilder;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.repository.TransportData;
import com.tramchester.dataimport.loader.TransportDataFactory;

@SuppressWarnings("unused")
public class GetReadyModule extends AbstractModule {
    @Provides
    TransportData providesTransportdata(TransportDataFactory transportDataFactory) {
        return transportDataFactory.getData();
    }

    @Provides
    StationsAndLinksGraphBuilder.Ready providesReadyToken(StationsAndLinksGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    StagedTransportGraphBuilder.Ready providesReadyToken(StagedTransportGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    StationGroupsGraphBuilder.Ready providesReadyToken(StationGroupsGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    AddNeighboursGraphBuilder.Ready providesReadyToken(AddNeighboursGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    FetchDataFromUrl.Ready providesReadyToken(FetchDataFromUrl fetchDataFromUrl) {
        return fetchDataFromUrl.getReady();
    }

    @Provides
    UnzipFetchedData.Ready providesReadyToken(UnzipFetchedData unzipFetchedData) {
        return unzipFetchedData.getReady();
    }

    @Provides
    AddDiversionsForClosedGraphBuilder.Ready providesReadyToken(AddDiversionsForClosedGraphBuilder addWalksForClosedGraphBuilder) {
        return addWalksForClosedGraphBuilder.getReady();
    }

}
