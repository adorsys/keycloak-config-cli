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


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

final class FileUtils {
    FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    static final Path CWD = Paths.get(System.getProperty("user.dir"));

    public static boolean hasHiddenAncestorDirectory(File file) {
        File relativeFile = relativize(file.getAbsoluteFile());
        relativeFile = relativeFile.getParentFile().toPath().toAbsolutePath().normalize().toFile();
        while (relativeFile != null) {
            if (relativeFile.isHidden()) {
                return true;
            }

            relativeFile = relativeFile.getParentFile();
        }
        return false;
    }

    public static File relativize(File file) {
        Path absolutePath = file.toPath().toAbsolutePath();
        if (absolutePath.startsWith(CWD)) {
            return CWD.relativize(absolutePath).toFile();
        }
        return absolutePath.toFile();
    }
}
