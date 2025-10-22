# Keycloak Configuration JSON Schema Usage Guide

This document outlines how the `keycloak-config-cli-import-schema.json` can be effectively utilized across various stages of software development and operations within an organization. The schema provides a robust way to validate Keycloak realm and client configurations, ensuring consistency, correctness, and maintainability.

## 1. Introduction to the JSON Schema

The `keycloak-config-cli-import-schema.json` defines the expected structure and data types for Keycloak configuration files (both YAML and JSON formats) used by `keycloak-config-cli`. By adhering to this schema, developers can prevent common configuration errors, improve readability, and streamline the development and deployment process.

## 2. Usage in Development Environments (IDEs)

Integrating the JSON schema into your Integrated Development Environment (IDE) provides immediate feedback and enhances the developer experience significantly.

### How to Integrate:

1.  **Reference the Schema**: In your Keycloak configuration files (e.g., `realm.yaml`, `client.json`), add a `$schema` property at the root level, pointing to the local schema file.

    - **For YAML files**:

      ```yaml
      # yaml-language-server: $schema=../../src/main/resources/keycloak-config-cli-import-schema.json
      realm: my-realm
      enabled: true
      # ... rest of your configuration
      ```

      (Adjust the relative path `../../src/main/resources/keycloak-config-cli-import-schema.json` based on your file's location relative to the schema file.)

    - **For JSON files**:
      ```json
      {
        "$schema": "./keycloak-config-cli-import-schema.json",
        "realm": "my-json-realm",
        "enabled": true
        // ... rest of your configuration
      }
      ```
      (Adjust the relative path `./keycloak-config-cli-import-schema.json` based on your file's location relative to the schema file.)

2.  **IDE Support**: Most modern IDEs (e.g., VS Code, IntelliJ IDEA) have built-in support for JSON Schema validation. Once the `$schema` reference is added, the IDE will automatically:
    - **Provide Autocompletion**: Yes, when you start typing a property name (e.g., "e" for "enabled" or "eventsEnabled"), the IDE will suggest valid properties and values based on the schema. This significantly speeds up configuration writing and reduces errors.
    - **Highlight Errors**: Underline or mark invalid configurations in real-time.
    - **Offer Tooltips**: Display detailed error messages and expected types (e.g., "Value must be one of [true, false]" for boolean fields).

### Benefits for Developers:

- **Early Error Detection**: Catch configuration errors before deployment, reducing debugging time.
- **Increased Productivity**: Autocompletion and inline validation speed up configuration writing.
- **Improved Consistency**: Ensures all configurations adhere to defined standards.
- **Reduced Cognitive Load**: Developers don't need to memorize all possible configuration options.

## 3. Usage in CI/CD Pipelines

Automating schema validation in your Continuous Integration/Continuous Deployment (CI/CD) pipeline is crucial for maintaining high quality and preventing erroneous configurations from reaching production environments.

### How to Integrate (Example using GitHub Actions with `ajv-cli` and `yq`):

You can add a step to your CI/CD workflow to validate configuration files against the schema.

```yaml
name: Validate Keycloak Configs

on: [push, pull_request]

jobs:
  validate-configs:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Install Node.js
        uses: actions/setup-node@v3
        with:
          node-version: "18"

      - name: Install AJV CLI (JSON Schema Validator)
        run: npm install -g ajv-cli

      - name: Install yq (YAML to JSON converter)
        run: sudo snap install yq

      - name: Validate YAML Configuration Files
        run: |
          SCHEMA_PATH="./src/main/resources/keycloak-config-cli-import-schema.json"
          CONFIG_FILES=$(find contrib/example-config -name "*.yaml")

          for file in $CONFIG_FILES; do
            echo "Validating YAML file: $file"
            # Convert YAML to JSON and then validate
            yq -o=json "$file" | ajv validate -s "$SCHEMA_PATH" -d -
            if [ $? -ne 0 ]; then
              echo "Validation failed for $file"
              exit 1
            fi
          done

      - name: Validate JSON Configuration Files
        run: |
          SCHEMA_PATH="./src/main/resources/keycloak-config-cli-import-schema.json"
          CONFIG_FILES=$(find src/main/resources -name "*.json") # Adjust path as needed

          for file in $CONFIG_FILES; do
            echo "Validating JSON file: $file"
            ajv validate -s "$SCHEMA_PATH" -d "$file"
            if [ $? -ne 0 ]; then
              echo "Validation failed for $file"
              exit 1
            fi
          done
```

### Benefits for CI/CD:

- **Automated Quality Gates**: Ensure that only valid configurations are merged and deployed.
- **Reduced Deployment Failures**: Prevent issues caused by malformed configurations in production.
- **Faster Feedback Loop**: Developers receive validation results directly from the pipeline.
- **Compliance**: Helps enforce organizational standards and best practices for configurations.

## 4. Benefits for Code Reviewers

Code reviewers play a critical role in maintaining code quality. The JSON schema significantly aids their efforts when reviewing Keycloak configurations.

### How it Helps Reviewers:

- **Focus on Logic, Not Syntax**: Reviewers can concentrate on the logical correctness and security implications of the configuration, rather than spending time on basic syntax or type errors.
- **Clearer Intent**: Well-validated configurations are easier to understand, as they adhere to a predictable structure.
- **Reduced Back-and-Forth**: Fewer trivial errors mean fewer iterations in the review process.
- **Onboarding**: New team members can quickly grasp the expected configuration format by relying on schema validation.

## 5. Keeping the Schema Updated

The JSON schema should be kept in sync with the `keycloak-config-cli`'s capabilities and the Keycloak API it interacts with.

- **Regular Review**: Periodically review the schema against the latest Keycloak versions and `keycloak-config-cli` updates.
- **Automated Generation (if applicable)**: If the schema can be generated from the Keycloak OpenAPI specification or Java classes, ensure the generation process is automated and part of the development workflow.
- **Version Control**: Store the schema in version control (`git`) alongside your code, allowing for tracking changes and ensuring consistency.

By implementing these practices, organizations can leverage the JSON schema to build more reliable, maintainable, and secure Keycloak configurations.
