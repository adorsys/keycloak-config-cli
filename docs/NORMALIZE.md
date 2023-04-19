# Realm normalization

Realm normalization is a feature that is supposed to aid users in migrating from an "unmanaged" Keycloak installation,
to an installation managed by keycloak-config-cli.
To achieve this, it uses a full [realm export](https://www.keycloak.org/server/importExport#_exporting_a_specific_realm)
as an input, and only retains things that deviate from the default

## Usage

To run the normalization, run keycloak-config-cli with the CLI option `--run.operation=NORMALIZE`.
The default value for this option is `IMPORT`, which will run the regular keycloak-config-cli import.

### Configuration options

| Configuration key                    | Purpose                                                                                                         | Example       |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------|---------------|
| run.operation                        | Tell keycloak-config-cli to normalize, rather than import                                                       | NORMALIZE     |
| normalization.files.input-locations  | Which realm files to import                                                                                     | See IMPORT.md |
| normalization.files.output-directory | Where to save output realm files                                                                                | ./exports/out |
| normalization.output-format          | Whether to output JSON or YAML. Default value is YAML                                                           | YAML          |
| normalization.fallback-version       | Use this version as a baseline of keycloak version in realm is not available as baseline in keycloak-config-cli | 19.0.3        |

### Unimplemented Features
- Components:
  - Currently, keycloak-config-cli will not yet look at the `components` section of the exported JSON
  - Therefore, some things (like LDAP federation configs and Key providers) are missing from the normalized YAML
- Users
  - Users are not currently considered by normalization.

## Missing entries
keycloak-config-cli will WARN if components that are present in a realm by default are missing from an exported realm.
An example of such a message is:
```
Default realm requiredAction 'webauthn-register-passwordless' was deleted in exported realm. It may be reintroduced during import
```
Messages like these will often show up when using keycloak-config-cli to normalize an import from an older version of Keycloak, and compared to a newer baseline.
In the above case, the Keycloak version is 18.0.3, and the baseline for comparison was 19.0.3.
Since the webauthn feature was not present (or enabled by default) in the older version, this message is mostly informative.
If a message like this appears on a component that was *not* deleted or should generally be present, this may indicate a bug in keycloak-config-cli.

## Cleaning of invalid data
Sometimes, realms of existing installations may contain invalid data, due to faulty migrations, or due to direct interaction with the database,
rather than the Keycloak API.
When such problems are found, we attempt to handle them in keycloak-config-cli, or at least notify the user about the existence of these problems.

### SAML Attributes on clients
While not necessarily invalid, openid-connect clients that were created on older Keycloak versions, will sometimes contain
SAML-related attributes. These are filtered out by keycloak-config-cli.

### Unused non-top-level Authentication Flows
Authentication flows in Keycloak are marked as top-level if they are supposed to be available for binding or overrides.
Authentication flows that are not marked as top-level are used as sub-flows in other authentication flows.
The normalization process recognizes recursively whether there are authentication flows that are not top level and not used
by any top level flow, and does not include them in the final result.

A warning message is logged, and you can use the following SQL query to find any authentication flows that are not referenced.
Note that this query, unlike keycloak-config-cli, is not recursive.
That means that after deleting an unused flow, additional unused flows may appear after the query is performed again.

```sql
select flow.alias
from authentication_flow flow
         join realm r on flow.realm_id = r.id
         left join authentication_execution execution on flow.id = execution.auth_flow_id
where r.name = 'mytest'
  and execution.id is null
  and not flow.top_level
```

### Unused and duplicate Authenticator Configs
Authenticator Configs are not useful if they are not referenced by at least one authentication execution.
Therefore, keycloak-config-cli detects unused configurations and does not include them in the resulting output.
Note that the check for unused configs runs *after* the check for unused flows.
That means a config will be detected as unused if it is referenced by an execution that is part of a flow that is unused.

A warning message is logged on duplicate or unused configs, and you can use the following SQL query to find any configs
that are unused:

```sql
select ac.alias, ac.id
from authenticator_config ac
         left join authentication_execution ae on ac.id = ae.auth_config
         left join authentication_flow af on ae.flow_id = af.id
         join realm r on ac.realm_id = r.id
where r.name = 'master' and af.alias is null
order by ac.alias
```

And the following query to find duplicates:

```sql
select alias, count(alias), r.name as realm_name
from authenticator_config
         join realm r on realm_id = r.id
group by alias, r.name
having count(alias) > 1
```

If the `af.id` and `af.alias` fields are `null`, the config in question is not in use.
Note that configs used by unused flows are not marked as unused in the SQL result, as these need to be deleted first
to become unused.
After the unused flows (and executions) are deleted, the configs will be marked as unused and can also be deleted.

### Authentication Executions with invalid subflows
Some keycloak exports have invalid authentication executions that reference a subflow, while also setting an authenticator.
This is only a valid configuration if the subflow's type is `form-flow`.
If it is not, then keycloak-config-cli will not import the configuration.
This will be marked by an ERROR severity message in the log output.
You can use this SQL query to find offending entries and remediate the configuration errors before continuing.

```sql
select parent.alias,
       subflow.alias,
       execution.alias
from authentication_execution execution
         join realm r on execution.realm_id = r.id
         join authentication_flow parent on execution.flow_id = parent.id
         join authentication_flow subflow on execution.auth_flow_id = subflow.id
where execution.auth_flow_id is not null
  and execution.authenticator is not null
  and subflow.provider_id <> 'form-flow'
  and r.name = 'REALMNAME';
```
