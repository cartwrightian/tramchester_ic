package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.Duration;

@SuppressWarnings("unused")
@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class RemoteDataSourceAppConfig extends RemoteDataSourceConfig {

    @NotNull
    @JsonProperty(value = "dataCheckURL")
    private String dataCheckURL;

    @NotNull
    @JsonProperty(value = "dataURL")
    private String dataURL;

    @NotNull
    @JsonProperty(value = "dataPath")
    private Path dataPath;

    @NotNull
    @JsonProperty(value = "downloadPath")
    private Path downloadPath;

    @NotNull
    @JsonProperty(value = "filename")
    private String filename;

    @NotNull
    @JsonProperty(value = "name")
    private String name;

    @NotNull
    @JsonProperty(value = "defaultExpiryDays")
    private Integer defaultExpiryDays;

    @JsonProperty(value = "modTimeCheckFilename")
    private String modTimeCheckFilename;

    @JsonProperty(value = "mandatory")
    private Boolean mandatory;

    @JsonProperty(value = "skipUpload")
    private Boolean skipUpload;

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
    public boolean getIsS3() {
        return dataURL.startsWith("s3://");
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
    public boolean getSkipUpload() {
        if (skipUpload==null) {
            return false;
        }
        return skipUpload;
    }
}
