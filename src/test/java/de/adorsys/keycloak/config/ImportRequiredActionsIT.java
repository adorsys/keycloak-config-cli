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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportRequiredActionsIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithRequiredActions";

    ImportRequiredActionsIT() {
        this.resourcePath = "import-files/required-actions";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithRequiredActions() {
        doImport("00_create_realm_with_required-action.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getRequiredActions(), hasSize(1));

        RequiredActionProviderRepresentation createdRequiredAction = getRequiredAction(createdRealm, "MY_CONFIGURE_TOTP");
        assertThat(createdRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(createdRequiredAction.getName(), is("My Configure OTP"));
        assertThat(createdRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(createdRequiredAction.isEnabled(), is(true));
        assertThat(createdRequiredAction.isDefaultAction(), is(false));
        assertThat(createdRequiredAction.getPriority(), is(0));
    }

    @Test
    @Order(1)
    void shouldFailIfAddingInvalidRequiredActionName() {
        RealmImport foundImport = getImport("01_update_realm__try_adding_invalid_required-action.json");

        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot import Required-Action 'my_terms_and_conditions': alias and provider-id have to be equal"));
    }

    @Test
    @Order(2)
    void shouldAddDefaultRequiredAction() {
        doImport("02_update_realm__add_default_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(2));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(0));

        RequiredActionProviderRepresentation addedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(addedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(addedRequiredAction.getName(), is("My Terms and Conditions"));
        assertThat(addedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(addedRequiredAction.isEnabled(), is(false));
        assertThat(addedRequiredAction.isDefaultAction(), is(false));
        assertThat(addedRequiredAction.getPriority(), is(1));
    }

    @Test
    @Order(3)
    void shouldChangeRequiredActionName() {
        doImport("03_update_realm__change_name_of_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(2));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(0));

        RequiredActionProviderRepresentation changedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(changedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.getName(), is("Changed: My Terms and Conditions"));
        assertThat(changedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.isEnabled(), is(false));
        assertThat(changedRequiredAction.isDefaultAction(), is(false));
        assertThat(changedRequiredAction.getPriority(), is(1));
    }

    @Test
    @Order(4)
    void shouldEnableRequiredAction() {
        doImport("04_update_realm__enable_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(2));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(0));

        RequiredActionProviderRepresentation changedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(changedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.getName(), is("Changed: My Terms and Conditions"));
        assertThat(changedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.isEnabled(), is(true));
        assertThat(changedRequiredAction.isDefaultAction(), is(false));
        assertThat(changedRequiredAction.getPriority(), is(1));
    }

    @Test
    @Order(5)
    void shouldChangePriorities() {
        doImport("05_update_realm__change_priorities_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(2));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(1));

        RequiredActionProviderRepresentation changedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(changedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.getName(), is("Changed: My Terms and Conditions"));
        assertThat(changedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.isEnabled(), is(true));
        assertThat(changedRequiredAction.isDefaultAction(), is(false));
        assertThat(changedRequiredAction.getPriority(), is(0));
    }

    @Test
    @Order(6)
    void shouldAddRequiredAction() {
        doImport("06_update_realm__delete_and_add_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(2));

        RequiredActionProviderRepresentation newRequiredAction1 = getRequiredAction(updatedRealm, "moped_required_action");
        assertThat(newRequiredAction1.getAlias(), is("moped_required_action"));
        assertThat(newRequiredAction1.getName(), is("Moped"));
        assertThat(newRequiredAction1.getProviderId(), is("moped_required_action"));
        assertThat(newRequiredAction1.isEnabled(), is(false));
        assertThat(newRequiredAction1.isDefaultAction(), is(false));
        assertThat(newRequiredAction1.getPriority(), is(48));
        assertThat(newRequiredAction1.getConfig(), is(anEmptyMap()));

        RequiredActionProviderRepresentation newRequiredAction2 = getRequiredAction(updatedRealm, "other-moped_required_action");
        assertThat(newRequiredAction2.getAlias(), is("other-moped_required_action"));
        assertThat(newRequiredAction2.getName(), is("Moped"));
        assertThat(newRequiredAction2.getProviderId(), is("other-moped_required_action"));
        assertThat(newRequiredAction2.isEnabled(), is(false));
        assertThat(newRequiredAction2.isDefaultAction(), is(false));
        assertThat(newRequiredAction2.getPriority(), is(42));
        assertThat(newRequiredAction2.getConfig(), is(aMapWithSize(1)));
        assertThat(newRequiredAction2.getConfig().get("hello"), is("world"));
    }

    @Test
    @Order(7)
    void shouldSkipRequiredAction() {
        doImport("07_update_realm__skip_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(2));

        RequiredActionProviderRepresentation newRequiredAction1 = getRequiredAction(updatedRealm, "moped_required_action");
        assertThat(newRequiredAction1.getAlias(), is("moped_required_action"));
        assertThat(newRequiredAction1.getName(), is("Moped"));
        assertThat(newRequiredAction1.getProviderId(), is("moped_required_action"));
        assertThat(newRequiredAction1.isEnabled(), is(false));
        assertThat(newRequiredAction1.isDefaultAction(), is(false));
        assertThat(newRequiredAction1.getPriority(), is(48));
        assertThat(newRequiredAction1.getConfig(), is(anEmptyMap()));

        RequiredActionProviderRepresentation newRequiredAction2 = getRequiredAction(updatedRealm, "other-moped_required_action");
        assertThat(newRequiredAction2.getAlias(), is("other-moped_required_action"));
        assertThat(newRequiredAction2.getName(), is("Moped"));
        assertThat(newRequiredAction2.getProviderId(), is("other-moped_required_action"));
        assertThat(newRequiredAction2.isEnabled(), is(false));
        assertThat(newRequiredAction2.isDefaultAction(), is(false));
        assertThat(newRequiredAction2.getPriority(), is(42));
        assertThat(newRequiredAction2.getConfig(), is(aMapWithSize(1)));
        assertThat(newRequiredAction2.getConfig().get("hello"), is("world"));
    }

    @Test
    @Order(98)
    void shouldDeleteRequiredAction() {
        doImport("98_update_realm__delete_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(1));

        RequiredActionProviderRepresentation newRequiredAction2 = getRequiredAction(updatedRealm, "other-moped_required_action");
        assertThat(newRequiredAction2.getAlias(), is("other-moped_required_action"));
        assertThat(newRequiredAction2.getName(), is("Moped"));
        assertThat(newRequiredAction2.getProviderId(), is("other-moped_required_action"));
        assertThat(newRequiredAction2.isEnabled(), is(false));
        assertThat(newRequiredAction2.isDefaultAction(), is(false));
        assertThat(newRequiredAction2.getPriority(), is(42));
        assertThat(newRequiredAction2.getConfig(), is(aMapWithSize(1)));
        assertThat(newRequiredAction2.getConfig().get("hello"), is("world2"));
    }

    @Test
    @Order(99)
    void shouldDeleteAllRequiredAction() {
        doImport("99_update_realm__delete_all_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getRequiredActions(), hasSize(0));
    }

    private RequiredActionProviderRepresentation getRequiredAction(RealmRepresentation realm, String requiredActionAlias) {
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = realm.getRequiredActions()
                .stream()
                .filter(r -> r.getAlias().equals(requiredActionAlias))
                .findFirst();

        assertThat("Cannot find required-action: '" + requiredActionAlias + "'", maybeRequiredAction.isPresent(), is(true));

        return maybeRequiredAction.orElse(null);
    }
}
