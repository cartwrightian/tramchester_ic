package com.tramchester.dataimport.NaPTAN.xml;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

@LazySingleton
public class NaptanDataImporterStax extends NaptanDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataImporterStax.class);

    @Inject
    public NaptanDataImporterStax(RemoteDataAvailable remoteDataRefreshed, TramchesterConfig config, FetchDataFromUrl.Ready dataIsReady) {
        super(remoteDataRefreshed, config, dataIsReady);
    }

    @Override
    void loadDataFromFile(Path filePath, ElementsFromXMLFile.XmlElementConsumer<NaptanStopData> consumer) {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        try {
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(filePath.toFile()));
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();
                if (nextEvent.isStartElement()) {
                    final StartElement startElement = nextEvent.asStartElement();
                    if ("StopPoint".equals(startElement.getName().getLocalPart())) {
                        Attribute statusAttrib = startElement.getAttributeByName(new QName("Status"));
                        String status = statusAttrib.getValue();
                        if ("active".equals(status)) {
                            processStopPoint(reader);
                        }
                    }
                }
                if (nextEvent.isEndElement()) {
                    EndElement endElement = nextEvent.asEndElement();
                    if ("StopPoint".equals(endElement.getName().getLocalPart())) {
                        //consumer.process();
                    }
                }
            }

        } catch (XMLStreamException | FileNotFoundException e) {
            String msg = "Unable to load from " + filePath.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void processStopPoint(XMLEventReader reader) throws XMLStreamException {
        XMLEvent nextEvent = reader.nextEvent();
        StartElement startElement = nextEvent.asStartElement();
    }
}



