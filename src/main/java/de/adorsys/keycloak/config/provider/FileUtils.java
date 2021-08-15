/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.provider;


import de.adorsys.keycloak.config.exception.ImportProcessingException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class FileUtils {
    FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Collection<File> extractFile(File src) {
        Assert.notNull(src, "The source file to extract cannot be null!");

        String fileExt = FilenameUtils.getExtension(src.getName());

        if (fileExt.equals("zip")) {
            return FileUtils.extractZipFile(src);
        }

        return Collections.singletonList(src);
    }

    public static File createTempFile(String name, InputStream inputStream) throws IOException {
        Assert.notNull(name, "The name of the file to create must be not null!");

        String fileName = FilenameUtils.getBaseName(name);
        String fileExt = FilenameUtils.getExtension(name);

        File tempFile = File.createTempFile(fileName, "." + fileExt);
        tempFile.deleteOnExit();

        OutputStream outputStream = Files.newOutputStream(tempFile.toPath());
        StreamUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return tempFile;
    }

    private static Collection<File> extractZipFile(File zipFile) {
        Assert.notNull(zipFile, "The source zip file to extract cannot be null!");

        Collection<File> result = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipFile, ZipFile.OPEN_READ)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream inputStream = zip.getInputStream(entry);
                    result.add(createTempFile(entry.getName(), inputStream));
                }
            }
        } catch (IOException ex) {
            throw new ImportProcessingException("Unable to extract zip file '" + zipFile.getAbsolutePath() + "'!", ex);
        }
        return result;
    }
}
