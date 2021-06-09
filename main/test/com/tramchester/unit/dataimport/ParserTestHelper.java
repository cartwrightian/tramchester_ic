package com.tramchester.unit.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.dataimport.DataLoader;

import java.io.StringReader;
import java.nio.file.Paths;

public class ParserTestHelper<T> {
    private DataLoader<T> dataDataLoader;
    private String header;

    protected void before(Class<T> klass, String header) {
        CsvMapper mapper = CsvMapper.builder().build();
        this.header = header;
        dataDataLoader = new DataLoader<>(Paths.get("unused"), klass, mapper);
    }

    protected T parse(String text) {
        StringReader reader = new StringReader(header+System.lineSeparator()+text+System.lineSeparator());
        return dataDataLoader.load(reader).findFirst().orElseThrow();
    }
}
