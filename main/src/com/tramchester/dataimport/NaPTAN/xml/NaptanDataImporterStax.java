package com.tramchester.dataimport.NaPTAN.xml;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;

import javax.inject.Inject;
import java.nio.file.Path;

public class NaptanDataImporterStax extends NaptanDataImporter {


    @Inject
    public NaptanDataImporterStax(RemoteDataAvailable remoteDataRefreshed, TramchesterConfig config, FetchDataFromUrl.Ready dataIsReady) {
        super(remoteDataRefreshed, config, dataIsReady);
    }

    @Override
    void loadDataFromFile(Path filePath, ElementsFromXMLFile.XmlElementConsumer<NaptanStopData> consumer) {

    }
}
