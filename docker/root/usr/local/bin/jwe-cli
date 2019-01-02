#!/bin/bash

JWKS_URL=${KEYCLOAK_URL}/auth/realms/master/protocol/openid-connect/certs

if [ -z ${JWKS_CONNECT_TIMEOUT} ] || [ -z ${JWKS_READ_TIMEOUT} ] || [ -z ${JWKS_SIZE_LIMIT} ]
then
    java -jar /usr/local/share/encrypt-password-for-keycloak.jar \
         --jwks-url ${JWKS_URL} \
         --password $1
else
    java -jar /usr/local/share/encrypt-password-for-keycloak.jar \
         --jwks-url ${JWKS_URL} \
         --password $1 \
         --connectTimeout ${JWKS_CONNECT_TIMEOUT} \
         --readTimeout ${JWKS_READ_TIMEOUT} \
         --sizeLimit ${JWKS_SIZE_LIMIT}
fi
