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

package de.adorsys.keycloak.config.service.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.adorsys.keycloak.config.KeycloakConfigRunner;
import de.adorsys.keycloak.config.properties.ExportConfigProperties;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.repository.RealmRepository;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class RealmExportService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigRunner.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new YAMLMapper();
    private static final Set<String> REALM_DEFAULT_FIELD_NAMES = new HashSet<>();

    private static final Map<String, PropertyDescriptor> DESCRIPTORS = new HashMap<>();

    static {
        YAML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        REALM_DEFAULT_FIELD_NAMES.add("displayName");
        REALM_DEFAULT_FIELD_NAMES.add("displayNameHtml");
        REALM_DEFAULT_FIELD_NAMES.add("notBefore");
        REALM_DEFAULT_FIELD_NAMES.add("defaultSignatureAlgorithm");
        REALM_DEFAULT_FIELD_NAMES.add("revokeRefreshToken");
        REALM_DEFAULT_FIELD_NAMES.add("refreshTokenMaxReuse");
        REALM_DEFAULT_FIELD_NAMES.add("accessTokenLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("accessTokenLifespanForImplicitFlow");
        REALM_DEFAULT_FIELD_NAMES.add("ssoSessionIdleTimeout");
        REALM_DEFAULT_FIELD_NAMES.add("ssoSessionMaxLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("ssoSessionIdleTimeoutRememberMe");
        REALM_DEFAULT_FIELD_NAMES.add("ssoSessionMaxLifespanRememberMe");
        REALM_DEFAULT_FIELD_NAMES.add("offlineSessionIdleTimeout");
        REALM_DEFAULT_FIELD_NAMES.add("offlineSessionMaxLifespanEnabled");
        REALM_DEFAULT_FIELD_NAMES.add("offlineSessionMaxLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("clientSessionIdleTimeout");
        REALM_DEFAULT_FIELD_NAMES.add("clientSessionMaxLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("clientOfflineSessionIdleTimeout");
        REALM_DEFAULT_FIELD_NAMES.add("clientOfflineSessionMaxLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("accessCodeLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("accessCodeLifespanUserAction");
        REALM_DEFAULT_FIELD_NAMES.add("accessCodeLifespanLogin");
        REALM_DEFAULT_FIELD_NAMES.add("actionTokenGeneratedByAdminLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("actionTokenGeneratedByUserLifespan");
        REALM_DEFAULT_FIELD_NAMES.add("OAuth2DeviceCodeLifespan"); // Not equal to field name, derived from getter/setter for bean introspection
        REALM_DEFAULT_FIELD_NAMES.add("OAuth2DevicePollingInterval"); // Not equal to field name, derived from getter/setter for bean introspection
        REALM_DEFAULT_FIELD_NAMES.add("sslRequired");
        REALM_DEFAULT_FIELD_NAMES.add("registrationAllowed");
        REALM_DEFAULT_FIELD_NAMES.add("registrationEmailAsUsername");
        REALM_DEFAULT_FIELD_NAMES.add("rememberMe");
        REALM_DEFAULT_FIELD_NAMES.add("verifyEmail");
        REALM_DEFAULT_FIELD_NAMES.add("loginWithEmailAllowed");
        REALM_DEFAULT_FIELD_NAMES.add("duplicateEmailsAllowed");
        REALM_DEFAULT_FIELD_NAMES.add("resetPasswordAllowed");
        REALM_DEFAULT_FIELD_NAMES.add("editUsernameAllowed");
        REALM_DEFAULT_FIELD_NAMES.add("bruteForceProtected");
        REALM_DEFAULT_FIELD_NAMES.add("permanentLockout");
        REALM_DEFAULT_FIELD_NAMES.add("maxFailureWaitSeconds");
        REALM_DEFAULT_FIELD_NAMES.add("minimumQuickLoginWaitSeconds");
        REALM_DEFAULT_FIELD_NAMES.add("waitIncrementSeconds");
        REALM_DEFAULT_FIELD_NAMES.add("quickLoginCheckMilliSeconds");
        REALM_DEFAULT_FIELD_NAMES.add("maxDeltaTimeSeconds");
        REALM_DEFAULT_FIELD_NAMES.add("failureFactor");
        REALM_DEFAULT_FIELD_NAMES.add("privateKey");
        REALM_DEFAULT_FIELD_NAMES.add("publicKey");
        REALM_DEFAULT_FIELD_NAMES.add("certificate");
        REALM_DEFAULT_FIELD_NAMES.add("codeSecret");
        REALM_DEFAULT_FIELD_NAMES.add("passwordPolicy");
        REALM_DEFAULT_FIELD_NAMES.add("otpPolicyType");
        REALM_DEFAULT_FIELD_NAMES.add("otpPolicyAlgorithm");
        REALM_DEFAULT_FIELD_NAMES.add("otpPolicyInitialCounter");
        REALM_DEFAULT_FIELD_NAMES.add("otpPolicyDigits");
        REALM_DEFAULT_FIELD_NAMES.add("otpPolicyLookAheadWindow");
        REALM_DEFAULT_FIELD_NAMES.add("otpPolicyPeriod");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyRpEntityName");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyRpId");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyAttestationConveyancePreference");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyAuthenticatorAttachment");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyRequireResidentKey");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyUserVerificationRequirement");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyCreateTimeout");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyAvoidSameAuthenticatorRegister");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessRpEntityName");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessRpId");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessAttestationConveyancePreference");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessAuthenticatorAttachment");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessRequireResidentKey");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessUserVerificationRequirement");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessCreateTimeout");
        REALM_DEFAULT_FIELD_NAMES.add("webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister");
        REALM_DEFAULT_FIELD_NAMES.add("loginTheme");
        REALM_DEFAULT_FIELD_NAMES.add("accountTheme");
        REALM_DEFAULT_FIELD_NAMES.add("adminTheme");
        REALM_DEFAULT_FIELD_NAMES.add("emailTheme");
        REALM_DEFAULT_FIELD_NAMES.add("eventsEnabled");
        REALM_DEFAULT_FIELD_NAMES.add("eventsExpiration");
        REALM_DEFAULT_FIELD_NAMES.add("adminEventsEnabled");
        REALM_DEFAULT_FIELD_NAMES.add("adminEventsDetailsEnabled");
        REALM_DEFAULT_FIELD_NAMES.add("internationalizationEnabled");
        REALM_DEFAULT_FIELD_NAMES.add("defaultLocale");
        REALM_DEFAULT_FIELD_NAMES.add("browserFlow");
        REALM_DEFAULT_FIELD_NAMES.add("registrationFlow");
        REALM_DEFAULT_FIELD_NAMES.add("directGrantFlow");
        REALM_DEFAULT_FIELD_NAMES.add("resetCredentialsFlow");
        REALM_DEFAULT_FIELD_NAMES.add("clientAuthenticationFlow");
        REALM_DEFAULT_FIELD_NAMES.add("dockerAuthenticationFlow");
        REALM_DEFAULT_FIELD_NAMES.add("keycloakVersion");
        REALM_DEFAULT_FIELD_NAMES.add("userManagedAccessAllowed");

        /*
         * TODO fields:
         *
         * roles
         * groups
         * defaultRoles
         * defaultRole
         * defaultGroups
         * requiredCredentials
         * otpSupportedApplications
         * webAuthnPolicySignatureAlgorithms
         * webAuthnPolicyAcceptableAaguids
         * webAuthnPolicyPasswordlessSignatureAlgorithms
         * webAuthnPolicyPasswordlessAcceptableAaguids
         * clientProfiles
         * clientPolicies
         * users
         * federatedUsers
         * scopeMappings
         * clientScopeMappings
         * clients
         * clientScopes
         * defaultDefaultClientScopes
         * defaultOptionalClientScopes
         * browserSecurityHeaders
         * smtpServer
         * userFederationProviders
         * userFederationMappers
         * eventsListeners
         * enabledEventTypes
         * identityProviders
         * identityProviderMappers
         * protocolMappers
         * components
         * supportedLocales
         * authenticationFlows
         * authenticatorConfig
         * requiredActions
         * attributes
         */

        try {
            for (var descriptor : Introspector.getBeanInfo(RealmRepresentation.class).getPropertyDescriptors()) {
                var fieldName = descriptor.getName();
                if (REALM_DEFAULT_FIELD_NAMES.contains(fieldName)) {
                    // Override the read method for boxed booleans if they don't use the get-Prefix
                    if ((descriptor.getPropertyType().equals(Boolean.class) && descriptor.getReadMethod() == null)) {
                        var getterName = "is" + capitalizeFieldName(fieldName);
                        descriptor.setReadMethod(RealmRepresentation.class.getMethod(getterName));
                    }

                    // For some reason, setDockerAuthenticationFlow is a "fluent-style" setter and returns itself, which disqualifies it as a setter
                    if (fieldName.equals("dockerAuthenticationFlow")) {
                        descriptor.setWriteMethod(RealmRepresentation.class.getMethod("setDockerAuthenticationFlow", String.class));
                    }

                    DESCRIPTORS.put(fieldName, descriptor);
                }
            }
        } catch (IntrospectionException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static String capitalizeFieldName(String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private final RealmRepository realmRepository;
    private final ExportConfigProperties exportConfigProperties;
    private final KeycloakConfigProperties keycloakConfigProperties;

    @Autowired
    public RealmExportService(RealmRepository realmRepository,
                              ExportConfigProperties exportConfigProperties,
                              KeycloakConfigProperties keycloakConfigProperties) {
        this.realmRepository = realmRepository;
        this.exportConfigProperties = exportConfigProperties;
        this.keycloakConfigProperties = keycloakConfigProperties;
    }

    public void doExports() throws IOException, IntrospectionException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        var outputLocation = Paths.get(exportConfigProperties.getLocation());
        if (!Files.exists(outputLocation)) {
            Files.createDirectories(outputLocation);
        }
        if (!Files.isDirectory(outputLocation)) {
            logger.error("Output location '{}' is not a directory. Aborting.", exportConfigProperties.getLocation());
        }
        var keycloakConfigVersion = keycloakConfigProperties.getVersion();
        var exportVersion = exportConfigProperties.getKeycloakVersion();
        if (!exportVersion.equals(keycloakConfigVersion)) {
            logger.warn("Keycloak-Config-CLI keycloak version {} and export keycloak version {} are not equal."
                            + " This may cause problems if the API changed."
                            + " Please compile keycloak-config-cli with a matching keycloak version!",
                    keycloakConfigVersion, exportVersion);
        }
        RealmRepresentation defaultRealm;
        try (var is = getClass().getResourceAsStream(String.format("/reference-realms/%s/realm.json", exportConfigProperties.getKeycloakVersion()))) {
            if (is == null) {
                logger.error("Reference realm for version {} does not exist", exportConfigProperties.getKeycloakVersion());
                return;
            }
            defaultRealm = OBJECT_MAPPER.readValue(is, RealmRepresentation.class);
        }
        var excludes = exportConfigProperties.getExcludes();
        for (var realm : realmRepository.getRealms()) {
            var realmName = realm.getRealm();
            if (excludes.contains(realmName)) {
                logger.info("Skipping realm {}", realmName);
            } else {
                logger.info("Exporting realm {}", realmName);
                var strippedRealm = new RealmRepresentation();
                strippedRealm.setRealm(realm.getRealm());
                strippedRealm.setEnabled(realm.isEnabled());
                if (!realm.getId().equals(realm.getRealm())) {
                    // If the realm ID diverges from the name, include it in the dump, otherwise ignore it
                    strippedRealm.setId(realm.getId());
                }

                for (var fieldName : REALM_DEFAULT_FIELD_NAMES) {
                    setNonDefaultValue(strippedRealm, defaultRealm, realm, fieldName);
                }

                var outputFile = Paths.get(exportConfigProperties.getLocation(), String.format("%s.yaml", realmName));
                try (var os = new FileOutputStream(outputFile.toFile())) {
                    YAML_MAPPER.writeValue(os, strippedRealm);
                }
            }
        }
    }

    private void setNonDefaultValue(RealmRepresentation strippedRealm,
                                    RealmRepresentation defaultRealm,
                                    RealmRepresentation exportedRealm,
                                    String fieldName) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        var propertyDescriptor = DESCRIPTORS.get(fieldName);
        if (propertyDescriptor == null) {
            logger.error("Can't set field '{}', no such property on RealmRepresentation", fieldName);
            return;
        }

        var getter = propertyDescriptor.getReadMethod();
        var setter = propertyDescriptor.getWriteMethod();

        /*
         * These methods need special treatment because while the getter returns boxed types, the setter accepts unboxed types.
         * This causes a mismatch and the proper getter can't be found.
         */
        if (fieldName.equals("eventsEnabled")) {
            getter = RealmRepresentation.class.getMethod("isEventsEnabled");
        }
        if (fieldName.equals("eventsExpiration")) {
            setter = RealmRepresentation.class.getMethod("setEventsExpiration", long.class);
        }
        Object defaultValue = getter.invoke(defaultRealm);
        Object exportedValue = getter.invoke(exportedRealm);

        if (!Objects.equals(defaultValue, exportedValue)) {
            /*
             * If the special setters are called with null values, we get an NPE because these only accept primitives
             * Therefore, do nothing, the underlying value will still be null because it's not a primitive
             */
            if (!((fieldName.equals("eventsEnabled") || fieldName.equals("eventsExpiration")) && exportedValue == null)) {
                setter.invoke(strippedRealm, exportedValue);
            }
        }
    }
}
