# Working with arrays

Arrays in a keycloak-config-cli representation describe collection values such
as redirect URIs, web origins, roles, group memberships, protocol mappers, and
client scopes.

keycloak-config-cli imports a desired state. An array is therefore not an
imperative “append” instruction: the values in the configuration are the values
that the relevant resource should have after import.

## Add a value

Include the existing values and the new value in the JSON representation:

```json
{
  "realm": "myrealm",
  "clients": [
    {
      "clientId": "my-app",
      "redirectUris": [
        "https://app.example.com/callback",
        "https://app.example.com/oauth/callback",
        "http://localhost:3000/callback"
      ]
    }
  ]
}
```

Import it with the CLI:

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=client.json
```

## Replace or remove values

To replace a collection, provide the complete desired array. To remove a value,
omit it from that desired array. Do not use an empty array unless you intend to
remove every value from that managed collection.

Whether resources absent from an import are deleted is controlled by the
resource-management settings and the resource type. See
[resource management](../MANAGED.md) before using an empty array or changing
`import.remote-state.enabled`.

## Safe workflow

1. Start with the current JSON representation of the resource.
2. Make the smallest array change needed.
3. Import the JSON through keycloak-config-cli.
4. Verify the resulting collection in Keycloak.
5. Repeat the import to confirm that the configuration is idempotent.
