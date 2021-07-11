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

package de.adorsys.keycloak.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowImportDiffService {
    private static final Logger logger = LoggerFactory.getLogger(ShowImportDiffService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;
    private final StateService stateService;

    @Autowired
    public ShowImportDiffService(RealmRepository realmRepository, ClientRepository clientRepository, StateService stateService) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
        this.stateService = stateService;
    }

    public void printDifference(RealmImport realmImport) {
        stateService.loadState(realmImport);

        RealmRepresentation existingRepresentation = realmRepository.partialExport(realmImport.getRealm(), true, true);

        existingRepresentation.setClients(fetchClients(realmImport));

        RealmRepresentation importRepresentation = CloneUtil.deepPatch(
                existingRepresentation,
                realmImport,
                "users"
        );

        List<String> importConfig;
        List<String> existingConfig;

        try {
            importConfig = Arrays.asList(objectMapper.writeValueAsString(importRepresentation).split("\n"));
            existingConfig = Arrays.asList(objectMapper.writeValueAsString(existingRepresentation).split("\n"));
        } catch (JsonProcessingException exception) {
            throw new ImportProcessingException(exception);
        }

        Patch<String> diff = DiffUtils.diff(existingConfig, importConfig);
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                "current-" + realmImport.getRealm(), "import-" + realmImport.getRealm(),
                importConfig, diff, 4
        );

        logger.warn("Dump configuration diff is in experimental state!");
        if (!unifiedDiff.isEmpty()) {
            logger.info("Dump detected configuration diff:\n" + String.join("\n", unifiedDiff));
        } else {
            logger.info("Dump detected configuration diff: - no difference -");
        }
    }

    private List<ClientRepresentation> fetchClients(RealmImport realmImport) {
        final List<String> stateClients = stateService.getClients();

        return clientRepository.getAll(realmImport.getRealm())
                .stream()
                .filter(client -> stateClients.contains(client.getClientId()))
                .collect(Collectors.toList());
    }
}
