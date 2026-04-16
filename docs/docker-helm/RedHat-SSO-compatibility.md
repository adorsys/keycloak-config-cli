# Red Hat SSO Compatibility

This section covers compatibility considerations when using keycloak-config-cli with Red Hat Single Sign-On (SSO) systems.

## Overview

Red Hat SSO provides enterprise-grade identity and access management built on Keycloak. keycloak-config-cli is compatible with Red Hat SSO deployments, though some considerations apply.

## Supported Features

### Core Functionality

- **Realm Management**: Full support for Red Hat SSO realms
- **User Management**: Complete user lifecycle management
- **Group Management**: Full group creation and management
- **Role Management**: Role-based access control
- **Client Management**: OAuth2/OIDC client configuration
- **Authentication Flows**: Support for Red Hat SSO authentication methods

### Red Hat SSO Specific Features

- **Enterprise Authentication**: Integration with Red Hat enterprise auth systems
- **Federation Support**: LDAP and Active Directory integration
- **Multi-Tenant Support**: Organization and tenant management
- **Advanced Security**: Enhanced security policies and controls
- **Monitoring Integration**: Compatibility with Red Hat monitoring tools

## Version Compatibility

### Red Hat SSO 7.6+

- **Full API Support**: Complete Keycloak API compatibility
- **Organization Features**: Full organization management support
- **Advanced Features**: All keycloak-config-cli features available
- **Performance**: Optimized for enterprise deployments

### Red Hat SSO 7.4-7.5

- **Core Features**: Most functionality supported
- **Limited Organizations**: Organization features may be restricted
- **API Compatibility**: Standard Keycloak API endpoints
- **Some Limitations**: Advanced features may not be available

### Legacy Versions (< 7.4)

- **Basic Management**: User, group, and role management
- **No Organizations**: Organization features not available
- **Limited API**: Some advanced features restricted
- **Manual Configuration**: Additional setup may be required

## Configuration Considerations

### Authentication Configuration

```json
{
  "realm": "rhsso-realm",
  "users": [
    {
      "username": "admin",
      "email": "admin@company.com",
      "enabled": true,
      "credentials": [{
        "type": "password",
        "value": "${ADMIN_PASSWORD}",
        "temporary": false
      }]
    }
  ]
}
```

### Red Hat SSO Integration

```bash
# Configure for Red Hat SSO endpoint
java -jar keycloak-config-cli.jar \
  --keycloak.url=https://rhsso.company.com/auth \
  --keycloak.realm=rhsso-realm \
  --keycloak.client=admin-cli \
  --keycloak.secret="${RHSSO_CLIENT_SECRET}" \
  --import.files=config.json
```

### Environment Variables

```bash
export KEYCLOAK_URL=https://rhsso.company.com/auth
export KEYCLOAK_REALM=rhsso-realm
export KEYCLOAK_CLIENT=admin-cli
export KEYCLOAK_SECRET="${RHSSO_CLIENT_SECRET}"

java -jar keycloak-config-cli.jar \
  --import.files=config.json
```

## Deployment Considerations

### Docker with Red Hat SSO

```dockerfile
FROM openjdk:11-jre

# Red Hat SSO specific configuration
ENV KEYCLOAK_URL=https://rhsso.company.com/auth
ENV KEYCLOAK_REALM=rhsso-realm
ENV KEYCLOAK_CLIENT=admin-cli

COPY keycloak-config-cli.jar /app/
COPY config.json /app/config/

CMD ["java", "-jar", "/app/keycloak-config-cli.jar", "--import.files=/app/config.json"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak-config-cli
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak-config-cli
  template:
    metadata:
      labels:
        app: keycloak-config-cli
    spec:
      containers:
      - name: keycloak-config-cli
        image: keycloak-config-cli:latest
        env:
          - name: KEYCLOAK_URL
            value: "https://rhsso.company.com/auth"
          - name: KEYCLOAK_REALM
            value: "rhsso-realm"
          - name: KEYCLOAK_CLIENT
            value: "admin-cli"
          - name: KEYCLOAK_SECRET
            valueFrom:
              secretKeyRef:
                name: rhsso-secrets
                key: client-secret
        command: ["java", "-jar", "/app/keycloak-config-cli.jar", "--import.files=/app/config.json"]
```

