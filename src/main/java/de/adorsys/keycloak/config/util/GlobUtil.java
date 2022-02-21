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

package de.adorsys.keycloak.config.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;

public class GlobUtil {
    GlobUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static Path relativize(Path baseDirectory, Path path) {
        return baseDirectory != null ? baseDirectory.relativize(path) : path;
    }

    public static boolean match(Collection<String> includeGlobs, Collection<String> excludeGlobs, Path baseDirectory, Path path) {
        Boolean excluded = null;
        if (excludeGlobs != null && excludeGlobs.size() > 0) {
            excluded = excludeGlobs.stream().anyMatch(glob -> FileSystems.getDefault().getPathMatcher("glob:"
                    + glob).matches(relativize(baseDirectory, path)));
        }

        if (excluded != null && excluded) {
            return false;
        }

        if (includeGlobs != null && includeGlobs.size() > 0) {
            return includeGlobs.stream().anyMatch(glob -> FileSystems.getDefault().getPathMatcher("glob:"
                    + glob).matches(relativize(baseDirectory, path)));
        }

        return true;
    }

    public static Predicate<? super Path> buildMatchPathPredicate(Collection<String> includeGlobs,
                                                                  Collection<String> excludeGlobs,
                                                                  Path baseDirectory) {
        return (path) -> match(includeGlobs, excludeGlobs, baseDirectory, path);
    }
}
