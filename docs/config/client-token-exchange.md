# Client Token Exchange Configuration

Keycloak supports OAuth 2.0 Token Exchange (RFC 8693), which allows clients to exchange one token for another. This is essential for microservices architectures, delegation scenarios, and service-to-service authentication. Understanding how to configure token exchange on clients through keycloak-config-cli is crucial for implementing secure token-based communication patterns.

Related issues: [#1428](https://github.com/adorsys/keycloak-config-cli/pull/1428)

## The Problem

Users encounter difficulties configuring token exchange on clients because:
- Token exchange settings are not exposed in standard client configuration
- The `oauth2.token.exchange.grant.enabled` attribute must be explicitly set
- It's unclear how to enable token exchange for service accounts
- Token exchange requires specific client permissions and policies
- Different token exchange patterns have different configuration requirements
- No clear examples of token exchange client configuration
- Confusion between impersonation and delegation patterns

## Understanding Token Exchange

### What is Token Exchange?

Token exchange (RFC 8693) allows a client to exchange one security token for another. Common use cases:

1. **Service-to-Service Authentication**
   - Microservice A calls Microservice B
   - A exchanges user token for service-specific token

2. **Token Delegation**
   - Frontend gets user token
   - Passes to backend which exchanges for backend-specific token

3. **Impersonation**
   - Admin service exchanges admin token for user token
   - Performs actions on behalf of user

4. **Token Translation**
   - Exchange external token for Keycloak token
   - Integrate with external identity providers

---

## Configuration Overview

### Basic Token Exchange Client

To enable token exchange on a client:
```json
{
  "enabled": true,
  "realm": "realmWithClients",
  "clients": [
    {
      "clientId": "moped-client",
      "attributes": {
        "standard.token.exchange.enabled": "true",
        "standard.token.exchange.enableRefreshRequestedTokenType": "SAME_SESSION"
      }
    }
  ]
}

```
step1

![Token exchange enabled on client configuration](../static/images/token-exchange-images/token-exchange-client-config1.png)

step2

![Token exchange enabled on client configuration](../static/images/token-exchange-images/token-exchange-client-config2.png)

*Client configured with token exchange enabled. The `oauth2.token.exchange.grant.enabled` attribute is set to true in the client's attributes section.*

---

## Token Exchange Patterns

### Pattern 1: Service-to-Service Token Exchange

**Scenario:** Backend service exchanges user token for service-specific token.
```json
{
  "realm": "microservices",
  "enabled": true,
  "clients": [
    {
      "clientId": "user-service",
      "enabled": true,
      "publicClient": false,
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "secret": "user-service-secret",
      "attributes": {
        "oauth2.token.exchange.grant.enabled": "true"
      }
    },
    {
      "clientId": "order-service",
      "enabled": true,
      "publicClient": false,
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "secret": "order-service-secret",
      "attributes": {
        "oauth2.token.exchange.grant.enabled": "true"
      }
    }
  ]
}
```

**Use case:**
- User authenticates with frontend
- Frontend calls user-service with user token
- user-service exchanges token to call order-service
- order-service validates token and processes request

---

### Pattern 2: Impersonation

**Scenario:** Admin service performs actions on behalf of users.
```json
{
  "realm": "master",
  "clients": [
    {
      "clientId": "admin-service",
      "enabled": true,
      "publicClient": false,
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "secret": "admin-service-secret",
      "attributes": {
        "oauth2.token.exchange.grant.enabled": "true"
      }
    }
  ],
  "users": [
    {
      "username": "service-account-admin-service",
      "enabled": true,
      "serviceAccountClientId": "admin-service",
      "realmRoles": ["admin"]
    }
  ]
}
```

**Required permissions:**
- Service account needs `impersonation` role
- Client needs permission to exchange tokens for target users

---

### Pattern 3: Token Delegation

**Scenario:** Frontend delegates token to backend for API calls.
```json
{
  "realm": "webapp",
  "clients": [
    {
      "clientId": "frontend-app",
      "enabled": true,
      "publicClient": true,
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": false
    },
    {
      "clientId": "backend-api",
      "enabled": true,
      "publicClient": false,
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "secret": "backend-api-secret",
      "attributes": {
        "oauth2.token.exchange.grant.enabled": "true"
      },
      "authorizationServicesEnabled": true
    }
  ]
}
```

---

## Complete Configuration Examples

### Example 1: Microservices Architecture
```json
{
  "enabled": true,
  "realm": "microservices",
  "clients": [
    {
      "clientId": "api-gateway",
      "attributes": {
        "standard.token.exchange.enabled": "true"
      }
    },
    {
      "clientId": "user-service",
      "attributes": {
        "standard.token.exchange.enabled": "true"
      }
    },
    {
      "clientId": "order-service",
      "attributes": {
        "standard.token.exchange.enabled": "true"
      }
    },
    {
      "clientId": "payment-service",
      "attributes": {
        "standard.token.exchange.enabled": "true"
      }
    }
  ]
}
```
step1

![Token exchange flow between microservices](../static/images/token-exchange-images/token-exchange-flow1.png)

step2

![Token exchange flow between microservices](../static/images/token-exchange-images/token-exchange-flow2.png)

*Token exchange workflow: API Gateway exchanges user token to call downstream microservices. Each service validates the exchanged token before processing requests.*

---

### Example 2: Admin Impersonation
```json
{
  "realm": "corporate",
  "enabled": true,
  "clients": [
    {
      "clientId": "admin-portal",
      "enabled": true,
      "publicClient": false,
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": false,
      "secret": "${ADMIN_PORTAL_SECRET}",
      "attributes": {
        "oauth2.token.exchange.grant.enabled": "true"
      },
      "authorizationServicesEnabled": true
    }
  ],
  "roles": {
    "realm": [
      {
        "name": "admin",
        "description": "Administrator role"
      },
      {
        "name": "user",
        "description": "Standard user"
      }
    ]
  },
  "users": [
    {
      "username": "service-account-admin-portal",
      "enabled": true,
      "serviceAccountClientId": "admin-portal",
      "realmRoles": ["admin"]
    }
  ]
}
```

---

### Example 3: External Token Exchange
```json
{
  "realm": "integration",
  "enabled": true,
  "clients": [
    {
      "clientId": "external-integration",
      "enabled": true,
      "publicClient": false,
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "secret": "${EXTERNAL_INTEGRATION_SECRET}",
      "attributes": {
        "oauth2.token.exchange.grant.enabled": "true"
      }
    }
  ],
  "identityProviders": [
    {
      "alias": "external-idp",
      "providerId": "oidc",
      "enabled": true,
      "config": {
        "clientId": "external-client-id",
        "clientSecret": "${EXTERNAL_IDP_SECRET}",
        "authorizationUrl": "https://external-idp.com/auth",
        "tokenUrl": "https://external-idp.com/token"
      }
    }
  ]
}
```

---

## Token Exchange Request Examples

### Exchange User Token for Service Token
```bash
# Get user token first
USER_TOKEN=$(curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=frontend" \
  -d "client_secret=frontend-secret" | jq -r '.access_token')

# Exchange token
curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
  -d "client_id=backend-service" \
  -d "client_secret=backend-secret" \
  -d "subject_token=${USER_TOKEN}" \
  -d "requested_token_type=urn:ietf:params:oauth:token-type:access_token"
```

---

### Impersonation Request
```bash
# Admin exchanges their token for user token
ADMIN_TOKEN=$(curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=admin-service" \
  -d "client_secret=admin-secret" | jq -r '.access_token')

# Exchange for user token
curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
  -d "client_id=admin-service" \
  -d "client_secret=admin-secret" \
  -d "subject_token=${ADMIN_TOKEN}" \
  -d "requested_subject=target-user" \
  -d "requested_token_type=urn:ietf:params:oauth:token-type:access_token"
```

---

## Client Configuration Options

### Required Attributes
```json
{
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

### Optional Attributes
```json
{
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true",
    "access.token.lifespan": "300",
    "client.session.idle.timeout": "1800",
    "client.session.max.lifespan": "36000"
  }
}
```

### Service Account Configuration
```json
{
  "clientId": "service-client",
  "serviceAccountsEnabled": true,
  "standardFlowEnabled": false,
  "directAccessGrantsEnabled": false,
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

---

## Permission Configuration

### Grant Token Exchange Permissions

For token exchange to work, proper permissions must be configured:

**Via Admin Console:**
1. Clients → Select source client
2. Permissions tab → Enable permissions
3. Token Exchange → Add target clients

**Via Configuration (using authorization services):**
```json
{
  "clients": [
    {
      "clientId": "source-client",
      "authorizationServicesEnabled": true,
      "authorizationSettings": {
        "policyEnforcementMode": "ENFORCING",
        "resources": [
          {
            "name": "Token Exchange Permission",
            "type": "urn:source-client:resources:token-exchange"
          }
        ],
        "policies": [
          {
            "name": "Allow Token Exchange",
            "type": "client",
            "logic": "POSITIVE",
            "config": {
              "clients": "[\"target-client\"]"
            }
          }
        ]
      }
    }
  ]
}
```

---

## Common Pitfalls

### 1. Token Exchange Not Enabled

**Problem:**
```json
{
  "clientId": "my-service",
  "serviceAccountsEnabled": true
}
```

**Error:**
```
400 Bad Request: Grant type not supported
```

**Solution:**
```json
{
  "clientId": "my-service",
  "serviceAccountsEnabled": true,
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

---

### 2. Missing Service Account

**Problem:**
```json
{
  "clientId": "my-service",
  "publicClient": false,
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

**Error:**
```
401 Unauthorized: Invalid client credentials
```

**Solution:**
```json
{
  "clientId": "my-service",
  "publicClient": false,
  "serviceAccountsEnabled": true,
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

---

### 3. Insufficient Permissions

**Problem:** Token exchange fails even with correct configuration

**Error:**
```
403 Forbidden: Client not allowed to exchange token
```

**Solution:** Grant token exchange permissions via Admin Console or authorization services.

---

### 4. Wrong Grant Type in Request

**Problem:**
```bash
curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -d "grant_type=client_credentials" \
  -d "subject_token=${TOKEN}"
```

**Solution:**
```bash
curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
  -d "subject_token=${TOKEN}"
```

---

### 5. Public Client for Token Exchange

**Problem:**
```json
{
  "clientId": "my-service",
  "publicClient": true,
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

**Result:** Token exchange requires confidential clients with secrets.

**Solution:**
```json
{
  "clientId": "my-service",
  "publicClient": false,
  "secret": "${CLIENT_SECRET}",
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

---

## Best Practices

1. **Use Confidential Clients**
```json
{
  "publicClient": false,
  "serviceAccountsEnabled": true
}
```

2. **Disable Unnecessary Flows**
```json
{
  "standardFlowEnabled": false,
  "directAccessGrantsEnabled": false,
  "implicitFlowEnabled": false
}
```

3. **Use Environment Variables for Secrets**
```json
{
  "secret": "${CLIENT_SECRET}"
}
```

4. **Configure Token Lifespans**
```json
{
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true",
    "access.token.lifespan": "300"
  }
}
```

5. **Enable Only for Required Clients**

Don't enable token exchange on all clients; only enable it where needed.

6. **Use Authorization Services for Fine-Grained Control**
```json
{
  "authorizationServicesEnabled": true
}
```

7. **Document Token Exchange Flows**

Maintain clear documentation of which clients can exchange tokens with which targets.

8. **Monitor Token Exchange Usage**

Enable event logging to track token exchange operations.

---

## Troubleshooting

### Token Exchange Fails with 400 Error

**Symptom:**
```
400 Bad Request: Grant type not supported
```

**Diagnosis:**

Check if token exchange is enabled:
```bash
# In Admin Console
# Clients → Select client → Settings → Advanced Settings
# Look for "OAuth 2.0 Token Exchange Grant Enabled"
```

**Solution:**
```json
{
  "attributes": {
    "oauth2.token.exchange.grant.enabled": "true"
  }
}
```

---

### Token Exchange Fails with 403 Error

**Symptom:**
```
403 Forbidden: Client not allowed to exchange token
```

**Diagnosis:**

Check permissions in Admin Console:
- Clients → Source client → Permissions tab
- Token Exchange → Verify target clients are allowed

**Solution:** Grant appropriate token exchange permissions.

---

### Service Account Not Created

**Symptom:** Client created but no service account user exists

**Diagnosis:**
```bash
# Check if service accounts are enabled
curl "http://localhost:8080/admin/realms/master/users" \
  -H "Authorization: Bearer $TOKEN" | \
  jq '.[] | select(.username | startswith("service-account"))'
```

**Solution:**
```json
{
  "serviceAccountsEnabled": true
}
```

---

### Invalid Token Type Error

**Symptom:**
```
400 Bad Request: Invalid requested_token_type
```

**Solution:** Use correct token type:
```bash
-d "requested_token_type=urn:ietf:params:oauth:token-type:access_token"
```

Or:
```bash
-d "requested_token_type=urn:ietf:params:oauth:token-type:refresh_token"
```

---

## Security Considerations

1. **Token Exchange Permissions**
   - Carefully control which clients can exchange tokens
   - Use least privilege principle

2. **Token Lifespans**
   - Configure short-lived tokens for exchanged tokens
   - Use refresh tokens where appropriate

3. **Audit Logging**
   - Enable event logging for token exchange
   - Monitor for unusual patterns

4. **Network Security**
   - Use TLS for all token exchange requests
   - Implement network segmentation

5. **Secret Management**
   - Store client secrets securely
   - Rotate secrets regularly
   - Use external secret management systems

---

## Configuration Options
```bash java -jar /home/dzeco/Downloads/keycloak-config-cli-18.0.2.jar \                                                                                                             ──(Tue,Mar10)─┘
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=token-exchange-client.json
Standard Commons Logging discovery in action with spring-jcl: please remove commons-logging.jar from classpath in order to avoid potential conflicts
2026-03-10T13:53:02.754+01:00  INFO 194173 --- [           main] d.a.k.config.KeycloakConfigApplication   : Starting KeycloakConfigApplication v6.4.1 using Java 21.0.10 with PID 194173 (/home/dzeco/Downloads/keycloak-config-cli-18.0.2.jar started by dzeco in /home/dzeco)
2026-03-10T13:53:02.773+01:00  INFO 194173 --- [           main] d.a.k.config.KeycloakConfigApplication   : No active profile set, falling back to 1 default profile: "default"
2026-03-10T13:53:06.045+01:00  INFO 194173 --- [           main] d.a.k.config.KeycloakConfigApplication   : Started KeycloakConfigApplication in 5.803 seconds (process running for 8.192)
2026-03-10T13:53:08.570+01:00  INFO 194173 --- [           main] d.a.k.config.KeycloakConfigRunner        : Importing file 'file:token-exchange-client.json'
2026-03-10T13:53:09.856+01:00 ERROR 194173 --- [           main] d.a.k.config.KeycloakConfigRunner        : Error during Keycloak import: RESTEASY004655: Unable to invoke request: org.apache.http.conn.HttpHostConnectException: Connect to localhost:8080 [localhost/127.0.0.1] failed: Connection refused
: "token-exchange-demo",

# Enable variable substitution for secrets
--import.var-substitution.enabled=true

# Validate configuration before import
--import.validate=true

# Enable remote state tracking
--import.remote-state.enabled=true
```

---

## Consequences

When enabling token exchange on clients:

1. **Security Implications:** Clients can exchange tokens, increasing attack surface
2. **Permission Management:** Requires careful permission configuration
3. **Complexity:** Adds complexity to authentication flows
4. **Token Lifespans:** Exchanged tokens may have different lifespans
5. **Auditing Required:** Token exchange should be logged and monitored
6. **Service Account Creation:** Automatically creates service account users

---
