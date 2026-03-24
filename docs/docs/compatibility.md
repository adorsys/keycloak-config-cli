# Compatibility

keycloak-config-cli supports multiple Keycloak versions.

## Version Matrix

| Keycloak | CLI Version | Status |
|----------|-------------|--------|
| 26.5.x   | 6.5.x       | Latest |
| 26.x     | 6.4.x       | Supported |
| 25.x     | 6.3.x       | Supported |
| 24.x     | 6.2.x       | Supported |
| 23.x     | 6.1.x       | Supported |
| 22.x     | 6.0.x       | LTS |

## Red Hat SSO Compatibility

keycloak-config-cli is compatible with Red Hat SSO (RH-SSO), which is based on Keycloak.

| RH-SSO Version | Keycloak Base | CLI Version |
|----------------|---------------|-------------|
| 7.6 | 18.0.0 | 5.6.x |
| 7.5 | 17.0.0 | 5.5.x |

## Limitations

Some features may not be available in all Keycloak versions:

- Fine-grained admin permissions v2 requires Keycloak 22+
- User profile requires Keycloak 21+
- Organizations support requires Keycloak 26+
- WebAuthn Passwordless Policy support requires Keycloak 26.4+

## Getting Help

If you encounter compatibility issues, please [open an issue on GitHub](https://github.com/adorsys/keycloak-config-cli/issues).
