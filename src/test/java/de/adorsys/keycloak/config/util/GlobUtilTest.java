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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class GlobUtilTest {
    @Test
    void shouldThrowOnNew() {
        assertThrows(IllegalStateException.class, GlobUtil::new);
    }

    @Test
    void pathPredicateIncludeBaseDirectory() {
        Predicate<? super Path> pathPredicate = GlobUtil.buildMatchPathPredicate(Arrays.asList("*.json", "*.yml"), null, Paths.get("/dummy/test"));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.txt")));

        assertFalse(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.txt")));
    }

    @Test
    void pathPredicatedIncludeSubDirectory() {
        Predicate<? super Path> pathPredicate = GlobUtil.buildMatchPathPredicate(Arrays.asList("**.json", "**.yml"), null, Paths.get("/dummy/test"));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.txt")));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.txt")));
    }

    @Test
    void pathPredicateExcludeBaseDirectory() {
        Predicate<? super Path> pathPredicate = GlobUtil.buildMatchPathPredicate(null, Arrays.asList("*.txt"), Paths.get("/dummy/test"));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.txt")));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.yml")));
        assertTrue(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.txt")));
    }

    @Test
    void pathPredicateExcludeSubDirectory() {
        Predicate<? super Path> pathPredicate = GlobUtil.buildMatchPathPredicate(null, Arrays.asList("**.txt"), Paths.get("/dummy/test"));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.txt")));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.txt")));
    }

    @Test
    void pathPredicateIncludeExcludeBaseDirectory() {
        Predicate<? super Path> pathPredicate = GlobUtil.buildMatchPathPredicate(Arrays.asList("**.yml"), Arrays.asList("test.yml"), Paths.get("/dummy/test"));

        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.txt")));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.txt")));
    }

    @Test
    void pathPredicateIncludeExcludeSubDirectory() {
        Predicate<? super Path> pathPredicate = GlobUtil.buildMatchPathPredicate(Arrays.asList("**.yml"), Arrays.asList("test.yml"), Paths.get("/dummy/test"));

        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/test.txt")));

        assertTrue(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.yml")));
        assertFalse(pathPredicate.test(Paths.get("/dummy/test/sub/directory/test.txt")));
    }
}
