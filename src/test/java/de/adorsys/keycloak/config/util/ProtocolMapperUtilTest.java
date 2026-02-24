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

package de.adorsys.keycloak.config.util;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class ProtocolMapperUtilTest {

    private static ProtocolMapperRepresentation pm(String name) {
        ProtocolMapperRepresentation pm = new ProtocolMapperRepresentation();
        pm.setName(name);
        return pm;
    }

    @Test
    void estimateProtocolMappersToRemove_shouldReturnEmptyListWhenExistingIsNullOrEmpty() {
        assertThat(ProtocolMapperUtil.estimateProtocolMappersToRemove(List.of(pm("a")), null), is(empty()));
        assertThat(ProtocolMapperUtil.estimateProtocolMappersToRemove(List.of(pm("a")), List.of()), is(empty()));
    }

    @Test
    void estimateProtocolMappersToRemove_shouldReturnMappersNotPresentInImport() {
        List<ProtocolMapperRepresentation> existing = List.of(pm("a"), pm("b"));
        List<ProtocolMapperRepresentation> imported = List.of(pm("a"));

        var toRemove = ProtocolMapperUtil.estimateProtocolMappersToRemove(imported, existing);
        assertThat(toRemove, contains(existing.get(1)));
    }

    @Test
    void estimateProtocolMappersToAdd_shouldReturnImportedWhenExistingIsNull() {
        List<ProtocolMapperRepresentation> imported = List.of(pm("a"));

        var toAdd = ProtocolMapperUtil.estimateProtocolMappersToAdd(imported, null);
        assertThat(toAdd, is(imported));
    }

    @Test
    void estimateProtocolMappersToAdd_shouldAddOnlyNotExisting() {
        List<ProtocolMapperRepresentation> existing = List.of(pm("a"));
        ProtocolMapperRepresentation b = pm("b");
        List<ProtocolMapperRepresentation> imported = List.of(pm("a"), b);

        var toAdd = ProtocolMapperUtil.estimateProtocolMappersToAdd(imported, existing);
        assertThat(toAdd, contains(b));
    }

    @Test
    void estimateProtocolMappersToUpdate_shouldReturnEmptyWhenExistingIsNull() {
        var toUpdate = ProtocolMapperUtil.estimateProtocolMappersToUpdate(List.of(pm("a")), null);
        assertThat(toUpdate, is(empty()));
    }

    @Test
    void estimateProtocolMappersToUpdate_shouldReturnPatchedMappersWhenExistingHasSameName() {
        ProtocolMapperRepresentation existing = pm("a");
        existing.setProtocol("openid-connect");

        ProtocolMapperRepresentation imported = pm("a");
        imported.setProtocol("openid-connect");

        var toUpdate = ProtocolMapperUtil.estimateProtocolMappersToUpdate(List.of(imported), List.of(existing));
        assertThat(toUpdate.size(), is(1));
        assertThat(toUpdate.get(0).getName(), is("a"));
    }

    @Test
    void areProtocolMappersEqual_shouldHandleNullAndSizeMismatch() {
        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(null, null), is(true));
        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(List.of(), null), is(true));
        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(List.of(pm("a")), null), is(false));
        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(List.of(pm("a")), List.of(pm("a"), pm("b"))), is(false));
    }

    @Test
    void areProtocolMappersEqual_shouldReturnFalseWhenMapperIsMissing() {
        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(List.of(pm("a")), List.of(pm("b"))), is(false));
    }

    @Test
    void areProtocolMappersEqual_shouldComparePatchedAndExistingIgnoringId() {
        ProtocolMapperRepresentation existing = pm("a");
        existing.setConfig(Map.of("k", "v"));

        ProtocolMapperRepresentation importedSame = pm("a");
        importedSame.setConfig(Map.of("k", "v"));

        ProtocolMapperRepresentation importedDifferent = pm("a");
        importedDifferent.setConfig(Map.of("k", "other"));

        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(List.of(importedSame), List.of(existing)), is(true));
        assertThat(ProtocolMapperUtil.areProtocolMappersEqual(List.of(importedDifferent), List.of(existing)), is(false));
    }
}
