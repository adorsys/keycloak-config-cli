# Organizations Feature

Keycloak organizations allow you to manage multi-tenant scenarios by grouping users, identity providers, and domains under organizational units. This feature is available in Keycloak 26+ and requires the `organization` feature to be enabled.

## Prerequisites

### Keycloak Version
- **Minimum Keycloak version**: 26.0.0
- **Feature flag**: `organization` must be enabled

### Enable Organizations in Keycloak

#### Option 1: Docker/Container
```bash
docker run -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin123 \
  quay.io/keycloak/keycloak:26.5.2 \
  start-dev --features=organization
```

#### Option 2: Docker Compose
```yaml
version: '3'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.5.2
    ports:
      - "8080:8080"
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin123
    command: start-dev --features=organization
```

## Supported Features

| Feature | Description | Since |
|---------|-------------|-------|
| Create organizations | Create new organizations with name, alias, and attributes | 6.4.1 |
| Update organizations | Modify organization properties and attributes | 6.4.1 |
| Delete organizations | Remove organizations from realm | 6.4.1 |
| Manage domains | Add/remove verified domains to organizations | 6.4.1 |
| Link identity providers | Associate realm IdPs with organizations | 6.4.1 |
| Manage members | Add/remove users as organization members | 6.4.1 |

## Configuration Format

### Basic Organization Structure

```json
{
  "realm": "org-feature-test",
  "organizationsEnabled": true,
  "organizations": [
    {
      "name": "Acme Corporation",
      "alias": "acme",
      "redirectUrl": "https://acme.com/redirect",
      "description": "Main organization for Acme Corporation",
      "domains": [
        {
          "name": "acme.com",
          "verified": false
        },
        {
          "name": "acme.org", 
          "verified": true
        }
      ],
      "attributes": {
        "industry": ["Technology"],
        "location": ["San Francisco"],
        "employeeCount": ["1000+"]
      },
      "members": [
        {
          "username": "myuser"
        },
        {
          "username": "myclientuser"
        }
      ],
      "enabled": true
    },
    {
      "name": "Tech Startup",
      "alias": "tech-startup",
      "redirectUrl": "https://tech-startup.io/redirect",
      "description": "Innovative tech startup",
      "domains": [
        {
          "name": "tech-startup.io",
          "verified": false
        }
      ],
      "attributes": {
        "industry": ["Software"],
        "stage": ["Series A"],
        "funding": ["$5M"]
      },
      "enabled": true,
      "members": [
        {
          "username": "ceo@tech-startup.io"
        },
        {
          "username": "cto@tech-startup.io"
        }
      ]
    }
  ],
  "users": [
    {
      "username": "ceo@tech-startup.io",
      "email": "ceo@tech-startup.io",
      "enabled": true,
      "firstName": "CEO",
      "lastName": "TechStartup"
    },
    {
      "username": "cto@tech-startup.io",
      "email": "cto@tech-startup.io",
      "enabled": true,
      "firstName": "CTO",
      "lastName": "TechStartup"
    },
    {
      "username": "myuser",
      "email": "myuser@mail.de",
      "enabled": true,
      "firstName": "My firstname",
      "lastName": "My lastname",
      "attributes": {
        "locale": [
          "de"
        ]
      }
    },
    {
      "username": "myclientuser",
      "email": "myclientuser@mail.de",
      "enabled": true,
      "firstName": "My clientuser's firstname",
      "lastName": "My clientuser's lastname",
      "credentials": [
        {
          "type": "password",
          "value": "myclientuser123"
        }
      ]
    }
  ]
}
```

### Complete Example with Realm-Level Identity Providers

```json
{
  "realm": "org-feature-test",
  "enabled": true,
  "identityProviders": [
    {
      "alias": "google",
      "providerId": "google",
      "enabled": true,
      "config": {
        "clientId": "your-google-client-id",
        "clientSecret": "your-google-client-secret",
        "hostedDomain": "acme.com"
      }
    },
    {
      "alias": "github",
      "providerId": "github",
      "enabled": true,
      "config": {
        "clientId": "your-github-client-id",
        "clientSecret": "your-github-client-secret"
      }
    }
  ],
  "organizations": [
    {
      "name": "Acme Corporation",
      "alias": "acme",
      "enabled": true,
      "domains": [
        {
          "name": "acme.com",
          "verified": true
        }
      ],
      "identityProviders": [
        {
          "alias": "google"
        }
      ],
      "members": [
        {
          "username": "myuser"
        },
        {
          "username": "myclientuser"
        }
      ]
    },
    {
      "name": "Tech Startup",
      "alias": "tech-startup",
      "enabled": true,
      "domains": [
        {
          "name": "tech-startup.io",
          "verified": true
        }
      ],
      "identityProviders": [
        {
          "alias": "github"
        }
      ],
      "members": [
        {
          "username": "ceo@tech-startup.io"
        },
        {
          "username": "cto@tech-startup.io"
        }
      ]
    }
  ],
  "users": [
    {
      "username": "myuser",
      "email": "myuser@acme.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "password123"
        }
      ]
    },
    {
      "username": "myclientuser",
      "email": "client@acme.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "password123"
        }
      ]
    },
    {
      "username": "ceo@tech-startup.io",
      "email": "ceo@tech-startup.io",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "password123"
        }
      ]
    },
    {
      "username": "cto@tech-startup.io",
      "email": "cto@tech-startup.io",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "password123"
        }
      ]
    }
  ]
}
```

## Best Practices

### 1. Identity Provider Order
- Define identity providers at the realm level before organizations
- The CLI automatically handles the correct import order

### 2. User Management
- Create users before adding them as organization members
- Use consistent usernames across the configuration

### 3. Domain Verification
- Set `verified: true` only for domains you actually own
- Use domain verification for security in production

### 4. Idempotency
- The configuration is idempotent - running it multiple times produces the same result
- Use this for CI/CD pipelines and automated deployments