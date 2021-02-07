package de.adorsys.keycloak.config.provider;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
final class FileUtils {


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

    public static Collection<File> extractZipFile(File zipFile) {
        Assert.notNull(zipFile, "The source zip file to extract cannot be null!");

        Collection<File> result = new ArrayList<>();
        try {
            ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream inputStream = zip.getInputStream(entry);
                    File file = createTempFile(entry.getName(), inputStream);
                    result.add(file);
                }
            }
            zip.close();
        } catch (IOException ioex) {
            log.error("Unable to handle zip file!", ioex);
        }
        return result;
    }

    public static File createTempFile(String name, InputStream inputStream) throws IOException {
        File file = new File(name);
        file.deleteOnExit();

        OutputStream outputStream = new FileOutputStream(file);
        StreamUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return file;
    }
}
