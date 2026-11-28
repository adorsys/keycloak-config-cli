# Organization groups

Keycloak 26.6+ supports groups that belong to one organization. Declare them under `groups` of an organization:

```yaml
organizations:
  - alias: acme
    members:
      - username: alice
    groups:
      - name: engineering
        description: Engineering             # optional; not declared = keep
        attributes:                          # optional; not declared = keep
          cost-center: ["42"]
        members: [alice]                     # optional usernames; must be organization members
        subGroups:                           # optional; not declared = keep the existing sub-groups
          - name: backend
```

- Groups are matched by name below their parent.
- With `import.managed.organization=full` (the default), groups that are not declared on a level whose list is declared are deleted. `groups: []` removes all groups of the organization.
- Leaving out `groups` leaves the organization's groups untouched.
- Keycloak does not support role mappings on organization groups, so `realmRoles`/`clientRoles` are not available here.
- On Keycloak versions older than 26.6, declared organization groups are skipped with a warning.
