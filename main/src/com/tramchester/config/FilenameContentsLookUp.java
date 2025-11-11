package com.tramchester.config;

import org.apache.commons.text.lookup.StringLookup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FilenameContentsLookUp implements StringLookup {
    private final Path configDir;

    FilenameContentsLookUp(Path configDir) {
        this.configDir = configDir;
    }

    @Override
    public String apply(final String key) {
        final Path path = configDir.resolve(key);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Could not find file matching " + path.toAbsolutePath());
        }
        StringBuilder result  = new StringBuilder();
        try (FileReader reader = new FileReader(path.toFile())) {
            BufferedReader bufferedReader = new BufferedReader(reader);
            while (bufferedReader.ready()) {
                result.append(bufferedReader.readLine());
                if (bufferedReader.ready()) {
                    result.append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load from " + path.toAbsolutePath(), e);
        }
        return result.toString();
    }

    @Override
    public String lookup(String key) {
        return apply(key);
    }
}
