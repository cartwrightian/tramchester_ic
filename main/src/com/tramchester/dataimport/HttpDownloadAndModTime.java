package com.tramchester.dataimport;

import jakarta.ws.rs.HttpMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.HttpHeader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.zip.GZIPInputStream;

import static jakarta.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;

public class HttpDownloadAndModTime implements DownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(HttpDownloadAndModTime.class);

    public final static String LAST_MOD_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private final DateTimeFormatter formatter;

    public HttpDownloadAndModTime() {
        formatter = DateTimeFormatter.ofPattern(LAST_MOD_PATTERN, Locale.ENGLISH);
    }

    @Override
    public URLStatus getStatusFor(URI originalUrl, ZonedDateTime localModTime, boolean warnIfMissing, List<Pair<String, String>> headers) throws IOException, InterruptedException {

        // TODO some servers return 200 for HEAD but a redirect status for a GET
        // So cannot rely on using the HEAD request for getting final URL for a resource
        final HttpResponse<Void> response = fetchHeaders(originalUrl, localModTime, HttpMethod.HEAD,
                headers,
                HttpResponse.BodyHandlers.discarding());

        final HttpHeaders responseHeaders = response.headers();

        final Duration modDuration = getServerModMillis(response);

        int httpStatusCode = response.statusCode();

        final boolean redirect = URLStatus.isRedirectCode(httpStatusCode);

        // might update depending on redirect status
        String finalUrl = originalUrl.toString();
        if (redirect) {
            Optional<String> locationField = responseHeaders.firstValue(LOCATION);
            if (locationField.isPresent()) {
                logger.warn(format("URL: '%s' Redirect status %s and Location header '%s'",
                        originalUrl, httpStatusCode, locationField));
                finalUrl = locationField.get();
            } else {
                logger.error(format("Location header missing for redirect %s, change status code to a 404 for %s",
                        httpStatusCode, originalUrl));
                httpStatusCode = 404;
            }
        } else {
            if (httpStatusCode!=200) {
                if (warnIfMissing) {
                    logger.warn("Got error status code " + httpStatusCode + " headers follow");
                } else {
                    logger.info("Got error status code " + httpStatusCode + " headers follow");
                }
                responseHeaders.map().forEach((header, values) -> logger.info("Header: " + header + " Value: " +values));
            }
        }

        return createURLStatus(finalUrl, modDuration, httpStatusCode, redirect);
    }

    private Duration getServerModMillis(final HttpResponse<?> response) {
        final HttpHeaders headers = response.headers();
        final Optional<String> lastModifiedHeader = headers.firstValue(LAST_MODIFIED);

        final Duration serverMod;
        if (lastModifiedHeader.isPresent()) {
            final String lastMod = lastModifiedHeader.get();

            final ZonedDateTime zonedDateTime = parseModTime(lastMod);

            serverMod = Duration.ofMillis(zonedDateTime.toInstant().toEpochMilli());

            logger.info("Mod time for " + response.uri() + " was raw text:'" + lastMod + "' parsed:"
                    + zonedDateTime + " " + serverMod + "ms status " + response.statusCode());

        } else {
            serverMod = Duration.ZERO;
            logger.warn("No mod time header for " + response.uri() + " status " + response.statusCode());
            logger.info("Headers were: ");
            headers.map().forEach((head, contents) -> logger.info(head + ": " + contents));
        }
        return serverMod;
    }

    public ZonedDateTime parseModTime(final String text) {
        return ZonedDateTime.parse(text, formatter);
    }

    private <T> HttpResponse<T> fetchHeaders(final URI uri, final ZonedDateTime localLastMod, final String method,
                                             final List<Pair<String,String>> headers,
                                             final HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {

        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().
                uri(uri).
                method(method, HttpRequest.BodyPublishers.noBody());

        if (!headers.isEmpty()) {
            // careful for logging here, might contain auth tokens or similar
            logger.info("Adding headers " + headers.size());
            headers.forEach(header -> httpRequestBuilder.setHeader(header.getKey(), header.getValue()));
        }

        if (localLastMod != URLStatus.invalidTime) {
            final ZonedDateTime httpLocalModTime = localLastMod.withZoneSameLocal(ZoneId.of("Etc/UTC"));

            final String headerIfModSince = formatter.format(httpLocalModTime);

            logger.info(format("Checking uri with %s : %s", HttpHeader.IF_MODIFIED_SINCE.name(), headerIfModSince));
            httpRequestBuilder.header(HttpHeader.IF_MODIFIED_SINCE.name(), headerIfModSince);
        }

        // setRequestProperty("Accept-Encoding", "gzip");
        if (HttpMethod.GET.equals(method)) {
            httpRequestBuilder.header(HttpHeader.ACCEPT_ENCODING.name(), "gzip");
        }

        final HttpRequest httpRequest = httpRequestBuilder.build();

        return client.send(httpRequest, bodyHandler);
    }

    @NotNull
    private URLStatus createURLStatus(String url, Duration serverMod, int httpStatusCode, boolean redirected) {
        URLStatus result;
        if (serverMod.isZero()) {
            if (!redirected) {
                logger.warn(format("No valid mod time from server, got 0, status code %s for %s", httpStatusCode, url));
            }
            result = new URLStatus(url, httpStatusCode);

        } else {
            ZonedDateTime modTime = getAsUTCZone(serverMod);
            logger.debug(format("Mod time %s, status %s for %s", modTime, url, httpStatusCode));
            result = new URLStatus(url, httpStatusCode, modTime);
        }

        if (!result.isOk()) { // && !result.isRedirect()) {
            logger.warn("Response code " + httpStatusCode + " for " + url);
        }

        return result;
    }

    @NotNull
    private ZonedDateTime getAsUTCZone(final Duration timeSinceEpoch) {
        final Instant instant = Instant.ofEpochSecond(timeSinceEpoch.getSeconds());
        return ZonedDateTime.ofInstant(instant, UTC);
    }

    @Override
    public URLStatus downloadTo(Path path, URI originalUrl, ZonedDateTime existingLocalModTime, List<Pair<String, String>> headers) {

        logger.info(format("Download from %s to %s", originalUrl, path.toAbsolutePath()));

        try {

            HttpResponse<InputStream> response = fetchHeaders(originalUrl, existingLocalModTime, HttpMethod.GET,
                    headers,
                    HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = response.statusCode();

            final boolean redirect = URLStatus.isRedirectCode(statusCode);

            // might update depending on redirect status
            if (redirect) {
                HttpHeaders responseHeaders = response.headers();
                Optional<String> locationField = responseHeaders.firstValue(LOCATION);
                if (locationField.isPresent()) {
                    logger.warn(format("URL: '%s' Redirect status %s and Location header '%s'",
                            originalUrl, statusCode, locationField));
                    String redirectURL = locationField.get();
                    return new URLStatus(redirectURL, statusCode);
                } else {
                    logger.error(format("Location header missing for redirect %s, change status code to a 404 for %s",
                            statusCode, originalUrl));
                    return new URLStatus(originalUrl, 404);
                }
            } else {
                // Not a redirect
                if (statusCode != 200) {
                    logger.warn("Status code on download not OK, got " + statusCode);
                    return new URLStatus(originalUrl, statusCode);
                }

                return downloadWhenStatusIsOK(response, path, statusCode);
            }


        } catch (IOException | InterruptedException exception) {
            String msg = format("Unable to download data from %s to %s exception %s", originalUrl, path, exception);
            logger.error(msg);
            throw new RuntimeException(msg,exception);
        }

    }

    @NotNull
    private URLStatus downloadWhenStatusIsOK(HttpResponse<InputStream> response, Path destination, int statusCode) throws IOException {

        String finalURL = response.toString();
        logger.info(format("Download is available from %s, save to %s", finalURL, destination));

        Duration serverModMillis = getServerModMillis(response);
        String contentType = getContentType(response);
        String encoding = getContentEncoding(response);
        long len = getLen(response);

        String contentDispos = getContentDispos(response);
        if (!contentDispos.isEmpty()) {
            logger.warn("Content disposition was " + contentDispos);
        }

        final String logSuffix = " for " + finalURL;
        final ZonedDateTime serverModDateTime = getAsUTCZone(serverModMillis);
        logger.info("Response last mod time is " + serverModMillis + " (" + serverModDateTime + ")");
        logger.info("Response content type '" + contentType + "'" + logSuffix);
        logger.info("Response encoding '" + encoding + "'" + logSuffix);
        logger.info("Content length is " + len + logSuffix);

        boolean gziped = "gzip".equals(encoding);

        File targetFile = destination.toFile();
        InputStream stream = getStreamFor(response.body(), gziped);
        if (len>0) {
            downloadByLength(stream, targetFile, len);
        } else {
            download(stream, targetFile);
        }

        if (!targetFile.exists()) {
            String msg = format("Failed to download from %s to %s, can't find output file", finalURL, targetFile.getAbsoluteFile());
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        URLStatus result;
        if (serverModMillis.isZero()) {
            result = new URLStatus(finalURL, statusCode);
            logger.warn("Server mod time is zero, not updating local file mod time " + logSuffix);
        } else {
            result = new URLStatus(finalURL, statusCode, getAsUTCZone(serverModMillis));
        }

        return result;
    }

    private void download(InputStream inputStream, File targetFile) throws IOException {
        int maxSize = 1000 * 1024 * 1024;

        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(targetFile);
        long received = 1;
        while (received > 0) {
            received = fos.getChannel().transferFrom(rbc, 0, maxSize);
            logger.info(format("Received %s bytes for %s", received, targetFile));
        }
        fos.close();
        rbc.close();

        long downloadedLength = targetFile.length();
        logger.info(format("Finished download, file %s size is %s", targetFile.getPath(), downloadedLength));
    }

    private void downloadByLength(InputStream inputStream, File targetFile, long len) throws IOException {

        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(targetFile);
        long received = fos.getChannel().transferFrom(rbc, 0, len);
        fos.close();
        rbc.close();

        long downloadedLength = targetFile.length();
        logger.info(format("Finished download, received %s, file %s size is %s",
                received, targetFile.getPath(), downloadedLength));
    }

    private InputStream getStreamFor(InputStream inputStream, boolean gziped) throws IOException {
        if (gziped) {
            logger.info("Response was gzip encoded, will decompress");
            return new GZIPInputStream(inputStream);
        } else {
            return inputStream;
        }
    }

    private long getLen(HttpResponse<InputStream> response) {
        OptionalLong header = response.headers().firstValueAsLong(HttpHeader.CONTENT_LENGTH.name());
        return header.orElse(0);
    }

    private String getContentEncoding(HttpResponse<?> response) {
        Optional<String> contentEncoding = response.headers().firstValue(HttpHeader.CONTENT_TYPE.name());
        return contentEncoding.orElse("");
    }

    private String getContentType(HttpResponse<?> response) {
        Optional<String> contentTypeHeader = response.headers().firstValue(HttpHeader.CONTENT_TYPE.name());
        return contentTypeHeader.orElse("");
    }

    private String getContentDispos(HttpResponse<?> response) {
        Optional<String> header = response.headers().firstValue("content-disposition");
        if (header.isPresent()) {
            return header.get();
        }
        header = response.headers().firstValue("Content-Disposition");
        return header.orElse("");
    }
}
