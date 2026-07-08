package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

public class XmlFilteredStreamReader {
    final XMLStreamReader contained;
    private final XmlFactory xmlFactory;
    private final String topLevelElement;

    public XmlFilteredStreamReader(final XMLStreamReader contained, final XmlFactory xmlFactory, final String topLevelElement) {
        this.contained = contained;
        this.xmlFactory = xmlFactory;
        this.topLevelElement = topLevelElement;
    }

    public boolean matches() throws XMLStreamException {
        while (contained.hasNext()) {
            if (contained.isStartElement()) {
                final String localName = contained.getLocalName();
                if (localName.equals(topLevelElement)) {
                    return true;
                }
            }
            contained.next();
        }
        return false;
    }

    public void close() throws XMLStreamException {
        contained.close();
    }

    public FromXmlParser getParser() throws IOException {
         return xmlFactory.createParser(contained);
    }
}
