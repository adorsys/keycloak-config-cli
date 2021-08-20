#!/bin/sh

trap 'docker rm -f keycloak-export > /dev/null 2>&1 || true' EXIT

GIT_ROOT="$(git rev-parse --show-toplevel)"

MKTMP="$(mktemp -d)"

# shellcheck source=../../.env
. "${GIT_ROOT}/.env"

mkdir -p "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/"
touch "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/master-realm.json"

docker run -d --rm \
  --name keycloak-export \
  -v "${MKTMP}:/opt/jboss/keycloak/standalone/data" \
  -e ROOT_LOGLEVEL=ERROR \
  -e KEYCLOAK_LOGLEVEL=ERROR \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin123 \
  "quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}"

while ! docker exec keycloak-export bash -c '/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://$HOSTNAME:8080/auth --realm master --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD'; do
  sleep 2
done

sleep 2

docker stop keycloak-export

sleep 2

docker run -ti --rm \
  --name keycloak-export \
  -v "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/:/tmp/export/" \
  -v "${MKTMP}:/opt/jboss/keycloak/standalone/data" \
  -e ROOT_LOGLEVEL=ERROR \
  -e KEYCLOAK_LOGLEVEL=ERROR \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin123 \
  --entrypoint bash \
  "quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}" \
  -c "timeout 30 /opt/jboss/keycloak/bin/standalone.sh -Dkeycloak.migration.action=export -Dkeycloak.migration.provider=dir -Dkeycloak.migration.dir=/tmp/export -Dkeycloak.migration.usersExportStrategy=REALM_FILE"

rm -rf "${MKTMP}"
