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
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Order(3)
@Component
class UrlResourceExtractor implements ResourceExtractor {

    private static final Logger logger = LoggerFactory.getLogger(UrlResourceExtractor.class);

    @Override
    public boolean canHandleResource(Resource resource) {
        return resource instanceof UrlResource;
    }

    @Override
    public Collection<File> extract(Resource resource) throws IOException {
        logger.debug("Extracting files from UrlResource ...");
        Assert.notNull(resource, "The resource to extract files must be not null!");

        URL url = resource.getURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Connection", "close");

        setupBasicAuth(urlConnection, url);

        try (InputStream inputStream = urlConnection.getInputStream()) {
            File tempFile = FileUtils.createTempFile(resource.getFilename(), inputStream);
            Assert.notNull(tempFile, "The temp file to extract resource must be not null!");

            return FileUtils.extractFile(tempFile);
        } finally {
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).disconnect();
            }
        }
    }

    private void setupBasicAuth(URLConnection urlConnection, URL url) {
        String userInfo = url.getUserInfo();
        if (userInfo != null) {
            String basicAuthHeader = "Basic " + Base64Utils.encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));
            urlConnection.setRequestProperty("Authorization", basicAuthHeader);
        }
    }
}
