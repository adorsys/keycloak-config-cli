---
title: Keycloak Version Compatibility
description: Keycloak version compatibility matrix and support information
sidebar_position: 1
---

# Keycloak Version Compatibility

keycloak-config-cli maintains compatibility with various Keycloak versions. This document outlines the supported versions and any known limitations.

## Supported Keycloak Versions

### Current Support Matrix

| keycloak-config-cli Version | Keycloak Version Range | Status | Notes |
|-----------------------------|------------------------|---------|---------|
| 6.x.x                       | 22.x - 26.x            | ✅ Fully Supported | Latest versions |
| 5.x.x                       | 18.x - 24.x            | ✅ Supported | Maintenance mode  |
| 4.x.x                       | 15.x - 23.x            | ⚠️ Limited | Bug fixes only  |
| 3.x.x                       | 12.x - 21.x            | ❌ Deprecated | Upgrade recommended |
| 2.x.x                       | 9.x - 18.x             | ❌ Deprecated | Upgrade recommended|
| 1.x.x                       | 6.x - 15.x             | ❌ Deprecated | Upgrade recommended |

### Version Support Policy

- **Current Versions**: Latest 2 major versions receive full support
- **Maintenance Mode**: Previous major versions receive security fixes only
- **Deprecated**: Versions older than 2 major releases are deprecated

## Keycloak Release Compatibility

### Keycloak 26.5.5 (Latest)

**keycloak-config-cli version**: 6.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution
- ✅ Advanced authentication flows
- ✅ Fine-grained permissions
- ✅ WebAuthn Passwordless Policies

**Known Issues**:
- None reported

### Keycloak 23.x (Latest)

**keycloak-config-cli version**: 4.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution
- ✅ Advanced authentication flows
- ✅ Fine-grained permissions

**Known Issues**:
- None reported

### Keycloak 22.x

**keycloak-config-cli version**: 4.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution
- ✅ Advanced authentication flows

**Known Issues**:
- Minor UI import/export differences

### Keycloak 21.x

**keycloak-config-cli version**: 4.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- Some deprecated features may cause warnings

### Keycloak 20.x

**keycloak-config-cli version**: 4.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 19.x

**keycloak-config-cli version**: 4.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 18.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- Some newer features not available

### Keycloak 17.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 16.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 15.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 14.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 13.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

### Keycloak 12.x

**keycloak-config-cli version**: 3.x+

**Features**:
- ✅ All import/export functionality
- ✅ Variable substitution
- ✅ JavaScript substitution

**Known Issues**:
- None reported

## Legacy Versions

### Keycloak 11.x

**keycloak-config-cli version**: 2.x+

**Status**: Limited Support

**Features**:
- ✅ Basic import/export functionality
- ✅ Variable substitution
- ⚠️ Limited JavaScript substitution

**Known Issues**:
- Some newer features not supported
- Authentication flow differences

### Keycloak 10.x

**keycloak-config-cli version**: 2.x+

**Status**: Limited Support

**Features**:
- ✅ Basic import/export functionality
- ✅ Variable substitution
- ❌ JavaScript substitution not supported

**Known Issues**:
- Limited feature support
- API differences

### Keycloak 9.x

**keycloak-config-cli version**: 2.x+

**Status**: Limited Support

**Features**:
- ✅ Basic import/export functionality
- ✅ Variable substitution
- ❌ JavaScript substitution not supported

**Known Issues**:
- Limited feature support
- API compatibility issues

## Version Detection

keycloak-config-cli automatically detects the Keycloak version and adapts its behavior accordingly.

### Automatic Detection

```bash
# Version is logged at startup
java -jar keycloak-config-cli.jar --keycloak.url=https://your-keycloak.com
```

### Manual Version Specification

You can specify the expected Keycloak version:

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.version=21.1.1 \
  --keycloak.url=https://your-keycloak.com
