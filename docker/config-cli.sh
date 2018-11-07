#!/usr/bin/env bash
set -e

if [ -z $KEYCLOAK_URL ]
	then
		echo "Missing KEYCLOAK_URL environment variable"
		exit 1
fi

if [ -z KEYCLOAK_ADMIN ]
	then
		echo "Missing KEYCLOAK_ADMIN environment variable"
		exit 1
fi

if [ -z KEYCLOAK_ADMIN_PASSWORD ]
	then
		echo "Missing KEYCLOAK_ADMIN_PASSWORD environment variable"
		exit 1
fi

echo 'wait until keycloak is available'
sh wtfc -T 120 -S 0 -I 2 curl -f ${KEYCLOAK_URL}

if [ "${KEYCLOAK_ENCRYPT_ADMIN_PASSWORD}" = true ]
then
    echo 'Encrypt admin password...'
    KEYCLOAK_ADMIN_PASSWORD=`jwe-cli "${KEYCLOAK_ADMIN_PASSWORD}"`
fi

$JAVA_HOME/bin/java $JAVA_OPTS -jar ./keycloak-config-cli.jar \
  --keycloakUrl=${KEYCLOAK_URL}/auth \
  --keycloakUser=${KEYCLOAK_ADMIN} \
  --keycloakPassword=${KEYCLOAK_ADMIN_PASSWORD} \
  --keycloakConfigDir=/tmp/keycloak-config-cli/configs
