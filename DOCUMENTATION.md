# General information

See: [README.md](README.md)

# Supported Features

See: [docs/FEATURES.md](docs/FEATURES.md)

# Workflow concurrency

On Keycloak versions supporting workflows, root-level `restart-in-progress` and
`cancel-in-progress` settings are sent using Keycloak's canonical field names.
Existing configurations using `restartInProgress` and `cancelInProgress` are
also accepted. Unset or null root-level settings are omitted from requests.

```yaml
realm: example
workflows:
  - name: disable-inactive-users
    enabled: true
    "on": user-created
    restart-in-progress: user-authenticated
    steps:
      - uses: disable-user
        after: 2m
```

The existing nested `concurrency` representation is also supported. Use one
representation for a given workflow to avoid conflicting settings. Quote `"on"`
in YAML so that it is read as a property name.

## Workflow step parameters

Values under a workflow step's `with` property may be written as a scalar or
as a list. A scalar is read as a one-element string list:

```yaml
workflows:
  - name: notify-new-user
    "on": user-created
    steps:
      - uses: notify-user
        with:
          message: Welcome to the realm
```

Use a YAML list when a parameter genuinely has more than one value:

```yaml
workflows:
  - name: notify-new-user
    "on": user-created
    steps:
      - uses: notify-user
        with:
          message:
            - Welcome to the realm
            - Complete your profile
```

The `with` representation is a map of string lists. Nested object values are
not supported.

# Declarative syntax and resource deletion

See: [docs/MANAGED.md](docs/MANAGED.md)

# Extend JSON, e.g. RealmRepresentation

See: [contrib/custom-representations/README.md](contrib/custom-representations/README.md)