```

## Migration Guide

### Upgrading Keycloak

When upgrading Keycloak versions:

1. **Backup Configuration**: Export current configuration
2. **Test Compatibility**: Test with new Keycloak version
3. **Update keycloak-config-cli**: Use compatible version
4. **Validate Import**: Test configuration import
5. **Deploy**: Deploy to production

### Upgrading keycloak-config-cli

When upgrading keycloak-config-cli:

1. **Check Compatibility**: Verify Keycloak version support
2. **Review Release Notes**: Check for breaking changes
3. **Test Configuration**: Validate with current setup
4. **Backup**: Keep previous version available
5. **Gradual Rollout**: Deploy incrementally

## Breaking Changes

### Major Version Changes

#### Keycloak 15.x → 16.x
- No breaking changes reported

#### Keycloak 14.x → 15.x
- Minor API changes in authentication flows
- New user profile features

#### Keycloak 13.x → 14.x
- Updated client scopes
- New authorization features

### keycloak-config-cli Breaking Changes

#### Version 4.x
- Requires Java 17+
- Updated configuration properties
- Enhanced security defaults

#### Version 3.x
- Added JavaScript substitution
- Improved error handling
- Updated import strategies

## Testing Compatibility

### Version Testing Script

```bash
#!/bin/bash
# test-compatibility.sh

KEYCLOAK_URL=$1
CONFIG_FILE=$2

echo "Testing compatibility with Keycloak at: $KEYCLOAK_URL"

# Test version detection
java -jar keycloak-config-cli.jar \
  --keycloak.url=$KEYCLOAK_URL \
  --import.dry-run=true \
  --import.files=$CONFIG_FILE \
  --logging.level.de.adorsys.keycloak.config=DEBUG
```

### Docker Testing

```bash
# Test with different Keycloak versions
docker run --rm \
  -e KEYCLOAK_URL=http://keycloak-15:8080 \
  -v ./config:/config \
  adorsys/keycloak-config-cli:latest

docker run --rm \
  -e KEYCLOAK_URL=http://keycloak-21:8080 \
  -v ./config:/config \
  adorsys/keycloak-config-cli:latest
```

## Known Limitations

### Feature Limitations by Version

| Feature | Keycloak 9.x | Keycloak 12.x | Keycloak 15.x | Keycloak 21.x |
|----------|----------------|----------------|----------------|----------------|
| Basic Import/Export | ✅ | ✅ | ✅ | ✅ |
| Variable Substitution | ✅ | ✅ | ✅ | ✅ |
| JavaScript Substitution | ❌ | ✅ | ✅ | ✅ |
| Fine-grained Permissions | ❌ | ⚠️ | ✅ | ✅ |
| User Profile Attributes | ❌ | ❌ | ✅ | ✅ |
| Advanced Auth Flows | ⚠️ | ✅ | ✅ | ✅ |

### Platform Limitations

- **Java Version**: Keycloak 15+ requires Java 17+
- **Memory Usage**: Larger configurations may require increased heap
- **Network**: Some corporate networks may block required endpoints

## Best Practices

### Version Management

1. **Pin Versions**: Use specific version tags in production
2. **Test Thoroughly**: Validate in staging environments
3. **Monitor**: Watch for compatibility issues
4. **Plan Upgrades**: Schedule regular upgrade cycles

### Configuration Management

1. **Version-Specific Configs**: Maintain separate configs per version
2. **Feature Detection**: Use feature detection when available
3. **Fallback Logic**: Implement graceful degradation
4. **Documentation**: Document version-specific workarounds

## Troubleshooting

### Common Issues

1. **Version Mismatch**: keycloak-config-cli version incompatible with Keycloak
   - **Solution**: Upgrade keycloak-config-cli or downgrade Keycloak

2. **Feature Not Available**: Feature used doesn't exist in Keycloak version
   - **Solution**: Check version compatibility or upgrade Keycloak

3. **API Changes**: Keycloak API changes break functionality
   - **Solution**: Update keycloak-config-cli or use workarounds

### Debug Information

Enable debug logging to troubleshoot compatibility issues:

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=https://your-keycloak.com \
  --logging.level.de.adorsys.keycloak.config=DEBUG \
  --import.files=config.json
```

## Next Steps

- [RH-SSO Support](../compatibility/rhso-support.md) - Red Hat SSO integration information
- [Limitations](../compatibility/limitations.md) - Known limitations and constraints
- [Configuration](../config/overview.md) - General configuration options
