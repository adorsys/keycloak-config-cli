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
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ConfigurationProperties(prefix = "import", ignoreUnknownFields = false)
@Validated
@SuppressWarnings({"java:S107"})
public class ImportConfigProperties {
    public static final String REALM_STATE_ATTRIBUTE_COMMON_PREFIX = "de.adorsys.keycloak.config";
    public static final String REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY = REALM_STATE_ATTRIBUTE_COMMON_PREFIX + ".import-checksum-{0}";
    public static final String REALM_STATE_ATTRIBUTE_PREFIX_KEY = REALM_STATE_ATTRIBUTE_COMMON_PREFIX + ".state-{0}-{1}";

    @NotNull
    private final boolean validate;

    @NotNull
    private final boolean parallel;

    @Valid
    private final ImportFilesProperties files;

    @Valid
    private final ImportVarSubstitutionProperties varSubstitution;

    @Valid
    private final ImportBehaviorsProperties behaviors;

    @Valid
    private final ImportCacheProperties cache;

    @Valid
    private final ImportManagedProperties managed;

    @Valid
    private final ImportRemoteStateProperties remoteState;

    public ImportConfigProperties(@DefaultValue("true") boolean validate,
                                  @DefaultValue("false") boolean parallel,
                                  @DefaultValue ImportFilesProperties files,
                                  @DefaultValue ImportVarSubstitutionProperties varSubstitution,
                                  @DefaultValue ImportBehaviorsProperties behaviors,
                                  @DefaultValue ImportCacheProperties cache,
                                  @DefaultValue ImportManagedProperties managed,
                                  @DefaultValue ImportRemoteStateProperties remoteState
    ) {
        this.validate = validate;
        this.parallel = parallel;
        this.files = files;
        this.varSubstitution = varSubstitution;
        this.behaviors = behaviors;
        this.cache = cache;
        this.managed = managed;
        this.remoteState = remoteState;
    }

    public boolean isValidate() {
        return validate;
    }

    public boolean isParallel() {
        return parallel;
    }

    public ImportFilesProperties getFiles() {
        return files;
    }

    public ImportVarSubstitutionProperties getVarSubstitution() {
        return varSubstitution;
    }

    public ImportBehaviorsProperties getBehaviors() {
        return behaviors;
    }

    public ImportCacheProperties getCache() {
        return cache;
    }

    public ImportManagedProperties getManaged() {
        return managed;
    }

