package com.tramchester.dataexport;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@LazySingleton
public class Zipper {
    private static final Logger logger = LoggerFactory.getLogger(Zipper.class);

    public ByteArrayOutputStream zip(Path beginning) throws IOException {
        Diagnostics diagnostics = new Diagnostics();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zipFile = new ZipOutputStream(outputStream);
        if (Files.isDirectory(beginning)) {
            compressDirToZip(zipFile, beginning.getFileName(), beginning.getParent(), diagnostics);
        } else {
            compressFileToZip(zipFile, "", beginning, diagnostics);
        }
        IOUtils.closeQuietly(zipFile);
        logger.info(diagnostics + " Compressed size " + outputStream.size());
        return outputStream;
    }

    private void compressDirToZip(ZipOutputStream zipFile, Path currentDir, final Path root, Diagnostics diagnostics) throws IOException {
        logger.debug("Compress files in '" + currentDir + "' root '" + root + "'");
        diagnostics.incDirs();

        File absoluteLocation = root.resolve(currentDir).toFile();

        File[] files = absoluteLocation.listFiles();
        if (files==null) {
            logger.warn("No files in " + absoluteLocation);
            return;
        }

        String currentZipDir = currentDir.toString();

        // entry for dir
        ZipEntry entry = new ZipEntry(currentZipDir+File.separator);
        addModTime(absoluteLocation, entry);
        zipFile.putNextEntry(entry);
        // entries for contained files
        for(File file : files) {
            if (file.isDirectory()) {
                Path path = file.toPath().getFileName();
                compressDirToZip(zipFile, currentDir.resolve(path), root, diagnostics);
            } else {
                compressFileToZip(zipFile, currentZipDir, file.toPath(), diagnostics);
            }
        }
    }

    private void compressFileToZip(ZipOutputStream zipFile, String zipDir, Path currentFile, Diagnostics diagnostics) throws IOException {
        diagnostics.incFiles();
        logger.debug("Dir '" + zipDir + "' add file '" + currentFile + "'");
        Path filename = currentFile.getFileName();


        String entryName = zipDir.isEmpty() ? filename.toString() : zipDir + File.separator + filename;
        ZipEntry entry = new ZipEntry(entryName);

        File file = currentFile.toFile();

        addModTime(file, entry);
        diagnostics.incSize(file.length());

        zipFile.putNextEntry(entry);

        FileInputStream in = new FileInputStream(file);
        IOUtils.copy(in, zipFile);
        IOUtils.closeQuietly(in);

    }

    private static void addModTime(File file, ZipEntry entry) {
        FileTime fileTime = FileTime.fromMillis(file.lastModified());
        entry.setLastModifiedTime(fileTime);
    }

    private static class Diagnostics {
        private final AtomicInteger files;
        private final AtomicInteger dirs;
        private final AtomicLong size;

        private Diagnostics() {
            files = new AtomicInteger(0);
            dirs = new AtomicInteger(0);
            size = new AtomicLong(0);
        }

        public void incDirs() {
            dirs.getAndIncrement();
        }

        public void incFiles() {
            files.getAndIncrement();
        }

        public void incSize(long length) {
            size.addAndGet(length);
        }

        @Override
        public String toString() {
            return String.format("Dirs %s Files %s Uncompressed size %s bytes",
                    dirs.get(), files.get(), size.get());
        }
    }

}
