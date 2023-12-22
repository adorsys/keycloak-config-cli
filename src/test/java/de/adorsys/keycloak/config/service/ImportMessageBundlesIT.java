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

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.util.LocalizationUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmLocalizationResource;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

class ImportMessageBundlesIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithMessageBundles";

    ImportMessageBundlesIT() {
        this.resourcePath = "import-files/message-bundles";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithMessageBundles() throws IOException {
        doImport("00_create_realm_with_message_bundles.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.isInternationalizationEnabled(), is(true));
        assertThat(createdRealm.getSupportedLocales(), contains("de", "en"));

        Map<String, String> deMessageBundle = loadMessageBundle("de");
        assertThat("de message bundle size not correct", deMessageBundle.size(), is(2));
        assertThat("hello not set for de", deMessageBundle.get("hello"), is("Hallo"));
        assertThat("world not set for de", deMessageBundle.get("world"), is("Welt!"));
        Map<String, String> enMessageBundle = loadMessageBundle("en");
        assertThat("en message bundle size not correct", enMessageBundle.size(), is(2));
        assertThat("hello not set for en", enMessageBundle.get("hello"), is("Hello"));
        assertThat("world not set for en", enMessageBundle.get("world"), is("World!"));
    }

    @Test
    @Order(1)
    void shouldUpdateRealmWithNewMessageBundleKeys() throws IOException {
        doImport("01_update_realm_with_new_message_bundles_keys.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.isInternationalizationEnabled(), is(true));
        assertThat(createdRealm.getSupportedLocales(), contains("de", "en"));

        Map<String, String> deMessageBundle = loadMessageBundle("de");
        assertThat("de message bundle size not correct", deMessageBundle.size(), is(3));
        assertThat("hello not set for de", deMessageBundle.get("hello"), is("Hallo"));
        assertThat("world not set for de", deMessageBundle.get("world"), is("Welt!"));
        assertThat("new not set for de", deMessageBundle.get("new"), is("neu"));
        Map<String, String> enMessageBundle = loadMessageBundle("en");
        assertThat("en message bundle size not correct", enMessageBundle.size(), is(3));
        assertThat("hello not set for en", enMessageBundle.get("hello"), is("Hello"));
        assertThat("world not set for en", enMessageBundle.get("world"), is("World!"));
        assertThat("new not set for en", enMessageBundle.get("new"), is("new"));
    }

    @Test
    @Order(2)
    void shouldUpdateRealmWithNewLocale() throws IOException {
        doImport("02_update_realm_with_new_locale.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.isInternationalizationEnabled(), is(true));
        assertThat(createdRealm.getSupportedLocales(), contains("de", "en", "es"));

        Map<String, String> deMessageBundle = loadMessageBundle("de");
        assertThat("de message bundle size not correct", deMessageBundle.size(), is(3));
        assertThat("hello not set for de", deMessageBundle.get("hello"), is("Hallo"));
        assertThat("world not set for de", deMessageBundle.get("world"), is("Welt!"));
        assertThat("new not set for de", deMessageBundle.get("new"), is("neu"));
        Map<String, String> enMessageBundle = loadMessageBundle("en");
        assertThat("en message bundle size not correct", enMessageBundle.size(), is(3));
        assertThat("hello not set for en", enMessageBundle.get("hello"), is("Hello"));
        assertThat("world not set for en", enMessageBundle.get("world"), is("World!"));
        assertThat("new not set for en", enMessageBundle.get("new"), is("new"));
        Map<String, String> esMessageBundle = loadMessageBundle("es");
        assertThat("es message bundle size not correct", esMessageBundle.size(), is(3));
        assertThat("hello not set for es", esMessageBundle.get("hello"), is("Hola"));
        assertThat("world not set for es", esMessageBundle.get("world"), is("Mundo!"));
        assertThat("new not set for es", esMessageBundle.get("new"), is("nuevo"));
    }

    @Test
    @Order(3)
    void shouldUpdateRealmWithRemovedLocale() throws IOException {
        doImport("03_update_realm_with_removed_locale.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.isInternationalizationEnabled(), is(true));
        assertThat(createdRealm.getSupportedLocales(), contains("de", "en"));
        assertThat(createdRealm.getSupportedLocales().contains("es"), is(false));

        Map<String, String> deMessageBundle = loadMessageBundle("de");
        assertThat("de message bundle size not correct", deMessageBundle.size(), is(3));
        assertThat("hello not set for de", deMessageBundle.get("hello"), is("Hallo"));
        assertThat("world not set for de", deMessageBundle.get("world"), is("Welt!"));
        assertThat("new not set for de", deMessageBundle.get("new"), is("neu"));
        Map<String, String> enMessageBundle = loadMessageBundle("en");
        assertThat("en message bundle size not correct", enMessageBundle.size(), is(3));
        assertThat("hello not set for en", enMessageBundle.get("hello"), is("Hello"));
        assertThat("world not set for en", enMessageBundle.get("world"), is("World!"));
        assertThat("new not set for en", enMessageBundle.get("new"), is("new"));
        Map<String, String> esMessageBundle = loadMessageBundle("es");
        assertThat("es message bundle not empty", esMessageBundle, anEmptyMap());
    }

    @Test
    @Order(4)
    void shouldUpdateRealmWithRemovedLocaleKeyForEn() throws IOException {
        doImport("04_update_realm_with_removed_locale_key_for_en.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.isInternationalizationEnabled(), is(true));
        assertThat(createdRealm.getSupportedLocales(), contains("de", "en"));

        Map<String, String> deMessageBundle = loadMessageBundle("de");
        assertThat("de message bundle size not correct", deMessageBundle.size(), is(3));
        assertThat("hello set for de", deMessageBundle.get("hello"), is("Hallo"));
        assertThat("world not set for de", deMessageBundle.get("world"), is("Welt!"));
        assertThat("new set for de", deMessageBundle.get("new"), is("neu"));
        Map<String, String> enMessageBundle = loadMessageBundle("en");
        assertThat("en message bundle size not correct", enMessageBundle.size(), is(2));
        assertThat("hello not set for en", enMessageBundle.get("hello"), is("Hello"));
        assertThat("world not set for en", enMessageBundle.get("world"), is("World!"));
        assertThat("new set for en, should be null", enMessageBundle.get("new"), nullValue());
    }

    @Test
    @Order(5)
    void shouldUpdateRealmWithRemovedAndNewKeys() throws IOException {
        doImport("05_update_realm_with_removed_and_new_keys.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.isInternationalizationEnabled(), is(true));
        assertThat(createdRealm.getSupportedLocales(), contains("de", "en"));

        Map<String, String> deMessageBundle = loadMessageBundle("de");
        assertThat("de message bundle size not correct", deMessageBundle.size(), is(2));
        assertThat("hello not set for de", deMessageBundle.get("hello"), is("Hallo"));
        assertThat("world not set for de", deMessageBundle.get("world"), is("Welt!"));
        assertThat("new set for de, should be null", deMessageBundle.get("new"), nullValue());
        Map<String, String> enMessageBundle = loadMessageBundle("en");
        assertThat("en message bundle size not correct", enMessageBundle.size(), is(3));
        assertThat("hello not set for en", enMessageBundle.get("hello"), is("Hello"));
        assertThat("world not set for en", enMessageBundle.get("world"), is("World!"));
        assertThat("anotherNew not set for en", enMessageBundle.get("anotherNew"), is("another new text"));
    }

    private Map<String, String> loadMessageBundle(String locale) {
        RealmLocalizationResource realmLocalizationResource = keycloakProvider.getInstance()
                .realm(REALM_NAME)
                .localization();

        return LocalizationUtil.getRealmLocalizationTexts(realmLocalizationResource, locale);
    }

}
