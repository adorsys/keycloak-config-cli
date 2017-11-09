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
sh /opt/wtfc.sh -T 120 -S 0 -I 2 curl -f ${KEYCLOAK_URL}

$JAVA_HOME/bin/java $JAVA_OPTS -jar ./keycloak-config-cli-*.jar \
  --keycloakUrl=${KEYCLOAK_URL} \
  --keycloakUser=${KEYCLOAK_ADMIN} \
  --keycloakPassword=${KEYCLOAK_ADMIN_PASSWORD} \
  --keycloakConfigDir=/opt/keycloak-config-cli/configs
