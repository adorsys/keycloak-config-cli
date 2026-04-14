# Organization Identity Provider Integration

This section covers integrating identity providers with organizations, allowing organizations to have specific authentication methods and user sources.

## Overview

Organization identity provider (IDP) integration allows you to associate specific authentication methods with individual organizations. This is useful for multi-tenant applications where different organizations may use different authentication providers.

## Supported Identity Provider Types

Keycloak supports various identity provider types that can be associated with organizations:

- **SAML 2.0** - Enterprise SSO solutions
- **OpenID Connect** - Modern OAuth2/OIDC providers
- **Social Providers** - Google, GitHub, Facebook, etc.
- **LDAP/Active Directory** - Directory-based authentication

## Configuration Structure

### Basic IDP Association

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": ["saml-acme", "oidc-acme"]
    }
  ]
}
```

### Complete IDP Configuration

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": [
        "saml-enterprise",
        "oidc-google",
        "ldap-active-directory"
      ]
    }
  ]
}
```

## Identity Provider Properties

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `identityProviders` | Array | No | List of identity provider aliases |

## Setup Process

### Step 1: Configure Identity Providers

First, configure the identity providers in Keycloak:

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
      "domains": ["enterprise.company.com"],
      "attributes": {
        "auth-type": "enterprise-sso",
        "saml-required": "true"
      }
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
    },
    {
      "name": "Client C",
      "alias": "client-c",
      "identityProviders": ["ldap-client-c"],
      "domains": ["client-c.saas.com"]
    }
  ]
}
```

### Hybrid Authentication Setup

```json
{
  "organizations": [
    {
      "name": "Hybrid Corp",
      "alias": "hybrid-corp",
      "identityProviders": [
        "saml-enterprise",
        "oidc-google",
        "github-enterprise"
      ],
      "attributes": {
        "primary-auth": "saml",
        "backup-auth": "oidc",
        "developer-auth": "github"
      }
    }
  ]
}
```

## Advanced Configuration

### Conditional IDP Access

Use organization attributes to control IDP access:

```json
{
  "organizations": [
    {
      "name": "Premium Client",
      "alias": "premium-client",
      "identityProviders": ["saml-premium", "oidc-premium"],
      "attributes": {
        "tier": "premium",
        "saml-enabled": "true",
        "oidc-enabled": "true"
      }
    },
    {
      "name": "Basic Client",
      "alias": "basic-client",
      "identityProviders": ["oidc-basic"],
      "attributes": {
        "tier": "basic",
        "saml-enabled": "false",
        "oidc-enabled": "true"
      }
    }
  ]
}
```

### IDP Priority Configuration

```json
{
  "organizations": [
    {
      "name": "Priority Corp",
      "alias": "priority-corp",
      "identityProviders": ["saml-primary", "oidc-backup"],
      "attributes": {
        "idp-priority": "saml-first,oidc-second",
        "failover-enabled": "true"
      }
    }
  ]
}
```

## Import Behavior

### Adding IDP Associations

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": ["saml-acme"]  // New association
    }
  ]
}
```

### Updating IDP Associations

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": ["saml-acme", "oidc-google"]  // Added OIDC
    }
  ]
}
```

### Removing IDP Associations

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "identityProviders": ["oidc-google"]  // SAML removed
    }
  ]
}
```


## Best Practices

1. **IDP Naming**: Use consistent naming conventions for IDP aliases
2. **Testing**: Test IDP configurations before associating with organizations
3. **Documentation**: Document IDP requirements and configurations
4. **Monitoring**: Monitor IDP performance and availability
5. **Backup**: Have backup authentication methods available
6. **Security**: Regularly review IDP security configurations

## Security Considerations

### IDP Validation

- **Certificate Validation**: Ensure SAML certificates are valid
- **Client Secrets**: Securely store OAuth2 client secrets
- **Redirect URLs**: Validate redirect URL configurations
- **User Mapping**: Ensure proper user attribute mapping

### Access Control

```json
{
  "organizations": [
    {
      "name": "Secure Org",
      "alias": "secure-org",
      "identityProviders": ["saml-secure"],
      "attributes": {
        "auth-required": "true",
        "mfa-enabled": "true",
        "session-timeout": "3600"
      }
    }
  ]
}
```

## Related Topics

- [Configuration](configuration.md) - Organization setup
- [Member Management](member-management.md) - Managing organization users
- [Identity Providers](identity-providers.md) - Configuring IDPs
