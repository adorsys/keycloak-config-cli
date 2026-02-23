# JavaScript Variable Substitution

keycloak-config-cli supports an explicit JavaScript evaluation phase for variable substitution in configuration files. This allows for complex logic, calculations, and conditional configurations directly within your JSON/YAML files.

## Important Note on Syntax

There are two ways JavaScript evaluation might appear in documentation, but only one is recommended and fully supported by the sandboxed engine in `keycloak-config-cli`:

1.  **Recommended Syntax**: `$${javascript: ... }` (Note the double dollar sign)
2.  **Legacy/Standard Syntax**: `$(script:javascript: ... )`

**You should always use `$${javascript: ... }`.**

The legacy `$(script:javascript: ... )` syntax is part of the standard Apache Commons Text lookups, but it is often disabled or unconfigured for security reasons, and it does not have access to the same sandboxed environment and `env` context as the recommended syntax.

## Enabling the Feature

This is an **opt-in** feature and is **disabled by default** for security reasons.

To enable it, you must set the following properties:

```bash
--import.var-substitution.enabled=true
--import.var-substitution.script-evaluation-enabled=true
```

## Syntax & Examples

JavaScript expressions are enclosed in `$${javascript: ... }`.

### 1. Simple Calculations

You can perform arithmetic operations directly.

```json
{
  "sessionTimeout": "$${javascript: 2 * 24 * 60 * 60}"
}
```
*Result: `172800`*

### 2. Conditional Logic (Boolean)

Expressions that evaluate to a boolean will be inserted as boolean values in the resulting JSON (not as strings "true"/"false").

```json
{
  "enabled": "$${javascript: env.APP_ENV === 'INT'}"
}
```
*Result if `APP_ENV=INT`: `"enabled": true`*

### 3. Using Environment Variables

The JavaScript engine has access to an `env` object that contains all environment variables and Java system properties.

**IMPORTANT: Nested substitutions like `$(script:javascript: ... )` or `$(script:javascript:"$(env:APP_ENV)" === "INT")` are NOT supported.**

**Correct way to access environment variables:**
Use `env.VARIABLE_NAME` or `env['VARIABLE_NAME']`.

```json
{
  "enabled": "$${javascript: env.APP_ENV === 'INT'}"
}
```

**Avoid Nested Substitution:**
While `$(env:APP_ENV)` might work inside a script block because standard substitution runs first, it is much cleaner and safer to use the `env` object:

*   **Recommended**: `"$${javascript: env.APP_ENV === 'INT'}"`
*   **Discouraged**: `"$${javascript: '$(env:APP_ENV)' === 'INT'}"`

## Context

The `env` object contains:
- All environment variables (e.g., `env.PATH`, `env.APP_ENV`).
- All Java system properties (e.g., `env['user.dir']`, `env['java.version']`).

Example usage of `env`:

```json
{
  "realm": "$${javascript: 'realm-' + (env.APP_ENV || 'default').toLowerCase()}"
}
```

## Security & Sandboxing

The JavaScript evaluation is sandboxed using GraalVM Polyglot:
- **No Host Access**: Scripts cannot access Java classes or the host file system.
- **No Network Access**: Scripts cannot make network requests.
- **Deterministic**: The environment is restricted to the provided `env` context.
- **JSON Serializable**: The evaluation result must be a JSON-serializable type:
    - `String`
    - `Number` (Integer, Long, Double)
    - `Boolean`
    - `null`
    - `Array`
    - `Object` (Map)

## Processing Flow

1. **Load raw content**: The configuration file is loaded as a string.
2. **Standard Variable Substitution**: `$(...)` substitutions are performed first.
3. **JavaScript Evaluation**: Any `$${javascript: ... }` blocks are detected and evaluated.
4. **Replacement**: The blocks are replaced by their evaluation results.
    - **Strings** are inserted raw.
    - **Other types** (boolean, number, etc.) are serialized to JSON strings.
5. **JSON/YAML Parsing**: The final resulting string is parsed as JSON or YAML.

## Troubleshooting

### IllegalArgumentException: Cannot resolve variable 'script:javascript:...'

If you see this error, it means you are using the `$(script:javascript:...)` syntax. As mentioned above, this syntax is not supported by the specialized sandboxed evaluator.

**Solution**: Change `$(script:javascript: ... )` to `$${javascript: ... }`.

**Example Change:**
- **From**: `"enabled": $(script:javascript:"$(env:APP_ENV)" === "INT")`
- **To**: `"enabled": "$${javascript: env.APP_ENV === 'INT'}"`

### Script evaluation used but --import.var-substitution.script-evaluation-enabled not set

This means you have `$${javascript: ... }` in your file, but the feature is not enabled.

**Solution**: Add `--import.var-substitution.script-evaluation-enabled=true` to your command line.

## Setting Environment Variables

Since `keycloak-config-cli` runs locally, the environment variables must be available in the environment where the CLI is executed.

### 1. In a Shell (Linux/macOS)

Use the `export` command before running the JAR:

```bash
export APP_ENV=INT
export SESSION_DAYS=2

java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.var-substitution.script-evaluation-enabled=true \
  --import.files.locations=my-config.json
```

### 2. In Docker

Pass environment variables using the `-e` flag:

```bash
docker run \
    -e APP_ENV="INT" \
    -e IMPORT_VAR-SUBSTITUTION_ENABLED=true \
    -e IMPORT_VAR-SUBSTITUTION_SCRIPT-EVALUATION-ENABLED=true \
    -v $(pwd)/config:/config \
    adorsys/keycloak-config-cli:latest
```

*Note: In Docker, property names with dots are usually replaced by underscores (e.g., `IMPORT_VAR-SUBSTITUTION_ENABLED`).*

### 3. In Helm

If you are using the Helm chart, you can set environment variables in your `values.yaml`:

```yaml
extraEnv:
  - name: APP_ENV
    value: "INT"

configuration:
  import:
    var-substitution:
      enabled: true
      script-evaluation-enabled: true
```

## Detailed Examples

### Conditional Registration

```json
{
  "registrationAllowed": "$${javascript: env.APP_ENV !== 'PROD'}"
}
```

### Complex Realm Display Name

You can use the `$${javascript: ... }` syntax to evaluate a string for the `displayName` and `displayNameHtml` fields.

```json
{
  "realm": "my-realm",
  "displayName": "$${javascript: 'System - ' + (env.APP_ENV === 'PROD' ? 'Production' : 'Non-Prod')}",
  "displayNameHtml": "$${javascript: '<div class=\"kc-logo-text\"><span>' + (env.APP_ENV || 'Default') + '</span></div>'}",
  "enabled": true
}
```
