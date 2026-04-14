# Docker & Helm Overview

keycloak-config-cli is designed to be easily deployed inside containerized environments and orchestrated via Kubernetes. A Docker image is published and updated regularly, providing an ideal way to perform idempotent Keycloak configuration within your CI/CD pipelines or as an init container.

## Available Distribution Methods

1. **Docker Hub & Quay.io**: Official images are built and pushed to both registries. These images contain minimal overhead and run the CLI directly. Support for multiple target Keycloak versions is provided through image tags.
2. **Helm Chart**: Using the official Helm chart, you can deploy the tool natively into Kubernetes, orchestrating how and when the configuration is synchronized with the Keycloak instances.

Explore the following guidelines to get started:
- [Docker Usage](docker-usage.md) - Learn how to run keycloak-config-cli inside Docker containers.
- [Helm Chart](helm-chart.md) - Detailed properties and instructions for Kubernetes deployments.
- [Kubernetes Examples](kubernetes-deployment.md) - Reference for deploying with raw Kubernetes manifests (e.g., Jobs, CronJobs).
