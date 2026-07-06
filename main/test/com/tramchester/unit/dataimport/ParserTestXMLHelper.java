package com.tramchester.unit.dataimport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParserTestXMLHelper<T> {

    private final XmlMapper mapper;
    private ElementsFromXMLFile<T> loader;
    private List<T> received;
    private final Class<T> elementType;

    public ParserTestXMLHelper(Class<T> elementType) {
        this.elementType = elementType;
        mapper = XmlMapper.builder().
                addModule(new BlackbirdModule()).
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).
                disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).
                build();
    }

    protected void before(Charset charset) {
        received = new ArrayList<>();
        Consumer<T> consumer = item -> received.add(item);
        loader = new ElementsFromXMLFile<>(Paths.get("unused"), charset, mapper,
                new ElementsFromXMLFile.XmlElementConsumer<>(elementType, consumer) {

                    @Override
                    protected boolean shouldInclude(T item) {
                        return true;
                    }
                });
    }

    protected T parseFirstOnly(final String text) throws XMLStreamException, IOException {
        final StringReader reader = new StringReader(text);
        loader.load(reader);

        return received.getFirst();

    }

    protected List<T> parseAll(final String text) throws XMLStreamException, IOException {
        final StringReader reader = new StringReader(text);
        loader.load(reader);
        return received;
    }
}
