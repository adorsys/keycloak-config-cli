#!/bin/sh

GIT_ROOT="$(git rev-parse --show-toplevel)"

MKTMP="$(mktemp -d)"

# shellcheck source=../../.env
. "${GIT_ROOT}/.env"

mkdir -p "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/"
touch "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/master-realm.json"

docker run -d \
  --name keycloak-export \
  -v "${MKTMP}:/opt/jboss/keycloak/standalone/data" \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin123 \
  "jboss/keycloak:${KEYCLOAK_VERSION}"

while ! docker exec -it keycloak-export bash -c '/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://$HOSTNAME:8080/auth --realm master --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD'; do
  sleep 2
done

sleep 2
docker stop keycloak-export
docker rm -f keycloak-export

docker run -ti --rm \
  --name keycloak-export \
  -v "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/:/tmp/export/" \
  -v "${MKTMP}:/opt/jboss/keycloak/standalone/data" \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin123 \
  "jboss/keycloak:${KEYCLOAK_VERSION}" \
  -Dkeycloak.migration.action=export \
  -Dkeycloak.migration.provider=dir \
  -Dkeycloak.migration.dir=/tmp/export \
  -Dkeycloak.migration.usersExportStrategy=REALM_FILE

rm -rf "${MKTMP}"
