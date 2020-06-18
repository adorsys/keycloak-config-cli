/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

package de.adorsys.keycloak.config;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

class ImportIdentityProvidersIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithIdentityProviders";

    ImportIdentityProvidersIT() {
        this.resourcePath = "import-files/identity-providers";
    }

    @Test
    @Order(0)
    void shouldCreateIdentityProvider() {
        doImport("0_create_realm_with_identity-providers.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<IdentityProviderRepresentation> identityProviders = createdRealm.getIdentityProviders();
        assertThat(identityProviders.size(), is(1));

        IdentityProviderRepresentation identityProvider = identityProviders.get(0);

        assertThat(identityProvider.getAlias(), is("saml"));
        assertThat(identityProvider.getProviderId(), is("saml"));
        assertThat(identityProvider.getDisplayName(), is(nullValue()));
        assertThat(identityProvider.isEnabled(), is(true));
        assertThat(identityProvider.isTrustEmail(), is(true));
        assertThat(identityProvider.isStoreToken(), is(false));
        assertThat(identityProvider.isAddReadTokenRoleOnCreate(), is(true));
        assertThat(identityProvider.isLinkOnly(), is(false));
        assertThat(identityProvider.getFirstBrokerLoginFlowAlias(), is("first broker login"));

        Map<String, String> config = identityProvider.getConfig();

        assertThat(config.get("validateSignature"), is("false"));
        assertThat(config.get("samlXmlKeyNameTranformer"), is("KEY_ID"));
        assertThat(config.get("signingCertificate"), is("MIIDETCCAfmgAwIBAgIUZRpDhkNKl5eWtJqk0Bu1BgTTargwDQYJKoZIhvcNAQEL\nBQAwFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwHhcNMTgwODI0MjExNDEwWhcNMzgw\nODI0MjExNDEwWjAWMRQwEgYDVQQDDAtzYW1sdGVzdC5pZDCCASIwDQYJKoZIhvcN\nAQEBBQADggEPADCCAQoCggEBAJrh9/PcDsiv3UeL8Iv9rf4WfLPxuOm9W6aCntEA\n8l6c1LQ1Zyrz+Xa/40ZgP29ENf3oKKbPCzDcc6zooHMji2fBmgXp6Li3fQUzu7yd\n+nIC2teejijVtrNLjn1WUTwmqjLtuzrKC/ePoZyIRjpoUxyEMJopAd4dJmAcCq/K\nk2eYX9GYRlqvIjLFoGNgy2R4dWwAKwljyh6pdnPUgyO/WjRDrqUBRFrLQJorR2kD\nc4seZUbmpZZfp4MjmWMDgyGM1ZnR0XvNLtYeWAyt0KkSvFoOMjZUeVK/4xR74F8e\n8ToPqLmZEg9ZUx+4z2KjVK00LpdRkH9Uxhh03RQ0FabHW6UCAwEAAaNXMFUwHQYD\nVR0OBBYEFJDbe6uSmYQScxpVJhmt7PsCG4IeMDQGA1UdEQQtMCuCC3NhbWx0ZXN0\nLmlkhhxodHRwczovL3NhbWx0ZXN0LmlkL3NhbWwvaWRwMA0GCSqGSIb3DQEBCwUA\nA4IBAQBNcF3zkw/g51q26uxgyuy4gQwnSr01Mhvix3Dj/Gak4tc4XwvxUdLQq+jC\ncxr2Pie96klWhY/v/JiHDU2FJo9/VWxmc/YOk83whvNd7mWaNMUsX3xGv6AlZtCO\nL3JhCpHjiN+kBcMgS5jrtGgV1Lz3/1zpGxykdvS0B4sPnFOcaCwHe2B9SOCWbDAN\nJXpTjz1DmJO4ImyWPJpN1xsYKtm67Pefxmn0ax0uE2uuzq25h0xbTkqIQgJzyoE/\nDPkBFK1vDkMfAW11dQ0BXatEnW7Gtkc0lh2/PIbHWj4AzxYMyBf5Gy6HSVOftwjC\nvoQR2qr2xJBixsg+MIORKtmKHLfU,MIIDEjCCAfqgAwIBAgIVAMECQ1tjghafm5OxWDh9hwZfxthWMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQC0Z4QX1NFKs71ufbQwoQoW7qkNAJRIANGA4iM0\nThYghul3pC+FwrGv37aTxWXfA1UG9njKbbDreiDAZKngCgyjxj0uJ4lArgkr4AOE\njj5zXA81uGHARfUBctvQcsZpBIxDOvUUImAl+3NqLgMGF2fktxMG7kX3GEVNc1kl\nbN3dfYsaw5dUrw25DheL9np7G/+28GwHPvLb4aptOiONbCaVvh9UMHEA9F7c0zfF\n/cL5fOpdVa54wTI0u12CsFKt78h6lEGG5jUs/qX9clZncJM7EFkN3imPPy+0HC8n\nspXiH/MZW8o2cqWRkrw3MzBZW3Ojk5nQj40V6NUbjb7kfejzAgMBAAGjVzBVMB0G\nA1UdDgQWBBQT6Y9J3Tw/hOGc8PNV7JEE4k2ZNTA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEASk3guKfTkVhEaIVvxEPNR2w3vWt3fwmwJCccW98XXLWgNbu3YaMb2RSn\n7Th4p3h+mfyk2don6au7Uyzc1Jd39RNv80TG5iQoxfCgphy1FYmmdaSfO8wvDtHT\nTNiLArAxOYtzfYbzb5QrNNH/gQEN8RJaEf/g/1GTw9x/103dSMK0RXtl+fRs2nbl\nD1JJKSQ3AdhxK/weP3aUPtLxVVJ9wMOQOfcy02l+hHMb6uAjsPOpOVKqi3M8XmcU\nZOpx4swtgGdeoSpeRyrtMvRwdcciNBp9UZome44qZAYH1iqrpmmjsfI9pJItsgWu\n3kXPjhSfj1AJGR1l9JGvJrHki1iHTA=="));
        assertThat(config.get("postBindingLogout"), is("true"));
        assertThat(config.get("nameIDPolicyFormat"), is("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(config.get("postBindingResponse"), is("true"));
        assertThat(config.get("singleLogoutServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SLO"));
        assertThat(config.get("signatureAlgorithm"), is("RSA_SHA256"));
        assertThat(config.get("useJwksUrl"), is("true"));
        assertThat(config.get("postBindingAuthnRequest"), is("true"));
        assertThat(config.get("singleSignOnServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SSO"));
        assertThat(config.get("wantAuthnRequestsSigned"), is("false"));
        assertThat(config.get("addExtensionsElementWithKeyInfo"), is("false"));
        assertThat(config.get("encryptionPublicKey"), is("MIIDEjCCAfqgAwIBAgIVAPVbodo8Su7/BaHXUHykx0Pi5CFaMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQCQb+1a7uDdTTBBFfwOUun3IQ9nEuKM98SmJDWa\nMwM877elswKUTIBVh5gB2RIXAPZt7J/KGqypmgw9UNXFnoslpeZbA9fcAqqu28Z4\nsSb2YSajV1ZgEYPUKvXwQEmLWN6aDhkn8HnEZNrmeXihTFdyr7wjsLj0JpQ+VUlc\n4/J+hNuU7rGYZ1rKY8AA34qDVd4DiJ+DXW2PESfOu8lJSOteEaNtbmnvH8KlwkDs\n1NvPTsI0W/m4SK0UdXo6LLaV8saIpJfnkVC/FwpBolBrRC/Em64UlBsRZm2T89ca\nuzDee2yPUvbBd5kLErw+sC7i4xXa2rGmsQLYcBPhsRwnmBmlAgMBAAGjVzBVMB0G\nA1UdDgQWBBRZ3exEu6rCwRe5C7f5QrPcAKRPUjA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEABZDFRNtcbvIRmblnZItoWCFhVUlq81ceSQddLYs8DqK340//hWNAbYdj\nWcP85HhIZnrw6NGCO4bUipxZXhiqTA/A9d1BUll0vYB8qckYDEdPDduYCOYemKkD\ndmnHMQWs9Y6zWiYuNKEJ9mf3+1N8knN/PK0TYVjVjXAf2CnOETDbLtlj6Nqb8La3\nsQkYmU+aUdopbjd5JFFwbZRaj6KiHXHtnIRgu8sUXNPrgipUgZUOVhP0C0N5OfE4\nJW8ZBrKgQC/6vJ2rSa9TlzI6JAa5Ww7gMXMP9M+cJUNQklcq+SBnTK8G+uBHgPKR\nzBDsMIEzRtQZm4GIoHJae4zmnCekkQ=="));
        assertThat(config.get("principalType"), is("SUBJECT"));
    }

    @Test
    @Order(1)
    void shouldUpdateIdentityProvider() {
        doImport("1_update_identity-provider.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<IdentityProviderRepresentation> identityProviders = createdRealm.getIdentityProviders();
        assertThat(identityProviders.size(), is(1));

        IdentityProviderRepresentation identityProvider = identityProviders.get(0);

        assertThat(identityProvider.getAlias(), is("saml"));
        assertThat(identityProvider.getProviderId(), is("saml"));
        assertThat(identityProvider.getDisplayName(), is(nullValue()));
        assertThat(identityProvider.isEnabled(), is(false));
        assertThat(identityProvider.isTrustEmail(), is(true));
        assertThat(identityProvider.isStoreToken(), is(false));
        assertThat(identityProvider.isAddReadTokenRoleOnCreate(), is(true));
        assertThat(identityProvider.isLinkOnly(), is(false));
        assertThat(identityProvider.getFirstBrokerLoginFlowAlias(), is("first broker login"));

        Map<String, String> config = identityProvider.getConfig();

        assertThat(config.get("validateSignature"), is("true"));
        assertThat(config.get("samlXmlKeyNameTranformer"), is("KEY_ID"));
        assertThat(config.get("signingCertificate"), is("MIIDETCCAfmgAwIBAgIUZRpDhkNKl5eWtJqk0Bu1BgTTargwDQYJKoZIhvcNAQEL\nBQAwFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwHhcNMTgwODI0MjExNDEwWhcNMzgw\nODI0MjExNDEwWjAWMRQwEgYDVQQDDAtzYW1sdGVzdC5pZDCCASIwDQYJKoZIhvcN\nAQEBBQADggEPADCCAQoCggEBAJrh9/PcDsiv3UeL8Iv9rf4WfLPxuOm9W6aCntEA\n8l6c1LQ1Zyrz+Xa/40ZgP29ENf3oKKbPCzDcc6zooHMji2fBmgXp6Li3fQUzu7yd\n+nIC2teejijVtrNLjn1WUTwmqjLtuzrKC/ePoZyIRjpoUxyEMJopAd4dJmAcCq/K\nk2eYX9GYRlqvIjLFoGNgy2R4dWwAKwljyh6pdnPUgyO/WjRDrqUBRFrLQJorR2kD\nc4seZUbmpZZfp4MjmWMDgyGM1ZnR0XvNLtYeWAyt0KkSvFoOMjZUeVK/4xR74F8e\n8ToPqLmZEg9ZUx+4z2KjVK00LpdRkH9Uxhh03RQ0FabHW6UCAwEAAaNXMFUwHQYD\nVR0OBBYEFJDbe6uSmYQScxpVJhmt7PsCG4IeMDQGA1UdEQQtMCuCC3NhbWx0ZXN0\nLmlkhhxodHRwczovL3NhbWx0ZXN0LmlkL3NhbWwvaWRwMA0GCSqGSIb3DQEBCwUA\nA4IBAQBNcF3zkw/g51q26uxgyuy4gQwnSr01Mhvix3Dj/Gak4tc4XwvxUdLQq+jC\ncxr2Pie96klWhY/v/JiHDU2FJo9/VWxmc/YOk83whvNd7mWaNMUsX3xGv6AlZtCO\nL3JhCpHjiN+kBcMgS5jrtGgV1Lz3/1zpGxykdvS0B4sPnFOcaCwHe2B9SOCWbDAN\nJXpTjz1DmJO4ImyWPJpN1xsYKtm67Pefxmn0ax0uE2uuzq25h0xbTkqIQgJzyoE/\nDPkBFK1vDkMfAW11dQ0BXatEnW7Gtkc0lh2/PIbHWj4AzxYMyBf5Gy6HSVOftwjC\nvoQR2qr2xJBixsg+MIORKtmKHLfU,MIIDEjCCAfqgAwIBAgIVAMECQ1tjghafm5OxWDh9hwZfxthWMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQC0Z4QX1NFKs71ufbQwoQoW7qkNAJRIANGA4iM0\nThYghul3pC+FwrGv37aTxWXfA1UG9njKbbDreiDAZKngCgyjxj0uJ4lArgkr4AOE\njj5zXA81uGHARfUBctvQcsZpBIxDOvUUImAl+3NqLgMGF2fktxMG7kX3GEVNc1kl\nbN3dfYsaw5dUrw25DheL9np7G/+28GwHPvLb4aptOiONbCaVvh9UMHEA9F7c0zfF\n/cL5fOpdVa54wTI0u12CsFKt78h6lEGG5jUs/qX9clZncJM7EFkN3imPPy+0HC8n\nspXiH/MZW8o2cqWRkrw3MzBZW3Ojk5nQj40V6NUbjb7kfejzAgMBAAGjVzBVMB0G\nA1UdDgQWBBQT6Y9J3Tw/hOGc8PNV7JEE4k2ZNTA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEASk3guKfTkVhEaIVvxEPNR2w3vWt3fwmwJCccW98XXLWgNbu3YaMb2RSn\n7Th4p3h+mfyk2don6au7Uyzc1Jd39RNv80TG5iQoxfCgphy1FYmmdaSfO8wvDtHT\nTNiLArAxOYtzfYbzb5QrNNH/gQEN8RJaEf/g/1GTw9x/103dSMK0RXtl+fRs2nbl\nD1JJKSQ3AdhxK/weP3aUPtLxVVJ9wMOQOfcy02l+hHMb6uAjsPOpOVKqi3M8XmcU\nZOpx4swtgGdeoSpeRyrtMvRwdcciNBp9UZome44qZAYH1iqrpmmjsfI9pJItsgWu\n3kXPjhSfj1AJGR1l9JGvJrHki1iHTA=="));
        assertThat(config.get("postBindingLogout"), is("true"));
        assertThat(config.get("nameIDPolicyFormat"), is("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(config.get("postBindingResponse"), is("true"));
        assertThat(config.get("singleLogoutServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SLO"));
        assertThat(config.get("signatureAlgorithm"), is("RSA_SHA256"));
        assertThat(config.get("useJwksUrl"), is("true"));
        assertThat(config.get("postBindingAuthnRequest"), is("true"));
        assertThat(config.get("singleSignOnServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SSO"));
        assertThat(config.get("wantAuthnRequestsSigned"), is("false"));
        assertThat(config.get("addExtensionsElementWithKeyInfo"), is("false"));
        assertThat(config.get("encryptionPublicKey"), is("MIIDEjCCAfqgAwIBAgIVAPVbodo8Su7/BaHXUHykx0Pi5CFaMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQCQb+1a7uDdTTBBFfwOUun3IQ9nEuKM98SmJDWa\nMwM877elswKUTIBVh5gB2RIXAPZt7J/KGqypmgw9UNXFnoslpeZbA9fcAqqu28Z4\nsSb2YSajV1ZgEYPUKvXwQEmLWN6aDhkn8HnEZNrmeXihTFdyr7wjsLj0JpQ+VUlc\n4/J+hNuU7rGYZ1rKY8AA34qDVd4DiJ+DXW2PESfOu8lJSOteEaNtbmnvH8KlwkDs\n1NvPTsI0W/m4SK0UdXo6LLaV8saIpJfnkVC/FwpBolBrRC/Em64UlBsRZm2T89ca\nuzDee2yPUvbBd5kLErw+sC7i4xXa2rGmsQLYcBPhsRwnmBmlAgMBAAGjVzBVMB0G\nA1UdDgQWBBRZ3exEu6rCwRe5C7f5QrPcAKRPUjA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEABZDFRNtcbvIRmblnZItoWCFhVUlq81ceSQddLYs8DqK340//hWNAbYdj\nWcP85HhIZnrw6NGCO4bUipxZXhiqTA/A9d1BUll0vYB8qckYDEdPDduYCOYemKkD\ndmnHMQWs9Y6zWiYuNKEJ9mf3+1N8knN/PK0TYVjVjXAf2CnOETDbLtlj6Nqb8La3\nsQkYmU+aUdopbjd5JFFwbZRaj6KiHXHtnIRgu8sUXNPrgipUgZUOVhP0C0N5OfE4\nJW8ZBrKgQC/6vJ2rSa9TlzI6JAa5Ww7gMXMP9M+cJUNQklcq+SBnTK8G+uBHgPKR\nzBDsMIEzRtQZm4GIoHJae4zmnCekkQ=="));
        assertThat(config.get("principalType"), is("SUBJECT"));
    }

    @Test
    @Order(2)
    void shouldCreateOtherIdentityProvider() {
        doImport("2_create_other_identity-provider.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<IdentityProviderRepresentation> identityProviders = createdRealm.getIdentityProviders();
        assertThat(identityProviders.size(), is(2));

        IdentityProviderRepresentation updatedIdentityProvider = identityProviders.stream()
                .filter(e -> e.getAlias().equals("saml"))
                .findFirst()
                .orElse(null);

        assertThat(updatedIdentityProvider, is(not(nullValue())));
        assertThat(updatedIdentityProvider.getAlias(), is("saml"));
        assertThat(updatedIdentityProvider.getProviderId(), is("saml"));
        assertThat(updatedIdentityProvider.getDisplayName(), is(nullValue()));
        assertThat(updatedIdentityProvider.isEnabled(), is(false));
        assertThat(updatedIdentityProvider.isTrustEmail(), is(true));
        assertThat(updatedIdentityProvider.isStoreToken(), is(false));
        assertThat(updatedIdentityProvider.isAddReadTokenRoleOnCreate(), is(true));
        assertThat(updatedIdentityProvider.isLinkOnly(), is(false));
        assertThat(updatedIdentityProvider.getFirstBrokerLoginFlowAlias(), is("first broker login"));

        Map<String, String> updatedIdentityProviderConfig = updatedIdentityProvider.getConfig();

        assertThat(updatedIdentityProviderConfig.get("validateSignature"), is("true"));
        assertThat(updatedIdentityProviderConfig.get("samlXmlKeyNameTranformer"), is("KEY_ID"));
        assertThat(updatedIdentityProviderConfig.get("signingCertificate"), is("MIIDETCCAfmgAwIBAgIUZRpDhkNKl5eWtJqk0Bu1BgTTargwDQYJKoZIhvcNAQEL\nBQAwFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwHhcNMTgwODI0MjExNDEwWhcNMzgw\nODI0MjExNDEwWjAWMRQwEgYDVQQDDAtzYW1sdGVzdC5pZDCCASIwDQYJKoZIhvcN\nAQEBBQADggEPADCCAQoCggEBAJrh9/PcDsiv3UeL8Iv9rf4WfLPxuOm9W6aCntEA\n8l6c1LQ1Zyrz+Xa/40ZgP29ENf3oKKbPCzDcc6zooHMji2fBmgXp6Li3fQUzu7yd\n+nIC2teejijVtrNLjn1WUTwmqjLtuzrKC/ePoZyIRjpoUxyEMJopAd4dJmAcCq/K\nk2eYX9GYRlqvIjLFoGNgy2R4dWwAKwljyh6pdnPUgyO/WjRDrqUBRFrLQJorR2kD\nc4seZUbmpZZfp4MjmWMDgyGM1ZnR0XvNLtYeWAyt0KkSvFoOMjZUeVK/4xR74F8e\n8ToPqLmZEg9ZUx+4z2KjVK00LpdRkH9Uxhh03RQ0FabHW6UCAwEAAaNXMFUwHQYD\nVR0OBBYEFJDbe6uSmYQScxpVJhmt7PsCG4IeMDQGA1UdEQQtMCuCC3NhbWx0ZXN0\nLmlkhhxodHRwczovL3NhbWx0ZXN0LmlkL3NhbWwvaWRwMA0GCSqGSIb3DQEBCwUA\nA4IBAQBNcF3zkw/g51q26uxgyuy4gQwnSr01Mhvix3Dj/Gak4tc4XwvxUdLQq+jC\ncxr2Pie96klWhY/v/JiHDU2FJo9/VWxmc/YOk83whvNd7mWaNMUsX3xGv6AlZtCO\nL3JhCpHjiN+kBcMgS5jrtGgV1Lz3/1zpGxykdvS0B4sPnFOcaCwHe2B9SOCWbDAN\nJXpTjz1DmJO4ImyWPJpN1xsYKtm67Pefxmn0ax0uE2uuzq25h0xbTkqIQgJzyoE/\nDPkBFK1vDkMfAW11dQ0BXatEnW7Gtkc0lh2/PIbHWj4AzxYMyBf5Gy6HSVOftwjC\nvoQR2qr2xJBixsg+MIORKtmKHLfU,MIIDEjCCAfqgAwIBAgIVAMECQ1tjghafm5OxWDh9hwZfxthWMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQC0Z4QX1NFKs71ufbQwoQoW7qkNAJRIANGA4iM0\nThYghul3pC+FwrGv37aTxWXfA1UG9njKbbDreiDAZKngCgyjxj0uJ4lArgkr4AOE\njj5zXA81uGHARfUBctvQcsZpBIxDOvUUImAl+3NqLgMGF2fktxMG7kX3GEVNc1kl\nbN3dfYsaw5dUrw25DheL9np7G/+28GwHPvLb4aptOiONbCaVvh9UMHEA9F7c0zfF\n/cL5fOpdVa54wTI0u12CsFKt78h6lEGG5jUs/qX9clZncJM7EFkN3imPPy+0HC8n\nspXiH/MZW8o2cqWRkrw3MzBZW3Ojk5nQj40V6NUbjb7kfejzAgMBAAGjVzBVMB0G\nA1UdDgQWBBQT6Y9J3Tw/hOGc8PNV7JEE4k2ZNTA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEASk3guKfTkVhEaIVvxEPNR2w3vWt3fwmwJCccW98XXLWgNbu3YaMb2RSn\n7Th4p3h+mfyk2don6au7Uyzc1Jd39RNv80TG5iQoxfCgphy1FYmmdaSfO8wvDtHT\nTNiLArAxOYtzfYbzb5QrNNH/gQEN8RJaEf/g/1GTw9x/103dSMK0RXtl+fRs2nbl\nD1JJKSQ3AdhxK/weP3aUPtLxVVJ9wMOQOfcy02l+hHMb6uAjsPOpOVKqi3M8XmcU\nZOpx4swtgGdeoSpeRyrtMvRwdcciNBp9UZome44qZAYH1iqrpmmjsfI9pJItsgWu\n3kXPjhSfj1AJGR1l9JGvJrHki1iHTA=="));
        assertThat(updatedIdentityProviderConfig.get("postBindingLogout"), is("true"));
        assertThat(updatedIdentityProviderConfig.get("nameIDPolicyFormat"), is("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(updatedIdentityProviderConfig.get("postBindingResponse"), is("true"));
        assertThat(updatedIdentityProviderConfig.get("singleLogoutServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SLO"));
        assertThat(updatedIdentityProviderConfig.get("signatureAlgorithm"), is("RSA_SHA256"));
        assertThat(updatedIdentityProviderConfig.get("useJwksUrl"), is("true"));
        assertThat(updatedIdentityProviderConfig.get("postBindingAuthnRequest"), is("true"));
        assertThat(updatedIdentityProviderConfig.get("singleSignOnServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SSO"));
        assertThat(updatedIdentityProviderConfig.get("wantAuthnRequestsSigned"), is("false"));
        assertThat(updatedIdentityProviderConfig.get("addExtensionsElementWithKeyInfo"), is("false"));
        assertThat(updatedIdentityProviderConfig.get("encryptionPublicKey"), is("MIIDEjCCAfqgAwIBAgIVAPVbodo8Su7/BaHXUHykx0Pi5CFaMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQCQb+1a7uDdTTBBFfwOUun3IQ9nEuKM98SmJDWa\nMwM877elswKUTIBVh5gB2RIXAPZt7J/KGqypmgw9UNXFnoslpeZbA9fcAqqu28Z4\nsSb2YSajV1ZgEYPUKvXwQEmLWN6aDhkn8HnEZNrmeXihTFdyr7wjsLj0JpQ+VUlc\n4/J+hNuU7rGYZ1rKY8AA34qDVd4DiJ+DXW2PESfOu8lJSOteEaNtbmnvH8KlwkDs\n1NvPTsI0W/m4SK0UdXo6LLaV8saIpJfnkVC/FwpBolBrRC/Em64UlBsRZm2T89ca\nuzDee2yPUvbBd5kLErw+sC7i4xXa2rGmsQLYcBPhsRwnmBmlAgMBAAGjVzBVMB0G\nA1UdDgQWBBRZ3exEu6rCwRe5C7f5QrPcAKRPUjA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEABZDFRNtcbvIRmblnZItoWCFhVUlq81ceSQddLYs8DqK340//hWNAbYdj\nWcP85HhIZnrw6NGCO4bUipxZXhiqTA/A9d1BUll0vYB8qckYDEdPDduYCOYemKkD\ndmnHMQWs9Y6zWiYuNKEJ9mf3+1N8knN/PK0TYVjVjXAf2CnOETDbLtlj6Nqb8La3\nsQkYmU+aUdopbjd5JFFwbZRaj6KiHXHtnIRgu8sUXNPrgipUgZUOVhP0C0N5OfE4\nJW8ZBrKgQC/6vJ2rSa9TlzI6JAa5Ww7gMXMP9M+cJUNQklcq+SBnTK8G+uBHgPKR\nzBDsMIEzRtQZm4GIoHJae4zmnCekkQ=="));
        assertThat(updatedIdentityProviderConfig.get("principalType"), is("SUBJECT"));

        IdentityProviderRepresentation createdIdentityProvider = identityProviders.stream()
                .filter(e -> e.getAlias().equals("sam"))
                .findFirst()
                .orElse(null);

        assertThat(createdIdentityProvider, is(not(nullValue())));
        assertThat(createdIdentityProvider.getAlias(), is("sam"));
        assertThat(createdIdentityProvider.getProviderId(), is("saml"));
        assertThat(createdIdentityProvider.getDisplayName(), is(nullValue()));
        assertThat(createdIdentityProvider.isEnabled(), is(true));
        assertThat(createdIdentityProvider.isTrustEmail(), is(true));
        assertThat(createdIdentityProvider.isStoreToken(), is(false));
        assertThat(createdIdentityProvider.isAddReadTokenRoleOnCreate(), is(true));
        assertThat(createdIdentityProvider.isLinkOnly(), is(false));
        assertThat(createdIdentityProvider.getFirstBrokerLoginFlowAlias(), is("first broker login"));

        Map<String, String> createdIdentityProviderConfig = createdIdentityProvider.getConfig();

        assertThat(createdIdentityProviderConfig.get("validateSignature"), is("false"));
        assertThat(createdIdentityProviderConfig.get("samlXmlKeyNameTranformer"), is("KEY_ID"));
        assertThat(createdIdentityProviderConfig.get("signingCertificate"), is("MIIDETCCAfmgAwIBAgIUZRpDhkNKl5eWtJqk0Bu1BgTTargwDQYJKoZIhvcNAQEL\nBQAwFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwHhcNMTgwODI0MjExNDEwWhcNMzgw\nODI0MjExNDEwWjAWMRQwEgYDVQQDDAtzYW1sdGVzdC5pZDCCASIwDQYJKoZIhvcN\nAQEBBQADggEPADCCAQoCggEBAJrh9/PcDsiv3UeL8Iv9rf4WfLPxuOm9W6aCntEA\n8l6c1LQ1Zyrz+Xa/40ZgP29ENf3oKKbPCzDcc6zooHMji2fBmgXp6Li3fQUzu7yd\n+nIC2teejijVtrNLjn1WUTwmqjLtuzrKC/ePoZyIRjpoUxyEMJopAd4dJmAcCq/K\nk2eYX9GYRlqvIjLFoGNgy2R4dWwAKwljyh6pdnPUgyO/WjRDrqUBRFrLQJorR2kD\nc4seZUbmpZZfp4MjmWMDgyGM1ZnR0XvNLtYeWAyt0KkSvFoOMjZUeVK/4xR74F8e\n8ToPqLmZEg9ZUx+4z2KjVK00LpdRkH9Uxhh03RQ0FabHW6UCAwEAAaNXMFUwHQYD\nVR0OBBYEFJDbe6uSmYQScxpVJhmt7PsCG4IeMDQGA1UdEQQtMCuCC3NhbWx0ZXN0\nLmlkhhxodHRwczovL3NhbWx0ZXN0LmlkL3NhbWwvaWRwMA0GCSqGSIb3DQEBCwUA\nA4IBAQBNcF3zkw/g51q26uxgyuy4gQwnSr01Mhvix3Dj/Gak4tc4XwvxUdLQq+jC\ncxr2Pie96klWhY/v/JiHDU2FJo9/VWxmc/YOk83whvNd7mWaNMUsX3xGv6AlZtCO\nL3JhCpHjiN+kBcMgS5jrtGgV1Lz3/1zpGxykdvS0B4sPnFOcaCwHe2B9SOCWbDAN\nJXpTjz1DmJO4ImyWPJpN1xsYKtm67Pefxmn0ax0uE2uuzq25h0xbTkqIQgJzyoE/\nDPkBFK1vDkMfAW11dQ0BXatEnW7Gtkc0lh2/PIbHWj4AzxYMyBf5Gy6HSVOftwjC\nvoQR2qr2xJBixsg+MIORKtmKHLfU,MIIDEjCCAfqgAwIBAgIVAMECQ1tjghafm5OxWDh9hwZfxthWMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQC0Z4QX1NFKs71ufbQwoQoW7qkNAJRIANGA4iM0\nThYghul3pC+FwrGv37aTxWXfA1UG9njKbbDreiDAZKngCgyjxj0uJ4lArgkr4AOE\njj5zXA81uGHARfUBctvQcsZpBIxDOvUUImAl+3NqLgMGF2fktxMG7kX3GEVNc1kl\nbN3dfYsaw5dUrw25DheL9np7G/+28GwHPvLb4aptOiONbCaVvh9UMHEA9F7c0zfF\n/cL5fOpdVa54wTI0u12CsFKt78h6lEGG5jUs/qX9clZncJM7EFkN3imPPy+0HC8n\nspXiH/MZW8o2cqWRkrw3MzBZW3Ojk5nQj40V6NUbjb7kfejzAgMBAAGjVzBVMB0G\nA1UdDgQWBBQT6Y9J3Tw/hOGc8PNV7JEE4k2ZNTA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEASk3guKfTkVhEaIVvxEPNR2w3vWt3fwmwJCccW98XXLWgNbu3YaMb2RSn\n7Th4p3h+mfyk2don6au7Uyzc1Jd39RNv80TG5iQoxfCgphy1FYmmdaSfO8wvDtHT\nTNiLArAxOYtzfYbzb5QrNNH/gQEN8RJaEf/g/1GTw9x/103dSMK0RXtl+fRs2nbl\nD1JJKSQ3AdhxK/weP3aUPtLxVVJ9wMOQOfcy02l+hHMb6uAjsPOpOVKqi3M8XmcU\nZOpx4swtgGdeoSpeRyrtMvRwdcciNBp9UZome44qZAYH1iqrpmmjsfI9pJItsgWu\n3kXPjhSfj1AJGR1l9JGvJrHki1iHTA=="));
        assertThat(createdIdentityProviderConfig.get("postBindingLogout"), is("true"));
        assertThat(createdIdentityProviderConfig.get("nameIDPolicyFormat"), is("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(createdIdentityProviderConfig.get("postBindingResponse"), is("true"));
        assertThat(createdIdentityProviderConfig.get("singleLogoutServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SLO"));
        assertThat(createdIdentityProviderConfig.get("signatureAlgorithm"), is("RSA_SHA256"));
        assertThat(createdIdentityProviderConfig.get("useJwksUrl"), is("true"));
        assertThat(createdIdentityProviderConfig.get("postBindingAuthnRequest"), is("true"));
        assertThat(createdIdentityProviderConfig.get("singleSignOnServiceUrl"), is("https://samltest.id/idp/profile/SAML2/POST/SSO"));
        assertThat(createdIdentityProviderConfig.get("wantAuthnRequestsSigned"), is("false"));
        assertThat(createdIdentityProviderConfig.get("addExtensionsElementWithKeyInfo"), is("false"));
        assertThat(createdIdentityProviderConfig.get("encryptionPublicKey"), is("MIIDEjCCAfqgAwIBAgIVAPVbodo8Su7/BaHXUHykx0Pi5CFaMA0GCSqGSIb3DQEB\nCwUAMBYxFDASBgNVBAMMC3NhbWx0ZXN0LmlkMB4XDTE4MDgyNDIxMTQwOVoXDTM4\nMDgyNDIxMTQwOVowFjEUMBIGA1UEAwwLc2FtbHRlc3QuaWQwggEiMA0GCSqGSIb3\nDQEBAQUAA4IBDwAwggEKAoIBAQCQb+1a7uDdTTBBFfwOUun3IQ9nEuKM98SmJDWa\nMwM877elswKUTIBVh5gB2RIXAPZt7J/KGqypmgw9UNXFnoslpeZbA9fcAqqu28Z4\nsSb2YSajV1ZgEYPUKvXwQEmLWN6aDhkn8HnEZNrmeXihTFdyr7wjsLj0JpQ+VUlc\n4/J+hNuU7rGYZ1rKY8AA34qDVd4DiJ+DXW2PESfOu8lJSOteEaNtbmnvH8KlwkDs\n1NvPTsI0W/m4SK0UdXo6LLaV8saIpJfnkVC/FwpBolBrRC/Em64UlBsRZm2T89ca\nuzDee2yPUvbBd5kLErw+sC7i4xXa2rGmsQLYcBPhsRwnmBmlAgMBAAGjVzBVMB0G\nA1UdDgQWBBRZ3exEu6rCwRe5C7f5QrPcAKRPUjA0BgNVHREELTArggtzYW1sdGVz\ndC5pZIYcaHR0cHM6Ly9zYW1sdGVzdC5pZC9zYW1sL2lkcDANBgkqhkiG9w0BAQsF\nAAOCAQEABZDFRNtcbvIRmblnZItoWCFhVUlq81ceSQddLYs8DqK340//hWNAbYdj\nWcP85HhIZnrw6NGCO4bUipxZXhiqTA/A9d1BUll0vYB8qckYDEdPDduYCOYemKkD\ndmnHMQWs9Y6zWiYuNKEJ9mf3+1N8knN/PK0TYVjVjXAf2CnOETDbLtlj6Nqb8La3\nsQkYmU+aUdopbjd5JFFwbZRaj6KiHXHtnIRgu8sUXNPrgipUgZUOVhP0C0N5OfE4\nJW8ZBrKgQC/6vJ2rSa9TlzI6JAa5Ww7gMXMP9M+cJUNQklcq+SBnTK8G+uBHgPKR\nzBDsMIEzRtQZm4GIoHJae4zmnCekkQ=="));
        assertThat(createdIdentityProviderConfig.get("principalType"), is("SUBJECT"));
    }
}
