package com.tramchester.config;

import java.nio.file.Path;

/***
 * Linked between downloaded data, unzipped target and gtfs data source loading
 */
public interface HasDataPath {
    // path where unpacked data ends up
    Path getDataPath();
}
