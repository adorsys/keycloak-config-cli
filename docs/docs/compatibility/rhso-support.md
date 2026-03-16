---
title: Red Hat SSO Support
description: Compatibility and setup instructions for Red Hat SSO
sidebar_position: 2
---

# Red Hat SSO Support

keycloak-config-cli is generally compatible with Red Hat SSO, which is based on Keycloak. However, there are some considerations and specific steps required for proper compatibility.

## General Compatibility

In general, Red Hat SSO is based on Keycloak, so keycloak-config-cli works with Red Hat SSO. However, some specific RH SSO versions may differ from Keycloak releases, which can introduce incompatibilities.

While keycloak-config-cli doesn't officially support RH SSO out-of-the-box, it's possible to build keycloak-config-cli against RH SSO.

## Requirements

### System Requirements

- Java JDK 8+
- Maven 3.6+
- Git

## Setup Instructions

### 1. Clone Repository

```bash
git clone https://github.com/adorsys/keycloak-config-cli.git
git checkout v3.4.0
```

### 2. Patch pom.xml

**Note:** This step can be skipped if keycloak-config-cli version 4.4.0 or higher is used.

Enrich the `pom.xml` with changes from [PR #583](https://github.com/adorsys/keycloak-config-cli/pull/583).

### 3. Identify Correct Keycloak Version

Red Hat SSO uses specific version identifiers that differ from upstream Keycloak releases.

1. Check the Red Hat SSO version you're using
2. Look up the corresponding Keycloak version at https://access.redhat.com/articles/2342881
3. Find the correct Keycloak version identifier at https://mvnrepository.com/artifact/org.keycloak/keycloak-core?repo=redhat-ga

For example, Keycloak 9.0.13 corresponds to version identifier `9.0.13.redhat-00006`.

### 4. Build Against RH SSO

```bash
./mvnw clean package -Prh-sso -Dkeycloak.version=9.0.13.redhat-00006
```

### 5. Test the Build

If the build is successful, the JAR file will be located at `target/keycloak-config-cli.jar`.

## Version Compatibility Matrix

| Red Hat SSO Version | Keycloak Version | keycloak-config-cli Version | Status |
|---------------------|------------------|---------------------------|---------|
| 7.4.x | 4.8.x | 2.x+ | ✅ Compatible |
| 7.5.x | 9.0.x | 3.x+ | ✅ Compatible |
| 7.6.x | 15.x | 4.x+ | ⚠️ May require patches |

## Known Limitations

### Breaking Changes

Red Hat may introduce breaking changes between versions that are not present in upstream Keycloak. If you encounter compiler errors during the build process, it's likely due to RH-specific changes.

### Feature Differences

Some features available in upstream Keycloak may not be available in Red Hat SSO, or may have different configurations:

- Certain authentication flows
- Specific user federation providers
- Custom themes and extensions

## Troubleshooting

### Build Errors

If you encounter compiler errors when building against RH SSO:

1. **Verify version compatibility**: Ensure you're using compatible versions
2. **Check for breaking changes**: Review RH SSO release notes
3. **Apply patches**: Look for community patches for your specific version

### Runtime Issues

For runtime issues with RH SSO:

1. **Enable debug logging**: Use `--logging.level.de.adorsys.keycloak.config=DEBUG`
2. **Check API compatibility**: Verify that the RH SSO API matches expected endpoints
3. **Test with dry-run**: Use `--import.dry-run=true` to test configurations

## Support Considerations

### Official Support

- Red Hat SSO is not officially supported by keycloak-config-cli
- Community support may be available for common issues
- Consider enterprise support for production deployments

### Community Resources

- GitHub Issues: Check for existing RH SSO related issues
- Community Forums: Look for RH SSO specific discussions
- Documentation: Community-maintained documentation may be available

## Best Practices

### Development

1. **Version Pinning**: Pin to specific RH SSO versions
2. **Testing**: Test thoroughly in non-production environments
3. **Backup**: Maintain backups of working configurations
4. **Documentation**: Document any custom patches or modifications

### Production

1. **Validation**: Validate configurations before deployment
2. **Monitoring**: Monitor for RH SSO specific issues
3. **Rollback Plan**: Have a rollback strategy ready

## Alternative Approaches

### Docker Approach

Instead of building against RH SSO, you can use Docker:

```bash
# Build custom Docker image
FROM openjdk:8-jdk-alpine
COPY target/keycloak-config-cli.jar /app/
WORKDIR /app
CMD ["java", "-jar", "keycloak-config-cli.jar"]
```

### Compatibility Layer

Consider creating a compatibility layer if you need to support multiple Keycloak-based products:

```java
// Example compatibility check
if (isRedHatSSO()) {
    // Apply RH SSO specific logic
} else {
    // Use standard Keycloak logic
}
```

## Next Steps

- [Keycloak Versions](./keycloak-versions) - Keycloak version compatibility
- [Limitations](./limitations) - Known limitations and constraints
- [Configuration](../configuration/overview) - General configuration options

## Additional Resources

- [Red Hat SSO Documentation](https://access.redhat.com/documentation/red-hat-single-sign-on/)
- [Keycloak Documentation](https://www.keycloak.org/documentation/)
- [GitHub Repository](https://github.com/adorsys/keycloak-config-cli)
