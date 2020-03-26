/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class ResourceLoader {

    public static File loadResource(String path) {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Cannot find file at '" + path + "'");
        }

        String filename = resource.getFile();

        return new File(filename);
    }

    public static File loadProjectFile(String path) {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();
        URL resource = classLoader.getResource("");

        if (resource == null) {
            throw new IllegalArgumentException("Cannot find file at '" + path + "'");
        }

        String projectPath = resource.toString() + "../../";
        String filePath = projectPath + path;

        URL fileUrl;
        try {
            fileUrl = new URL(filePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String filename = fileUrl.getFile();
        return new File(filename);
    }

    public static File loadTargetFile(String relativeFilePath) {
        ProtectionDomain protectionDomain = ResourceLoader.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();

        URI uri = getUri(location);
        File file = new File(uri);

        return new File(file, relativeFilePath);
    }

    private static URI getUri(URL location) {
        URI uri;

        try {
            uri = location.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return uri;
    }
}
