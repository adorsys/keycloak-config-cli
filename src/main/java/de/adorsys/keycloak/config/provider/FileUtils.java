package de.adorsys.keycloak.config.provider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.util.StreamUtils;

@Slf4j
final class ZipFileUtils {

    public static Collection<File> extract(File zipFile) {
        Collection<File> result = new ArrayList<>();
        try {
            ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    File file = new File(entry.getName());
                    file.deleteOnExit();

                    InputStream inputStream = zip.getInputStream(entry);
                    OutputStream outputStream = new FileOutputStream(file);
                    StreamUtils.copy(inputStream, outputStream);
                    inputStream.close();
                    outputStream.close();
                    result.add(file);
                }
            }
            zip.close();
        } catch (IOException ioex) {
            log.error("Unable to handle zip file!", ioex);
        }
        return result;
    }
}
