package com.tramchester.dataimport;

import java.net.URI;
import java.time.LocalDateTime;

public class URLStatus {

    public static final int MOVED_PERMANENTLY = 301;
    public static final int MOVED_TEMPORARILY = 302;
    public static final int TEMPORARY_REDIRECT = 307;
    public static final int OK = 200;
    public static final int NOT_FOUND = 404;


    private final String url;
    private final int responseCode;
    private final LocalDateTime modTime;

    public URLStatus(String url, int responseCode) {
        this(url, responseCode, LocalDateTime.MIN);
    }

    public URLStatus(String url, int responseCode, LocalDateTime modTime) {
        this.url = url;
        this.responseCode = responseCode;
        this.modTime = modTime;
    }

    public URLStatus(URI uri, int responseCode) {
        this(uri, responseCode, LocalDateTime.MIN);
    }

    public URLStatus(URI uri, int responseCode, LocalDateTime modTime) {
        this(uri.toASCIIString(), responseCode, modTime);
    }

    public LocalDateTime getModTime() {
        return modTime;
    }

    public boolean hasModTime() {
        return !modTime.equals(LocalDateTime.MIN);
    }

    public boolean isOk() {
        return 200 == responseCode;
    }

    public int getStatusCode() {
        return responseCode;
    }

    public String getActualURL() {
        return url;
    }

    @Override
    public String toString() {
        return "URLStatus{" +
                "url='" + url + '\'' +
                ", responseCode=" + responseCode +
                ", modTime=" + modTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        URLStatus urlStatus = (URLStatus) o;

        if (responseCode != urlStatus.responseCode) return false;
        if (!url.equals(urlStatus.url)) return false;
        return modTime.equals(urlStatus.modTime);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + responseCode;
        result = 31 * result + modTime.hashCode();
        return result;
    }

    public boolean isRedirect() {
        return isRedirectCode(responseCode);
    }

    public static boolean isRedirectCode(int code) {
        return code == MOVED_PERMANENTLY
                || code == MOVED_TEMPORARILY
                || code == TEMPORARY_REDIRECT;
    }


}
