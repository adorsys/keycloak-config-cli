package de.adorsys.keycloak.config.provider;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
final class FileUtils {

    static final String REGEX_FILE_NAME_EXTENSION_SPLITTER = "\\.(?=[^\\.]+$)";

    public static Collection<File> extractFile(File src) throws IOException {
        Assert.notNull(src, "The source file to extract cannot be null!");

        String fileExt = FilenameUtils.getExtension(src.getName());

        switch (fileExt) {
            case "zip":
                return FileUtils.extractZipFile(src);
            default:
                return Arrays.asList(src);
        }
    }

    public static File createTempFile(String name, InputStream inputStream) throws IOException {
        Assert.notNull(name, "The name of the file to create must be not null!");

        String[] splitted = name.split(REGEX_FILE_NAME_EXTENSION_SPLITTER);
        Path tempFile = Files.createTempFile(splitted[0], "." + splitted[1]);
        OutputStream outputStream = Files.newOutputStream(tempFile);
        StreamUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return tempFile.toFile();
    }

    private static Collection<File> extractZipFile(File zipFile) {
        Assert.notNull(zipFile, "The source zip file to extract cannot be null!");

        Collection<File> result = new ArrayList<>();
        try {
            ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream inputStream = zip.getInputStream(entry);
                    result.add(createTempFile(entry.getName(), inputStream));
                }
            }
            zip.close();
        } catch (IOException ioex) {
            log.error("Unable to extract zip file!", ioex);
        }
        return result;
    }
}
