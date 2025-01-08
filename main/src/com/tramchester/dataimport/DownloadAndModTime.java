package com.tramchester.dataimport;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

public interface DownloadAndModTime {
    URLStatus getStatusFor(URI uri, ZonedDateTime localModTime, boolean warnIfMissing, List<Pair<String, String>> headers) throws IOException, InterruptedException;

    URLStatus downloadTo(Path path, URI uri, ZonedDateTime localModTime, List<Pair<String, String>> headers) throws IOException;

}
