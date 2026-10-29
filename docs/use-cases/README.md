# Use cases

This section explains common keycloak-config-cli scenarios with small,
copyable configuration examples. Each page describes the desired state,
the import command, and the management behavior to verify afterwards.

## Available scenarios

- [Working with arrays](arrays-handling.md): add, replace, and remove values in
  collection properties such as client redirect URIs.

For the complete option reference, see [Import settings](../IMPORT.md),
[resource management](../MANAGED.md), and the [supported features](../FEATURES.md).

## Importing an example

Examples are configuration input, not Admin Console instructions. Save the
example as JSON and import it with the CLI:

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=example.json
```

After the command completes, verify the resulting state in Keycloak or by
running the import again and checking that no unintended changes are reported.
