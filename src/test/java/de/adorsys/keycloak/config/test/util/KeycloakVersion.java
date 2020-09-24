/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.test.util;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class KeycloakVersion {
    private static final DefaultArtifactVersion KEYCLOAK_VERSION = new DefaultArtifactVersion(
            System.getProperty("keycloak.version")
    );

    public static DefaultArtifactVersion get() {
        return KEYCLOAK_VERSION;
    }

    public static boolean gt(String version) {
        return KEYCLOAK_VERSION.compareTo(new DefaultArtifactVersion(version)) > 0;
    }

    public static boolean ge(String version) {
        return KEYCLOAK_VERSION.compareTo(new DefaultArtifactVersion(version)) >= 0;
    }

    public static boolean lt(String version) {
        return KEYCLOAK_VERSION.compareTo(new DefaultArtifactVersion(version)) < 0;
    }

    public static boolean le(String version) {
        return KEYCLOAK_VERSION.compareTo(new DefaultArtifactVersion(version)) <= 0;
    }

    public static boolean eq(String version) {
        return KEYCLOAK_VERSION.compareTo(new DefaultArtifactVersion(version)) == 0;
    }

    public static boolean eqPrefix(String version) {
        return KEYCLOAK_VERSION.toString().startsWith(version);
    }
}
