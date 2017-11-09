# keycloak-config-cli

## Before using

Before you use the module, make sure you start keycloack and register required providers by installing the modules.

## Usage

This module will do the following work:

Use the file `src/test/resources/${realm}/realm.yml` to create new realms

Use the file `src/test/resources/${realm}/components.yml` register components like the custom-user-storage-provider

Use the file `src/test/resources/moped/authentication.yml` to register new authentication flows and bind them to the browser login flow and the direct grant login flow.

```
$ ./config.sh http://<keycloak_host>:8080/auth 
```

where `<keycloak_host>` is the hostname of your local docker-host. This could also be `localhost` if the docker where you run keycloak is available on localhost.
