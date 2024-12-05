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

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.LocalizationUtil;
import org.keycloak.admin.client.resource.RealmLocalizationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates and updates message bundles in your realm
 */
@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class MessageBundleImportService {
    private static final Logger logger = LoggerFactory.getLogger(MessageBundleImportService.class);

    private final RealmRepository realmRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;

    @Autowired
    public MessageBundleImportService(RealmRepository realmRepository, ImportConfigProperties importConfigProperties,
                                      StateService stateService) {
        this.realmRepository = realmRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        Map<String, Map<String, String>> messageBundles = realmImport.getMessageBundles();
        if (messageBundles == null) return;

        String realmName = realmImport.getRealm();
        RealmLocalizationResource localizationResource = realmRepository.getResource(realmName).localization();
        Set<Map.Entry<String, Map<String, String>>> locales = messageBundles.entrySet();

        if (importConfigProperties.getManaged().getMessageBundles() == ImportConfigProperties
                .ImportManagedProperties.ImportManagedPropertiesValues.FULL) {
            deleteMessageBundlesMissingOnImport(realmName, realmImport.getMessageBundles());
        }

        for (Map.Entry<String, Map<String, String>> localeEntry : locales) {
            String locale = localeEntry.getKey();
            Map<String, String> newMessageBundles = localeEntry.getValue();
            Map<String, String> oldMessageBundles = LocalizationUtil
                    .getRealmLocalizationTexts(localizationResource, locale);

            localizationResource.createOrUpdateRealmLocalizationTexts(locale, newMessageBundles);

            // clean up
            if (oldMessageBundles != null
                    && importConfigProperties.getManaged().getMessageBundles() == ImportConfigProperties
                            .ImportManagedProperties.ImportManagedPropertiesValues.FULL) {
                for (String oldMessageBundleKey : oldMessageBundles.keySet()) {
                    if (!newMessageBundles.containsKey(oldMessageBundleKey)) {
                        localizationResource.deleteRealmLocalizationText(locale, oldMessageBundleKey);
                        logger.debug("Delete message bundle localization text with key '{}' for locale '{}' in realm '{}'",
                                oldMessageBundleKey, locale, realmName);
                    }
                }
            }
        }
    }

    private void deleteMessageBundlesMissingOnImport(
            String realmName,
            Map<String, Map<String, String>> importedMessageBundles) {
        if (importConfigProperties.getRemoteState().isEnabled()) {
            // unknown message bundles are ignored always
            List<String> messageBundlesInState = stateService.getMessageBundles();

            Set<String> importMessageBundles = importedMessageBundles.keySet();

            for (String messageBundle : messageBundlesInState) {
                if (importMessageBundles.contains(messageBundle)) continue;

                logger.debug("Delete message bundle '{}' in realm '{}'", messageBundle, realmName);
                realmRepository.getResource(realmName).localization().deleteRealmLocalizationTexts(messageBundle);
            }
        }
    }


}