    public ImportRemoteStateProperties getRemoteState() {
        return remoteState;
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

        @NotNull
        private final ImportManagedPropertiesValues clientAuthorizationPolicies;

        @NotNull
        private final ImportManagedPropertiesValues clientAuthorizationScopes;

        @NotNull
        private final ImportManagedPropertiesValues messageBundles;

        public ImportManagedProperties(@DefaultValue("FULL") ImportManagedPropertiesValues requiredAction,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues group,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues clientScope,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues scopeMapping,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues clientScopeMapping,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues component,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues subComponent,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues authenticationFlow,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues identityProvider,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues identityProviderMapper,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues role,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues client,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues clientAuthorizationResources,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues clientAuthorizationPolicies,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues clientAuthorizationScopes,
                                       @DefaultValue("FULL") ImportManagedPropertiesValues messageBundles) {
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
            this.clientAuthorizationPolicies = clientAuthorizationPolicies;
            this.clientAuthorizationScopes = clientAuthorizationScopes;
            this.messageBundles = messageBundles;
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

        public ImportManagedPropertiesValues getClientAuthorizationPolicies() {
            return clientAuthorizationPolicies;
        }

        public ImportManagedPropertiesValues getClientAuthorizationScopes() {
            return clientAuthorizationScopes;
        }

        public ImportManagedPropertiesValues getMessageBundles() {
            return messageBundles;
        }

        public enum ImportManagedPropertiesValues {
            FULL, NO_DELETE
        }
    }

    @SuppressWarnings("unused")
    public static class ImportFilesProperties {
        @NotNull
        private final Collection<String> locations;

        @NotNull
        private final Collection<String> excludes;

        @NotNull
        private final boolean includeHiddenFiles;

        public ImportFilesProperties(Collection<String> locations,
                                     @DefaultValue Collection<String> excludes,
                                     @DefaultValue("false") boolean includeHiddenFiles) {
            this.locations = locations;
            this.excludes = excludes;
            this.includeHiddenFiles = includeHiddenFiles;
        }

        public Collection<String> getLocations() {
            return locations;
        }

        public Collection<String> getExcludes() {
            return excludes;
        }

        public boolean isIncludeHiddenFiles() {
            return includeHiddenFiles;
        }
    }

    @SuppressWarnings("unused")
    public static class ImportVarSubstitutionProperties {
        @NotNull
        private final boolean enabled;

        @NotNull
        private final boolean nested;

        @NotNull
        private final boolean undefinedIsError;

        @NotNull
        private final String prefix;

        @NotNull
        private final String suffix;

        public ImportVarSubstitutionProperties(@DefaultValue("false") boolean enabled,
                                               @DefaultValue("true") boolean nested,
                                               @DefaultValue("true") boolean undefinedIsError,
                                               @DefaultValue("$(") String prefix,
                                               @DefaultValue(")") String suffix) {
            this.enabled = enabled;
            this.nested = nested;
            this.undefinedIsError = undefinedIsError;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isNested() {
            return nested;
        }

        public boolean isUndefinedIsError() {
            return undefinedIsError;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    @SuppressWarnings("unused")
    public static class ImportBehaviorsProperties {
        @NotNull
        private final boolean syncUserFederation;

        @NotNull
        private final boolean removeDefaultRoleFromUser;

        @NotNull
        private final boolean skipAttributesForFederatedUser;

        @NotNull
        private final boolean checksumWithCacheKey;

        @NotNull
        private final ChecksumChangedOption checksumChanged;

        public ImportBehaviorsProperties(boolean syncUserFederation, boolean removeDefaultRoleFromUser, boolean skipAttributesForFederatedUser,
                                         boolean checksumWithCacheKey, ChecksumChangedOption checksumChanged) {
            this.syncUserFederation = syncUserFederation;
            this.removeDefaultRoleFromUser = removeDefaultRoleFromUser;
            this.skipAttributesForFederatedUser = skipAttributesForFederatedUser;
            this.checksumWithCacheKey = checksumWithCacheKey;
            this.checksumChanged = checksumChanged;
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

        public boolean isChecksumWithCacheKey() {
            return checksumWithCacheKey;
        }

        public ChecksumChangedOption getChecksumChanged() {
            return checksumChanged;
        }

        public enum ChecksumChangedOption {
            CONTINUE, FAIL
        }
    }

    @SuppressWarnings("unused")
    public static class ImportCacheProperties {
        @NotNull
        private final boolean enabled;

        @NotNull
        private final String key;

        public ImportCacheProperties(@DefaultValue("true") boolean enabled,
                                     @DefaultValue("default") String key) {
            this.enabled = enabled;
            this.key = key;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getKey() {
            return key;
        }
    }

    @SuppressWarnings("unused")
    public static class ImportRemoteStateProperties {
        @NotNull
        private final boolean enabled;

        private final String encryptionKey;

        @Pattern(regexp = "^[A-Fa-f0-9]+$")
        private final String encryptionSalt;

        public ImportRemoteStateProperties(@DefaultValue("true") boolean enabled,
                                           String encryptionKey,
                                           @DefaultValue("2B521C795FBE2F2425DB150CD3700BA9") String encryptionSalt) {
            this.enabled = enabled;
            this.encryptionKey = encryptionKey;
            this.encryptionSalt = encryptionSalt;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public String getEncryptionSalt() {
            return encryptionSalt;
        }
    }
}
