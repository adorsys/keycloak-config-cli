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

package de.adorsys.keycloak.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "import")
@ConstructorBinding
@Validated
public class ImportConfigProperties {
    public static final String REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY = "de.adorsys.keycloak.config.import-checksum-{0}";
    public static final String REALM_STATE_ATTRIBUTE_PREFIX_KEY = "de.adorsys.keycloak.config.state-{0}-{1}";

    @NotBlank
    private final String path;

    @NotNull
    private final boolean varSubstitution;

    @NotNull
    private final boolean force;

    @NotBlank
    private final String cacheKey;

    @NotNull
    private final boolean state;

    @NotNull
    private final ImportFileType fileType;

    @NotNull
    private final boolean parallel;

    @Valid
    private final ImportManagedProperties managed;

    public ImportConfigProperties(String path, boolean varSubstitution, boolean force, String cacheKey, boolean state, ImportFileType fileType, boolean parallel, ImportManagedProperties managed) {
        this.path = path;
        this.varSubstitution = varSubstitution;
        this.force = force;
        this.cacheKey = cacheKey;
        this.state = state;
        this.fileType = fileType;
        this.parallel = parallel;
        this.managed = managed;
    }

    public String getPath() {
        return path;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isVarSubstitution() {
        return varSubstitution;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public ImportManagedProperties getManaged() {
        return managed;
    }

    public boolean isState() {
        return state;
    }

    public ImportFileType getFileType() {
        return fileType;
    }

    public boolean isParallel() {
        return parallel;
    }

    public enum ImportFileType {
        AUTO,
        JSON,
        YAML
    }

    @SuppressWarnings("unused")
    public static class ImportManagedProperties {
        @NotNull
        private final ImportManagedPropertiesValues requiredAction;

        @NotNull
        private final ImportManagedPropertiesValues group;

        @NotNull
        private final ImportManagedPropertiesValues clientScope;

        @NotNull
        private final ImportManagedPropertiesValues scopeMapping;

        @NotNull
        private final ImportManagedPropertiesValues component;

        @NotNull
        private final ImportManagedPropertiesValues subComponent;

        @NotNull
        private final ImportManagedPropertiesValues authenticationFlow;

        @NotNull
        private final ImportManagedPropertiesValues identityProvider;

        @NotNull
        private final ImportManagedPropertiesValues identityProviderMapper;

        @NotNull
        private final ImportManagedPropertiesValues role;

        public ImportManagedProperties(
                ImportManagedPropertiesValues requiredAction, ImportManagedPropertiesValues group,
                ImportManagedPropertiesValues clientScope, ImportManagedPropertiesValues scopeMapping,
                ImportManagedPropertiesValues component, ImportManagedPropertiesValues subComponent,
                ImportManagedPropertiesValues authenticationFlow, ImportManagedPropertiesValues identityProvider,
                ImportManagedPropertiesValues identityProviderMapper, ImportManagedPropertiesValues role) {
            this.requiredAction = requiredAction;
            this.group = group;
            this.clientScope = clientScope;
            this.scopeMapping = scopeMapping;
            this.component = component;
            this.subComponent = subComponent;
            this.authenticationFlow = authenticationFlow;
            this.identityProvider = identityProvider;
            this.identityProviderMapper = identityProviderMapper;
            this.role = role;
        }

        public ImportManagedPropertiesValues getRequiredAction() {
            return requiredAction;
        }

        public ImportManagedPropertiesValues getClientScope() {
            return clientScope;
        }

        public ImportManagedPropertiesValues getScopeMapping() {
            return scopeMapping;
        }

        public ImportManagedPropertiesValues getComponent() {
            return component;
        }

        public ImportManagedPropertiesValues getSubComponent() {
            return subComponent;
        }

        public ImportManagedPropertiesValues getAuthenticationFlow() {
            return authenticationFlow;
        }

        public ImportManagedPropertiesValues getGroup() {
            return group;
        }

        public ImportManagedPropertiesValues getIdentityProvider() {
            return identityProvider;
        }

        public ImportManagedPropertiesValues getIdentityProviderMapper() {
            return identityProviderMapper;
        }

        public ImportManagedPropertiesValues getRole() {
            return role;
        }

        public enum ImportManagedPropertiesValues {
            FULL,
            NO_DELETE
        }
    }
}
