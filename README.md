# keycloak-config-cli

## Before using

Before you use the module, make sure you start keycloack and register required providers by installing the modules.

## Usage

This module will do the following work:

Use the file `<your config path>/${realm}/realm.yml` to create new realms

Use the file `<your config path>/${realm}/clients.yml` to register new clients.

Use the file `<your config path>/${realm}/components.yml` register components like the custom-user-storage-provider

Use the file `<your config path>/${realm}/authentication.yml` to register new authentication flows and bind them to the browser login flow and the direct grant login flow.

### Docker

#### Docker run

```
$ docker run -e KEYCLOAK_URL=http://<your keycloak host>:8080/auth \
             -e KEYCLOAK_ADMIN=<keycloak admin username> \
             -e KEYCLOAK_ADMIN_PASSWORD=<keycloak admin password> \
             -v <your config path>:/opt/keycloak-config-cli/configs \
             adorsys/keycloak-config-cli:latest
```

#### Docker-compose

```
version: '3.1'
services:
  keycloak:
    image: jboss/keycloak:3.2.1.Final
    command:
    - "-b"
    - "0.0.0.0"
  post_process:
    image: adorsys/keycloak-config-cli:latest
    depends_on:
    - keycloak
    links:
    - keycloak
    volumes:
    - <your config path>:/opt/keycloak-config-cli/configs
    environment:
    - KEYCLOAK_URL=http://<your keycloak host>:8080/auth
    - KEYCLOAK_ADMIN=<keycloak admin username>
    - KEYCLOAK_ADMIN_PASSWORD=<keycloak admin password>

```

### Within maven

```
$ ./config.sh http://<your keycloak host>:8080/auth
```

where `<your keycloak host>` is the hostname of your local docker-host. This could also be `localhost` if the docker where you run keycloak is available on localhost.
