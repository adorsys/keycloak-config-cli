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

import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.util.GlobUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Order(2)
@Component
class DirectoryResourceExtractor implements ResourceExtractor {
    private final ImportConfigProperties config;

    private static final Logger logger = LoggerFactory.getLogger(DirectoryResourceExtractor.class);

    public DirectoryResourceExtractor(ImportConfigProperties config) {
        this.config = config;
    }

    public boolean canHandleResource(Resource resource) throws IOException {
        File file = resource.getFile();
        return file.isDirectory() && file.canRead();
    }

    public Collection<File> extract(Resource resource) throws IOException {
        logger.debug("Extracting files from DirectoryResource ...");
        Assert.notNull(resource, "The resource to extract files cannot be null!");

        File directory = resource.getFile();
        Stream<Path> fileStream = this.config.isDeep() ? Files.walk(directory.toPath()) : Files.walk(directory.toPath(), 1);

        fileStream = fileStream.filter(
                GlobUtil.buildMatchPathPredicate(this.config.getInclude(), this.config.getExclude(), Paths.get(directory.getPath()))
        );

        List<File> extracted = fileStream
                .map(Path::toFile)
                .filter(f -> !f.isDirectory())
                .sorted()
                .collect(Collectors.toList());

        return extracted;
    }
}
