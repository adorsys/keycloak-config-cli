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

package de.adorsys.keycloak.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@ConfigurationProperties(prefix = "import")
@ConstructorBinding
@Validated
@SuppressWarnings({"java:S107"})
public class ImportConfigProperties {

    public static final String REALM_STATE_ATTRIBUTE_COMMON_PREFIX = "de.adorsys.keycloak.config";
    public static final String REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY = REALM_STATE_ATTRIBUTE_COMMON_PREFIX + ".import-checksum-{0}";
    public static final String REALM_STATE_ATTRIBUTE_PREFIX_KEY = REALM_STATE_ATTRIBUTE_COMMON_PREFIX + ".state-{0}-{1}";

    @NotNull
    private final Collection<String> path;

    private final Collection<String> exclude;

    @NotNull
    private final boolean hiddenFiles;

    @NotNull
    private final boolean varSubstitution;

    @NotNull
    private final boolean force;

    @NotNull
    private final boolean validate;

    @NotBlank
    private final String cacheKey;

    @NotNull
    private final boolean state;

    private final String stateEncryptionKey;

    @Pattern(regexp = "^[A-Fa-f0-9]+$")
    private final String stateEncryptionSalt;

    @NotNull
    private final ImportFileType fileType;

    @NotNull
    private final boolean parallel;

    @NotNull
    private final boolean varSubstitutionInVariables;

    @NotNull
    private final boolean varSubstitutionUndefinedThrowsExceptions;

    @NotNull
    private final String varSubstitutionPrefix;

    @NotNull
    private final String varSubstitutionSuffix;

    @Valid
    private final ImportManagedProperties managed;

    @NotNull
    private final boolean syncUserFederation;

    @NotNull
    private final boolean removeDefaultRoleFromUser;

    @NotNull
    private final boolean skipAttributesForFederatedUser;

    public ImportConfigProperties(
            Collection<String> path,
            Collection<String> exclude,
            boolean hiddenFiles,
            boolean varSubstitution,
            boolean force,
            boolean validate,
            String cacheKey,
            boolean state,
            String stateEncryptionKey,
            String stateEncryptionSalt,
            ImportFileType fileType,
            boolean parallel,
            boolean varSubstitutionInVariables,
            boolean varSubstitutionUndefinedThrowsExceptions,
            String varSubstitutionPrefix,
            String varSubstitutionSuffix,
            ImportManagedProperties managed,
            boolean syncUserFederation,
            boolean removeDefaultRoleFromUser,
            boolean skipAttributesForFederatedUser) {
        this.path = path;
        this.exclude = exclude;
        this.hiddenFiles = hiddenFiles;
        this.varSubstitution = varSubstitution;
        this.force = force;
        this.validate = validate;
        this.cacheKey = cacheKey;
        this.state = state;
        this.stateEncryptionKey = stateEncryptionKey;
        this.stateEncryptionSalt = stateEncryptionSalt;
        this.fileType = fileType;
        this.parallel = parallel;
        this.varSubstitutionInVariables = varSubstitutionInVariables;
        this.varSubstitutionUndefinedThrowsExceptions = varSubstitutionUndefinedThrowsExceptions;
        this.varSubstitutionPrefix = varSubstitutionPrefix;
        this.varSubstitutionSuffix = varSubstitutionSuffix;
        this.managed = managed;
        this.syncUserFederation = syncUserFederation;
        this.removeDefaultRoleFromUser = removeDefaultRoleFromUser;
        this.skipAttributesForFederatedUser = skipAttributesForFederatedUser;
    }

    public Collection<String> getPath() {
        return path;
    }

    public Collection<String> getExclude() {
        return exclude;
    }

    public boolean isHiddenFiles() {
        return hiddenFiles;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isValidate() {
        return validate;
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

    public String getStateEncryptionKey() {
        return stateEncryptionKey;
    }

    public String getStateEncryptionSalt() {
        return stateEncryptionSalt;
    }

    public ImportFileType getFileType() {
        return fileType;
    }

    public boolean isParallel() {
        return parallel;
    }

    public boolean isVarSubstitutionInVariables() {
        return varSubstitutionInVariables;
    }

    public boolean isVarSubstitutionUndefinedThrowsExceptions() {
        return varSubstitutionUndefinedThrowsExceptions;
    }

    public String getVarSubstitutionPrefix() {
        return varSubstitutionPrefix;
    }

    public String getVarSubstitutionSuffix() {
        return varSubstitutionSuffix;
    }

    public boolean isSyncUserFederation() {
        return syncUserFederation;
    }

    public boolean isRemoveDefaultRoleFromUser() {
        return removeDefaultRoleFromUser;
    }

    public boolean isSkipAttributesForFederatedUser() {
        return skipAttributesForFederatedUser;
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
        private final ImportManagedPropertiesValues clientScopeMapping;

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

        @NotNull
        private final ImportManagedPropertiesValues client;

        @NotNull
        private final ImportManagedPropertiesValues clientAuthorizationResources;

        public ImportManagedProperties(
                ImportManagedPropertiesValues requiredAction, ImportManagedPropertiesValues group,
                ImportManagedPropertiesValues clientScope, ImportManagedPropertiesValues scopeMapping,
                ImportManagedPropertiesValues clientScopeMapping,
                ImportManagedPropertiesValues component, ImportManagedPropertiesValues subComponent,
                ImportManagedPropertiesValues authenticationFlow, ImportManagedPropertiesValues identityProvider,
                ImportManagedPropertiesValues identityProviderMapper, ImportManagedPropertiesValues role,
                ImportManagedPropertiesValues client, ImportManagedPropertiesValues clientAuthorizationResources) {
            this.requiredAction = requiredAction;
            this.group = group;
            this.clientScope = clientScope;
            this.scopeMapping = scopeMapping;
            this.clientScopeMapping = clientScopeMapping;
            this.component = component;
            this.subComponent = subComponent;
            this.authenticationFlow = authenticationFlow;
            this.identityProvider = identityProvider;
            this.identityProviderMapper = identityProviderMapper;
            this.role = role;
            this.client = client;
            this.clientAuthorizationResources = clientAuthorizationResources;
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

        public ImportManagedPropertiesValues getClientScopeMapping() {
            return clientScopeMapping;
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

        public ImportManagedPropertiesValues getClient() {
            return client;
        }

        public ImportManagedPropertiesValues getClientAuthorizationResources() {
            return clientAuthorizationResources;
        }

        public enum ImportManagedPropertiesValues {
            FULL,
            NO_DELETE
        }
    }
}
