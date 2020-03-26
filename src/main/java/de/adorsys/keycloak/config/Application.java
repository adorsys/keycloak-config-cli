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

package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Application implements CommandLineRunner {
    private final KeycloakImportProvider keycloakImportProvider;
    private final RealmImportService realmImportService;

    @Autowired
    public Application(
            KeycloakImportProvider keycloakImportProvider,
            RealmImportService realmImportService
    ) {
        this.keycloakImportProvider = keycloakImportProvider;
        this.realmImportService = realmImportService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        KeycloakImport keycloakImport = keycloakImportProvider.get();

        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        for (Map.Entry<String, RealmImport> realmImport : realmImports.entrySet()) {
            realmImportService.doImport(realmImport.getValue());
        }
    }
}
