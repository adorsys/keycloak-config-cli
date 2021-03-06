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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Order(2)
@Component
class DirectoryResourceExtractor implements ResourceExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryResourceExtractor.class);

    public boolean canHandleResource(Resource resource) throws IOException {
        File file = resource.getFile();
        return file.isDirectory() && file.canRead();
    }

    public Collection<File> extract(Resource resource) throws IOException {
        logger.debug("Extracting files from DirectoryResource ...");
        Assert.notNull(resource, "The resource to extract files cannot be null!");

        File file = resource.getFile();
        File[] files = file.listFiles();

        return Optional.ofNullable(files).map(f -> Arrays.stream(f).filter(File::isFile).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
