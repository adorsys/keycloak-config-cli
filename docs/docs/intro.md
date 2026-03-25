---
title: Introduction
description: keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file.
sidebar_position: 1
---

![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg) ![GitHub release](https://img.shields.io/github/v/release/adorsys/keycloak-config-cli?logo=github&sort=semver) ![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker) ![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/main/graph/badge.svg) ![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)

# Keycloak Config CLI

keycloak-config-cli is a declarative configuration tool for managing Keycloak. It allows you to define and maintain your Keycloak setup—such as realms, clients, users, and roles—using JSON or YAML files instead of manual UI operations.

By treating configuration as code, you can ensure consistency, repeatability, and easier maintenance across different environments without relying on manual changes in the Keycloak Admin Console.

## Key Features

- **Declarative Configuration**: Define the desired state of your Keycloak instance using JSON or YAML
- **Idempotent Execution**: Apply configurations multiple times without creating duplicates or inconsistencies
- **Automation Ready**: Integrate seamlessly into CI/CD pipelines and deployment workflows
- **Version Controlled**: Store configurations in Git to track and manage changes over time

## How It Works

keycloak-config-cli connects to a running Keycloak instance and applies the provided configuration files. It compares the current state with the desired state and performs only the necessary updates.

This ensures your Keycloak instance always remains aligned with your configuration.

## Who Is It For?

keycloak-config-cli is useful for:

- Developers managing local or development environments
- DevOps engineers automating infrastructure and deployments
- Teams maintaining consistent configurations across multiple environments

## Documentation Overview

This documentation helps you:

- Get started quickly with installation and setup
- Understand configuration options and supported resources
- Explore advanced usage and integration patterns
- Troubleshoot common issues and edge cases


## Next Steps


- [Quick Start](quick-start.md) - Set up your first configuration
- [Configuration](configuration/overview.md) - Learn about configuration options
- [Docker & Helm](https://github.com/adorsys/keycloak-config-cli#docker) - Deploy using containers