package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.DataSourceID;
import io.dropwizard.core.Configuration;

import java.nio.file.Path;
import java.time.Duration;

@JsonDeserialize(as=RemoteDataSourceAppConfig.class)
public abstract class RemoteDataSourceConfig extends Configuration implements HasDataPath, DownloadedConfig {

    // url to check mod time against to see if newer data available
    @Override
    public abstract String getDataCheckUrl();

    // url where data is located

    @Override
    public abstract String getDataUrl();

    // default expiry when cannot check mod time via http(s)
    @Override
    public abstract Duration getDefaultExpiry();

    // TODO Should be RemoteDataSourceId
    // useful name for data set
    public abstract String getName();

    @JsonIgnore
    public DataSourceID getDataSourceId() {
        return DataSourceID.findOrUnknown(getName());
    }

    @JsonIgnore
    public abstract boolean getIsS3();

    @Override
    public String toString() {
        return "RemoteDataSourceConfig {"+
                "dataCheckURL: '"+getDataCheckUrl()+"' " +
                "dataURL: '"+getDataUrl()+"' " +
                "downloadFilename: '"+getDownloadFilename()+"' " +
                "name: '"+getName()+"' " +
                "dataSourceId: '"+getDataSourceId()+"' " +
                "isS3: '"+getIsS3()+"' " +
                "dataPath: '"+getDataPath()+"' " +
                "downloadPath: '"+getDownloadPath()+"' " +
                "defaultExpiry: '" + getDefaultExpiry() +"' " +
                "mandatory: '" + isMandatory() +"' " +
                "modTimeCheckFilename: '" + getModTimeCheckFilename() +"' " +
                "}";
    }

    // place to put downloaded file
    @Override
    public abstract Path getDownloadPath();

    // download file name
    @Override
    public abstract String getDownloadFilename();

    // if non-empty will be used in preference to getDownloadFilename for all local mod time checks
    // so allows to check mod time against a contained file rather than a downloaded zip
    @Override
    public abstract String getModTimeCheckFilename();

    @Override
    public boolean hasModCheckFilename() {
        String modCheckFilename = getModTimeCheckFilename();
        if (modCheckFilename==null) {
            return false;
        }
        return !modCheckFilename.isEmpty();
    }

    public boolean isMandatory() {
        return true;
    }

    public boolean getSkipUpload() {
        return false;
    }
}
