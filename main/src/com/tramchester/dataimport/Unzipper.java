package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@LazySingleton
public class Unzipper {
    private static final Logger logger = LoggerFactory.getLogger(Unzipper.class);

    final PathMatcher zipMatcher = FileSystems.getDefault().getPathMatcher("glob:**.zip");

    public boolean unpackIfZipped(final Path filename, final Path targetDirectory) {
        final File zipFile = filename.toFile();

        try {
            if (zipMatcher.matches(filename)) {
                if (alreadyPresent(filename, targetDirectory)) {
                    logger.info("Already unzipped " + filename.toAbsolutePath() + " (based on mod time)");
                    return true;
                }

                int entries = 0;
                logger.info(format("Unzipping data from %s to %s ", filename, targetDirectory));
                final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    entries++;
                    extractEntryTo(targetDirectory, zipEntry, zipInputStream);
                    zipInputStream.closeEntry();
                    zipEntry = zipInputStream.getNextEntry();
                }
                zipInputStream.close();
                if (entries==0) {
                    logger.error("Unzipped zero entries, was this a zip file? " + filename);
                }
            } else {
                logger.info(format("Skipping unzip, %s not a zip file", zipFile.getAbsoluteFile()));
            }
            // update mod time
            updateFileModTime(targetDirectory, zipFile);

            return true;
        } catch (ZipException zipException) {
            logger.error("Unable to unzip, zip exception " + filename, zipException);
            return false;
        }
        catch (FileNotFoundException fileNotFoundException) {
            logger.error("File is missing " + filename, fileNotFoundException);
            return false;
        } catch (IOException ioException) {
            logger.error("IOException while processing zip file " + filename, ioException);
            return false;
        }

    }

    private boolean alreadyPresent(final Path zipfile, final Path target) {
        final File targetFile = target.toFile();
        if (targetFile.exists()) {
            final long targetMod = targetFile.lastModified();
            final long zipMod = zipfile.toFile().lastModified();
            return zipMod == targetMod;
        }
        return false;
    }

    private void updateFileModTime(final Path targetDirectory, final File zipFile) {
        final long zipMod = zipFile.lastModified();
        logger.info("Set '" + targetDirectory.toAbsolutePath() + "' mod time to: " + zipMod);
        final boolean undatedModTime = targetDirectory.toFile().setLastModified(zipMod);
        if (!undatedModTime) {
            logger.warn("Could not update the modification time of " + targetDirectory.toAbsolutePath());
        }
    }

    private void extractEntryTo(final Path targetDirectory, final ZipEntry zipEntry, final ZipInputStream zipInputStream) throws IOException {
        final Path target = targetDirectory.resolve(zipEntry.getName());
        if (logger.isDebugEnabled()) {
            logger.debug("Extracting entry " + toLogString(zipEntry));
        }

        final String absolutePath = target.toAbsolutePath().toString();
        if (zipEntry.isDirectory()) {
            logger.debug("Create directory " + absolutePath);
            Files.createDirectories(target);
            return;
        }

        logger.debug("Unpack file " + absolutePath);
        final Path parent = target.getParent();
        if (!parent.toFile().exists()) {
            logger.info("Create needed directory " + parent + " for " +absolutePath);
            Files.createDirectories(parent);
        }

        final File unpackTarget = target.toFile();
        if (unpackTarget.exists()) {
            logger.debug(absolutePath + " already exists");

            // file size check is cheaper than mod time check
            if (checkFileSize(zipEntry, unpackTarget)) {
                if (checkModTime(zipEntry, unpackTarget)) {
                    logger.debug("Not over-writing " + absolutePath);
                    return;
                }
            }

            logger.debug("Deleting " + absolutePath);
            Files.delete(target);
        }

        try {
            Files.copy(zipInputStream, target);
            boolean setModTime = unpackTarget.setLastModified(zipEntry.getLastModifiedTime().toMillis());
            if (!setModTime) {
                logger.warn("Could not set mod time on " + absolutePath);
            }
        } catch (IOException e) {
            logger.error("Exception while extracting entry :'" + toLogString(zipEntry) + "' to '" + absolutePath + "'");
        }


    }

    // toString on zipEntry is just the name../.
    private String toLogString(final ZipEntry zipEntry) {
        return String.format("zipEntry{name:%s size:%s comp size: %s method:%s}", zipEntry.getName(), zipEntry.getSize(),
                zipEntry.getCompressedSize(), zipEntry.getMethod());
    }

    private boolean checkFileSize(final ZipEntry zipEntry, final File file) {
        if (zipEntry.getSize()==-1) {
            logger.info("No size present in zip for " + file);
            return true;
        }

        final boolean sizeMatches = zipEntry.getSize() == file.length();
        if (!sizeMatches) {
            logger.warn(format("File %s exists but size (%s) does not match (%s)",
                    file, file.length(), zipEntry.getSize()));
        } else {
            logger.debug(format("File %s exists size (%s) matches (%s)",
                    file, file.length(), zipEntry.getSize()));
        }
        return sizeMatches;
    }

    private boolean checkModTime(final ZipEntry zipEntry, final File file) {
        final Instant fromFile = Instant.ofEpochMilli(file.lastModified());
        final Instant fromZip = Instant.ofEpochMilli(zipEntry.getLastModifiedTime().toMillis());
        // only second resolution accurate here
        final boolean modTimeMatches =  fromFile.getEpochSecond() == fromZip.getEpochSecond();
        if (!modTimeMatches) {
            logger.info(format("File %s exists but mod time %s (%s) does not match %s (%s) to nearest second",
                    file,
                    fromFile, LocalDateTime.ofInstant(fromFile, TramchesterConfig.TimeZoneId),
                    fromZip, LocalDateTime.ofInstant(fromZip, TramchesterConfig.TimeZoneId)));
        }
        return modTimeMatches;
    }

    public List<Path> getContents(final Path filename) {
        final List<Path> contents = new ArrayList<>();

        final File zipFile = filename.toFile();
        try {
            if (zipMatcher.matches(filename)) {
                int entries = 0;
                logger.info(format("Listing contents data from %s", filename));
                final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    entries++;
                    contents.add(Path.of(zipEntry.getName()));
                    zipInputStream.closeEntry();
                    zipEntry = zipInputStream.getNextEntry();
                }
                zipInputStream.close();
                if (entries==0) {
                    logger.error("Unzipped zero entries, was this a zip file? " + filename);
                }
            } else {
                logger.info(format("Skipping unzip, %s not a zip file", zipFile.getAbsoluteFile()));
            }

        } catch (ZipException zipException) {
            logger.error("Unable to unzip, zip exception ", zipException);
        }
        catch (FileNotFoundException fileNotFoundException) {
            logger.error("File is missing ", fileNotFoundException);
        } catch (IOException ioException) {
            logger.error("IOException while processing zip file ", ioException);
        }

        return contents;
    }

}
