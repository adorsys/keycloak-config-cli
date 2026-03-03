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
import de.adorsys.keycloak.config.repository.ScopeMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;

import java.util.List;
import java.util.Set;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopeMappingImportServiceTest {

    private RealmRepository realmRepository;
    private ScopeMappingRepository scopeMappingRepository;
    private ImportConfigProperties importConfigProperties;
    private ImportConfigProperties.ImportManagedProperties managed;

    private ScopeMappingImportService service;

    @BeforeEach
    void setUp() {
        realmRepository = mock(RealmRepository.class);
        scopeMappingRepository = mock(ScopeMappingRepository.class);
        importConfigProperties = mock(ImportConfigProperties.class);
        managed = mock(ImportConfigProperties.ImportManagedProperties.class);

        when(importConfigProperties.getManaged()).thenReturn(managed);
        when(managed.getScopeMapping()).thenReturn(ImportManagedPropertiesValues.NO_DELETE);
        when(managed.getClientScope()).thenReturn(ImportManagedPropertiesValues.NO_DELETE);

        service = new ScopeMappingImportService(realmRepository, scopeMappingRepository, importConfigProperties);
    }

    private static ScopeMappingRepresentation clientMapping(String client, Set<String> roles) {
        ScopeMappingRepresentation r = new ScopeMappingRepresentation();
        r.setClient(client);
        r.setClientScope(null);
        r.setRoles(roles);
        return r;
    }

    private static ScopeMappingRepresentation clientScopeMapping(String clientScope, Set<String> roles) {
        ScopeMappingRepresentation r = new ScopeMappingRepresentation();
        r.setClient(null);
        r.setClientScope(clientScope);
        r.setRoles(roles);
        return r;
    }

    @Test
    void doImport_shouldReturnWhenScopeMappingsNull() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test");

        service.doImport(realmImport);

        verify(realmRepository, never()).partialExport(eq("test"), any(Boolean.class), any(Boolean.class));
        verify(scopeMappingRepository, never()).addScopeMapping(eq("test"), any());
    }

    @Test
    void doImport_shouldAddNewClientScopeMappingWhenNotExisting() {
        RealmImport realmImport = mock(RealmImport.class);
        when(realmImport.getRealm()).thenReturn("test");

        ScopeMappingRepresentation toImport = clientScopeMapping("scope-a", Set.of("role-a"));
        when(realmImport.getScopeMappings()).thenReturn(List.of(toImport));

        RealmRepresentation existingRealm = mock(RealmRepresentation.class);
        when(existingRealm.getScopeMappings()).thenReturn(List.of());
        when(realmRepository.partialExport("test", true, true)).thenReturn(existingRealm);

        service.doImport(realmImport);

        verify(scopeMappingRepository).addScopeMapping("test", toImport);
    }

    @Test
    void doImport_shouldAddAndRemoveRolesForClientMappingWhenManagedFull() {
        when(managed.getClientScope()).thenReturn(ImportManagedPropertiesValues.FULL);

        RealmImport realmImport = mock(RealmImport.class);
        when(realmImport.getRealm()).thenReturn("test");

        ScopeMappingRepresentation existing = clientMapping("client-a", Set.of("role-existing", "role-to-remove"));
        RealmRepresentation existingRealm = mock(RealmRepresentation.class);
        when(existingRealm.getScopeMappings()).thenReturn(List.of(existing));
        when(realmRepository.partialExport("test", true, true)).thenReturn(existingRealm);

        ScopeMappingRepresentation toImport = clientMapping("client-a", Set.of("role-existing", "role-to-add"));
        when(realmImport.getScopeMappings()).thenReturn(List.of(toImport));

        service.doImport(realmImport);

        verify(scopeMappingRepository).addScopeMappingRolesForClient("test", "client-a", List.of("role-to-add"));
        verify(scopeMappingRepository).removeScopeMappingRolesForClient("test", "client-a", List.of("role-to-remove"));
    }

    @Test
    void doImport_shouldCleanupExistingRolesWhenManagedScopeMappingFull() {
        when(managed.getScopeMapping()).thenReturn(ImportManagedPropertiesValues.FULL);

        RealmImport realmImport = mock(RealmImport.class);
        when(realmImport.getRealm()).thenReturn("test");
        when(realmImport.getScopeMappings()).thenReturn(List.of());

        ScopeMappingRepresentation existing = clientScopeMapping("scope-to-clean", Set.of("role-x"));
        RealmRepresentation existingRealm = mock(RealmRepresentation.class);
        when(existingRealm.getScopeMappings()).thenReturn(List.of(existing));
        when(realmRepository.partialExport("test", true, true)).thenReturn(existingRealm);

        service.doImport(realmImport);

        verify(scopeMappingRepository).removeScopeMappingRolesForClientScope("test", "scope-to-clean", Set.of("role-x"));
    }

    @Test
    void areScopeMappingsEqual_shouldHandleNullAndClientVsClientScope() {
        ScopeMappingRepresentation a = clientMapping("client-a", Set.of());
        ScopeMappingRepresentation b = clientScopeMapping("scope-a", Set.of());

        org.junit.jupiter.api.Assertions.assertFalse(service.areScopeMappingsEqual(null, a));
        org.junit.jupiter.api.Assertions.assertFalse(service.areScopeMappingsEqual(a, null));
        org.junit.jupiter.api.Assertions.assertFalse(service.areScopeMappingsEqual(a, b));
        org.junit.jupiter.api.Assertions.assertTrue(service.areScopeMappingsEqual(a, clientMapping("client-a", Set.of("x"))));
        org.junit.jupiter.api.Assertions.assertTrue(service.areScopeMappingsEqual(b, clientScopeMapping("scope-a", Set.of("y"))));
    }
}
