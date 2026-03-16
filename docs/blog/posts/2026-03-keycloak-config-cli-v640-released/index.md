---
date: 2026-03-15
---

# keycloak-config-cli v6.4.0 Released

We're excited to announce the release of keycloak-config-cli v6.4.0!

## What's New

### Keycloak 26.x Support

Full compatibility with the latest Keycloak 26.x release, including support for:

- New authentication flows
- Enhanced user profile features
- Improved performance for large realm imports

### Sandboxed JavaScript Evaluation

JavaScript variable substitution now runs in a sandboxed environment for improved security. Dynamic expressions in your YAML files are safer than ever.

```yaml
sessionTimeout: ${javascript: 2 * 60 * 60}
```

## Upgrade Guide

To upgrade from v6.3.x:

1. Update your Docker image tag to `v6.4.0`
2. Review the [changelog](https://github.com/adorsys/keycloak-config-cli/releases/tag/v6.4.0)
3. Test in a non-production environment first

## Download

Get the latest release from [GitHub Releases](https://github.com/adorsys/keycloak-config-cli/releases/tag/v6.4.0) or pull the Docker image:

```bash
docker pull adorsys/keycloak-config-cli:v6.4.0
```
