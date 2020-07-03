/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

public class KeycloakVersion {

    private static final String KEYCLOAK_VERSION_PROPERTY_NAME = "keycloak.version";

    public static boolean isKeycloak8() {
        String version = getKeycloakVersion();
        return version != null && version.startsWith("8.");
    }

    private static String getKeycloakVersion() {
        return System.getProperty(KEYCLOAK_VERSION_PROPERTY_NAME);
    }

}