## Security Considerations

### Red Hat SSO Security Policies

- **Certificate Management**: Use Red Hat SSO certificates
- **Access Controls**: Respect Red Hat SSO access policies
- **Audit Requirements**: Enable audit logging for compliance
- **Data Protection**: Follow Red Hat data protection guidelines

### Authentication Methods

- **Kerberos Integration**: Support for Kerberos authentication
- **Certificate-based Auth**: X.509 certificate authentication
- **Multi-factor Authentication**: Integration with Red Hat MFA systems
- **SSO Integration**: Cross-domain single sign-on

## Performance Optimization

### Red Hat SSO Specific Optimizations

```bash
# Connection pooling for Red Hat SSO
java -jar keycloak-config-cli.jar \
  --keycloak.connection.pool.size=10 \
  --keycloak.connection.pool.timeout=30000 \
  --import.files=config.json

# Batch processing optimization
java -jar keycloak-config-cli.jar \
  --import.batch.size=50 \
  --import.parallel.enabled=true \
  --import.parallel.threads=4
```

### Caching Strategy

```bash
# Enable caching for Red Hat SSO
java -jar keycloak-config-cli.jar \
  --import.cache.enabled=true \
  --import.cache.ttl=1800 \
  --import.files=config.json
```

## Troubleshooting

### Common Issues

#### Connection Issues

**Error:** `Connection refused: Red Hat SSO unreachable`

**Solutions:**
- Check Red Hat SSO service status
- Verify network connectivity
- Confirm firewall rules
- Validate SSL certificates

#### Authentication Failures

**Error:** `Authentication failed: Invalid credentials for Red Hat SSO`

**Solutions:**
- Verify client credentials in Red Hat SSO admin console
- Check client secret expiration
- Confirm client has proper permissions
- Validate realm configuration

#### Permission Issues

**Error:** `Access denied: Insufficient permissions for realm management`

**Solutions:**
- Check user permissions in Red Hat SSO
- Verify role assignments
- Confirm client has management rights
- Check Red Hat SSO policies

### Debug Mode

```bash
# Enable debug for Red Hat SSO
java -jar keycloak-config-cli.jar \
  --keycloak.url=https://rhsso.company.com/auth \
  --logging.level.redhat=DEBUG \
  --import.files=config.json
```

## Best Practices

### Red Hat SSO Deployment

1. **Use Environment Variables**: Store sensitive data in environment variables
2. **Test in Staging**: Validate configurations in development/staging
3. **Monitor Performance**: Track import times and resource usage
4. **Backup Regularly**: Create realm backups before major changes
5. **Document Configuration**: Maintain clear configuration records
6. **Use Service Accounts**: Prefer service accounts over user accounts

### Integration Guidelines

1. **Follow Red Hat Standards**: Adhere to Red Hat SSO configuration guidelines
2. **Use Supported Features**: Leverage Red Hat SSO-specific capabilities
3. **Plan for Scale**: Design configurations for enterprise growth
4. **Security First**: Prioritize security in all configurations
5. **Monitor Compliance**: Ensure ongoing compliance with Red Hat policies

## Version Specific Notes

### Red Hat SSO 7.6+

- **Full Feature Support**: All keycloak-config-cli features available
- **Organization Management**: Complete organization support
- **Advanced Import**: Full import strategy support
- **Performance Optimized**: Enhanced performance for enterprise

### Red Hat SSO 7.5

- **Enhanced Security**: Additional security features
- **Improved API**: Better API performance
- **Organization Support**: Organization features available
- **Migration Support**: Tools for migrating from older versions

## Related Topics

- [Configuration](../config/overview.md) - General configuration options
- [Docker & Helm](../docker-helm/overview.md) - Container deployment options
- [Variable Substitution](../variable-substitution/overview.md) - Dynamic configuration
- [Organizations](../organizations/overview.md) - Organization management
