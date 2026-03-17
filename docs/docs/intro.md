---
title: Introduction
description: keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file.
sidebar_position: 1
---

![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg) ![GitHub release](https://img.shields.io/github/v/release/adorsys/keycloak-config-cli?logo=github&sort=semver) ![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker) ![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/main/graph/badge.svg) ![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)

# Keycloak Config CLI

keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file. The format of the JSON/YAML file is based on the export realm format. Store and handle the configuration files inside git just like normal code. A Keycloak restart isn't required to apply the configuration.

## Key Features

- **Declarative Configuration**: Define your Keycloak realm configuration in JSON/YAML files
- **GitOps Ready**: Store configuration files in git for version control and CI/CD
- **No Downtime**: Apply configuration changes without restarting Keycloak
- **Variable Substitution**: Support for environment variables and dynamic configuration
- **Import Methods**: Multiple ways to import and apply configurations
- **Docker & Helm Ready**: Container-friendly deployment options

## Quick Overview

The config files are based on the Keycloak export files. You can use them to re-import your settings. Keep your files as small as possible by removing all UUIDs and default settings set by Keycloak.

[Check out our example configuration](https://github.com/adorsys/keycloak-config-cli/blob/main/contrib/example-config/moped.json) for a complete working example. More examples are available in our [test resources](https://github.com/adorsys/keycloak-config-cli/tree/main/src/test/resources/import-files).

## Variable Substitution

keycloak-config-cli supports variable substitution of config files. This can be enabled by `import.var-substitution.enabled=true` (**disabled by default**).

Variables exposed by Spring Boot (through configtree or [external configuration](https://docs.spring.io/spring-boot/docs/2.5.0/reference/htmlsingle/#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)) can be accessed by `$(property.name)`.

:::note

For advanced variable substitution including JavaScript support, see our [Variable Substitution](https://github.com/adorsys/keycloak-config-cli/blob/main/docs-backup/javascript-substitution.md) guide.

:::

## Next Steps

- [Installation](./installation) - Get started with installing keycloak-config-cli
- [Quick Start](./quick-start) - Set up your first configuration
- [Configuration](./configuration/overview) - Learn about configuration options
- [Docker & Helm](https://github.com/adorsys/keycloak-config-cli#docker) - Deploy using containers
