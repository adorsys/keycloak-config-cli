## Environment Variable Substitution for JSON Arrays
This document provides a step-by-step guide on importing environment variables into JSON arrays using the Keycloak Config CLI.

### Prerequisites
- Ensure you have dotenv installed and configured in your environment.
- Have a Keycloak Config CLI setup ready for performing realm imports.
### Steps to Perform Import of Environment Variables
  - Step 1: Configure the .env File

    Create or update your .env file to include the necessary environment variables. For example:

```bash
CLIENT_WEBAPP_REDIRECT_URIS="https://app1.example.com/callback","https://app2.example.com/callback","https://app3.example.com/callback"
```
This environment variable contains a comma-separated list of redirect URIs.

- Step 2: Prepare the JSON Configuration File

Include the environment variable reference in your JSON configuration file. For example:

```json
{
  "enabled": true,
  "realm": "realmWithClient",
  "clients": [
    {
      "clientId": "my-client",
      "redirectUris": ["$(env:CLIENT_WEBAPP_REDIRECT_URIS)"]
    }
  ]
}
```
Here, the `$(env:CLIENT_WEBAPP_REDIRECT_URIS)` syntax indicates that the value should be replaced with the corresponding environment variable.

- Step 3: Execute the Import Command

Use the dotenv command together with the import command to load the .env file and execute the Keycloak Config CLI import:

```bach
dotenv -e /PATH/TO/THE/.env kc-cli import --file /PATH/TO/CONFIG.JSON
```
Replace /PATH/TO/THE/.env with the full path to your .env file and /PATH/TO/CONFIG.JSON with the path to your JSON configuration file.

- Step 4: Enable Variable Substitution

Set the IMPORT_VARSUBSTITUTION_ENABLED environment variable to true to activate variable substitution:

```
export IMPORT_VARSUBSTITUTION_ENABLED=true
```
Ensure this environment variable is set in your shell or runtime environment before performing the import.
