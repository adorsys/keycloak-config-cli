---
date: 2026-03-12
---

# keycloak-config-cli v6.5.0 Released

We're excited to announce the release of keycloak-config-cli v6.5.0 with significant security improvements, new features, and bug fixes!

## Security

- **Fix secrets leak in HTTP debug logs** - passwords, tokens, and credentials are now sanitized when `LOGGING_LEVEL_HTTP=debug` is enabled

## What's New

### Keycloak Organizations Support

Full support for importing and managing Keycloak Organizations, including identity providers and members.

### Keycloak Workflows Management

Add support for Keycloak Workflows management.

### JavaScript Variable Substitution

JavaScript variable substitution is now supported in configuration files:

```yaml
sessionTimeout: ${javascript: 2 * 60 * 60}
```

### Enhanced Remote State Management

- Track groups in remote-state to prevent deletion of groups created outside config-cli (e.g., via Keycloak UI)
- Add subGroups as managed import properties

### Token Exchange Configuration

Allow configuring Standard Token Exchange on clients via client attributes, including refresh token mode.

### User Import Improvements

- Add option to configure ignored user properties during user update (`--import.behaviors.user-update-ignored-properties`)
- Add optional merge mode for user realm roles and groups during import (`--import.users.merge-roles`, `--import.users.merge-groups`)
- Add support for `defaultValue` property in user profile attributes (Keycloak 26.4.0+)

### Performance Enhancements

- Enhance getting all Clients to remove Flow Override by using pagination by 100 to avoid timeout
- Increase code point limit to 500MB for import and normalization processes
- Avoid timeout from Keycloak when importing into realm with large amount of groups

## Bug Fixes

- Fix issue where FGAP returns 501 Not implemented for keycloak-26.2.0+
- Prevent unnecessary authentication flow recreation when only realm-level properties change
- Fix bug where `clientProfiles` and `clientPolicies` were erased when importing multiple realm configuration files
- Improve idempotency for OTP policy, state, and checksum updates
- Fix issue where empty or null composite realm roles were not being cleared during import
- Fix Keycloak client library compatibility with different server versions
- Fix authorization import order: create scopes before resources

## Upgrade Guide

To upgrade from v6.4.x:

1. Update your Docker image tag to `v6.5.0`
2. Review the [full changelog](https://github.com/adorsys/keycloak-config-cli/blob/main/CHANGELOG.md)
3. Test in a non-production environment first

## Download

Get the latest release from [GitHub Releases](https://github.com/adorsys/keycloak-config-cli/releases/tag/v6.5.0) or pull the Docker image:

```bash
docker pull adorsys/keycloak-config-cli:v6.5.0
```
