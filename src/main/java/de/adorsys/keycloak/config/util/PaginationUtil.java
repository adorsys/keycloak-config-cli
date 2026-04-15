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

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PaginationUtil {
    private static final int DEFAULT_PAGE_SIZE = 20;

    public static <T> Stream<T> findAll(BiFunction<Integer, Integer, List<T>> getPage) {
        return findAll(DEFAULT_PAGE_SIZE, getPage);
    }

    public static <T> Stream<T> findAll(int pageSize, BiFunction<Integer, Integer, List<T>> getPage) {
        return IntStream.iterate(0, i -> i + 1)
                .mapToObj(i -> getPage.apply(i * pageSize, pageSize))
                .takeWhile(r -> !r.isEmpty())
                .flatMap(List::stream);
    }
}
