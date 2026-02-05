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

package io.github.doriangrelu.keycloak.config.properties;

import io.github.doriangrelu.keycloak.config.extensions.GithubActionsExtension;
import io.github.doriangrelu.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import io.github.doriangrelu.keycloak.config.properties.ImportConfigProperties.ImportBehaviorsProperties.ChecksumChangedOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

// From: https://tuhrig.de/testing-configurationproperties-in-spring-boot/
@ExtendWith(SpringExtension.class)
@ExtendWith(GithubActionsExtension.class)
@SpringBootTest(classes = {ImportConfigPropertiesTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "spring.main.log-startup-info=false",

        "import.parallel=true",
        "import.validate=false",
        "import.files.locations=other",
        "import.files.include-hidden-files=true",
        "import.files.excludes=exclude1,exclude2",
        "import.var-substitution.enabled=true",
        "import.var-substitution.nested=false",
        "import.var-substitution.undefined-is-error=false",
        "import.var-substitution.prefix=${",
        "import.var-substitution.suffix=}",
        "import.cache.enabled=false",
        "import.cache.key=custom",
        "import.remote-state.enabled=false",
        "import.remote-state.encryption-key=password",
        "import.remote-state.encryption-salt=0123456789ABCDEFabcdef",
        "import.managed.authentication-flow=no-delete",
        "import.managed.group=no-delete",
        "import.managed.required-action=no-delete",
        "import.managed.client-scope=no-delete",
        "import.managed.scope-mapping=no-delete",
        "import.managed.client-scope-mapping=no-delete",
        "import.managed.component=no-delete",
        "import.managed.sub-component=no-delete",
        "import.managed.identity-provider=no-delete",
        "import.managed.identity-provider-mapper=no-delete",
        "import.managed.role=no-delete",
        "import.managed.client=no-delete",
        "import.managed.client-authorization-resources=no-delete",
        "import.behaviors.sync-user-federation=true",
        "import.behaviors.remove-default-role-from-user=true",
        "import.behaviors.skip-attributes-for-federated-user=true",
        "import.behaviors.checksum-with-cache-key=true",
        "import.behaviors.checksum-changed=fail"
})
class ImportConfigPropertiesTest {

    @Autowired
    private ImportConfigProperties properties;

    @Test
    @SuppressWarnings({"java:S2699", "java:S5961"})
    void shouldPopulateConfigurationProperties() {
        assertThat(properties.isValidate(), is(false));
        assertThat(properties.isParallel(), is(true));
        assertThat(properties.getFiles().getLocations(), contains("other"));
        assertThat(properties.getFiles().getExcludes(), contains("exclude1", "exclude2"));
        assertThat(properties.getFiles().isIncludeHiddenFiles(), is(true));
        assertThat(properties.getVarSubstitution().isEnabled(), is(true));
        assertThat(properties.getVarSubstitution().isNested(), is(false));
        assertThat(properties.getVarSubstitution().isUndefinedIsError(), is(false));
        assertThat(properties.getVarSubstitution().getPrefix(), is("${"));
        assertThat(properties.getVarSubstitution().getSuffix(), is("}"));
        assertThat(properties.getCache().isEnabled(), is(false));
        assertThat(properties.getCache().getKey(), is("custom"));
        assertThat(properties.getRemoteState().isEnabled(), is(false));
        assertThat(properties.getRemoteState().getEncryptionKey(), is("password"));
        assertThat(properties.getRemoteState().getEncryptionSalt(), is("0123456789ABCDEFabcdef"));
        assertThat(properties.getManaged().getAuthenticationFlow(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getGroup(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getRequiredAction(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClientScope(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getScopeMapping(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClientScopeMapping(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getComponent(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getSubComponent(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getIdentityProvider(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getIdentityProviderMapper(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getRole(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClient(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClientAuthorizationResources(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getBehaviors().isSyncUserFederation(), is(true));
        assertThat(properties.getBehaviors().isRemoveDefaultRoleFromUser(), is(true));
        assertThat(properties.getBehaviors().isSkipAttributesForFederatedUser(), is(true));
        assertThat(properties.getBehaviors().isChecksumWithCacheKey(), is(true));
        assertThat(properties.getBehaviors().getChecksumChanged(), is(ChecksumChangedOption.FAIL));
    }

    @EnableConfigurationProperties(ImportConfigProperties.class)
    public static class TestConfiguration {
        // nothing
    }
}
