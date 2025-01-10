package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.time.Duration;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = false)
public class RemoteDataSourceAppConfig extends RemoteDataSourceConfig {

    private final String dataCheckURL;
    private final String dataURL;
    private final Path dataPath;
    private final Path downloadPath;
    private final String filename;
    private final String name;
    private final Integer defaultExpiryDays;
    private final String modTimeCheckFilename;
    private final Boolean mandatory;
    private final Boolean skipUpload;
    private final Boolean checkOnlyIfExpired;

    @JsonCreator
    public RemoteDataSourceAppConfig(@JsonProperty(value = "dataCheckURL", required = true) String dataCheckURL,
                                     @JsonProperty(value = "dataURL", required = true) String dataURL,
                                     @JsonProperty(value = "dataPath", required = true) Path dataPath,
                                     @JsonProperty(value = "downloadPath", required = true) Path downloadPath,
                                     @JsonProperty(value = "filename", required = true) String filename,
                                     @JsonProperty(value = "name", required = true) String name,
                                     @JsonProperty(value = "defaultExpiryDays", required = true) Integer defaultExpiryDays,
                                     @JsonProperty(value = "modTimeCheckFilename") String modTimeCheckFilename,
                                     @JsonProperty(value = "mandatory") Boolean mandatory,
                                     @JsonProperty(value = "skipUpload") Boolean skipUpload,
                                     @JsonProperty(value = "checkOnlyIfExpired") Boolean checkOnlyIfExpired) {
        this.dataCheckURL = dataCheckURL;
        this.dataURL = dataURL;
        this.dataPath = dataPath;
        this.downloadPath = downloadPath;
        this.filename = filename;
        this.name = name;
        this.defaultExpiryDays = defaultExpiryDays;
        this.modTimeCheckFilename = modTimeCheckFilename;
        this.mandatory = mandatory;
        this.skipUpload = skipUpload;
        this.checkOnlyIfExpired = checkOnlyIfExpired;
    }

    @Override
    public String getDataCheckUrl() {
        return dataCheckURL;
    }

    @Override
    public String getDataUrl() {
        return dataURL;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(defaultExpiryDays);
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public Path getDownloadPath() {
        return downloadPath;
    }

    @Override
    public String getDownloadFilename() {
        return filename;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getModTimeCheckFilename() {
        return modTimeCheckFilename;
    }

    @Override
    public boolean isMandatory() {
        if (mandatory==null) {
            return true;
        }
        return mandatory;
    }

    @Override
    public boolean checkOnlyIfExpired() {
        if (checkOnlyIfExpired==null) {
            return false;
        }
        return checkOnlyIfExpired;
    }

    @Override
    public boolean getSkipUpload() {
        if (skipUpload==null) {
            return false;
        }
        return skipUpload;
    }
}
