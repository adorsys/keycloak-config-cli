# Identity Provider Integration

This section covers integrating identity providers with organizations, allowing organizations to have specific authentication methods and user sources.

## Overview

Organization identity provider (IDP) integration allows you to associate specific authentication methods with individual organizations. This enables multi-tenant applications where different organizations can use different authentication providers.

## Supported Identity Provider Types

Keycloak supports various identity provider types that can be associated with organizations:

- **SAML 2.0** - Enterprise SSO solutions
- **OpenID Connect** - Modern OAuth2/OIDC providers
- **Social Providers** - Google, GitHub, Facebook, etc.
- **LDAP/Active Directory** - Directory-based authentication

## Basic Configuration

### IDP Association

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": ["saml-acme", "oidc-google"]
    }
  ]
}
```

### Multiple IDPs

```json
{
  "organizations": [
    {
      "name": "Flexible Corp",
      "alias": "flexible-corp",
      "identityProviders": [
        "saml-enterprise",
        "oidc-google",
        "github-social"
      ]
    }
  ]
}
```

## IDP Properties

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `identityProviders` | Array | No | List of identity provider aliases |

## Setup Process

### Step 1: Configure Identity Providers

First, configure identity providers in Keycloak:

```json
{
  "identityProviders": [
    {
      "alias": "saml-acme",
      "displayName": "Acme SAML",
      "providerId": "saml",
      "config": {
        "validateSignature": "true",
        "singleSignOnServiceUrl": "https://sso.acme.com/saml",
        "nameIDPolicyFormat": "urn:oasis:names:tc:SAML:2.0:nameid-format:emailAddress"
      }
    },
    {
      "alias": "oidc-google",
      "displayName": "Google Workspace",
      "providerId": "oidc",
      "config": {
        "clientId": "your-google-client-id",
        "clientSecret": "your-google-secret",
        "authorizationUrl": "https://accounts.google.com/o/oauth2/auth",
        "tokenUrl": "https://oauth2.googleapis.com/token",
        "userInfoUrl": "https://www.googleapis.com/oauth2/v2/userinfo"
      }
    }
  ]
}
```

### Step 2: Associate with Organizations

Then associate the configured IDPs with organizations:

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": ["saml-acme", "oidc-google"]
    },
    {
      "name": "Tech Startup",
      "alias": "tech-startup",
      "identityProviders": ["oidc-github", "oidc-google"]
    }
  ]
}
```

## IDP Integration Scenarios

### Enterprise SSO Integration

```json
{
  "organizations": [
    {
      "name": "Enterprise Division",
      "alias": "enterprise-div",
      "identityProviders": ["saml-enterprise", "ldap-ad"],
      "domains": ["enterprise.company.com"]
    }
  ]
}
```

### Multi-Tenant SaaS Setup

```json
{
  "organizations": [
    {
      "name": "Client A",
      "alias": "client-a",
      "identityProviders": ["saml-client-a"],
      "domains": ["client-a.saas.com"]
    },
    {
      "name": "Client B",
      "alias": "client-b", 
      "identityProviders": ["oidc-client-b"],
      "domains": ["client-b.saas.com"]
    }
  ]
}
```

### Social Provider Integration

```json
{
  "organizations": [
    {
      "name": "Startup Company",
      "alias": "startup-co",
      "identityProviders": ["google", "github"],
      "domains": ["startup.com"]
    }
  ]
}
```

### LDAP/AD Integration

```json
{
  "organizations": [
    {
      "name": "Corporate Division",
      "alias": "corporate-div",
      "identityProviders": ["ldap-corporate"],
      "domains": ["corp.company.com"]
    }
  ]
}
```

## Best Practices

### Planning

1. **IDP Inventory**: Catalog all required identity providers
2. **Organization Mapping**: Plan which organizations need which IDPs
3. **Domain Strategy**: Align email domains with IDP choices
4. **Fallback Planning**: Design backup authentication methods

### Implementation

1. **Configure IDPs First**: Set up identity providers before organizations
2. **Test Separately**: Verify each IDP works independently
3. **Associate Gradually**: Link IDPs to organizations incrementally
4. **Validate Access**: Test authentication flows for each organization

### Security

1. **Separate Credentials**: Use unique credentials per organization IDP
2. **Certificate Management**: Properly configure SAML certificates
3. **Access Controls**: Limit IDP access to authorized organizations
4. **Audit Logging**: Enable comprehensive IDP access logging

## Related Topics

- [Configuration](configuration.md) - Organization setup
- [Member Management](member-management.md) - Managing organization users
- [Examples](examples.md) - Complete scenarios with examples
