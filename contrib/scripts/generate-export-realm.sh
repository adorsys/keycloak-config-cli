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
  -v "${MKTMP}:/opt/keycloak/imex" \
  -e ROOT_LOGLEVEL=ERROR \
  -e KEYCLOAK_LOGLEVEL=ERROR \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin123 \
  "quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}" \
  start-dev

while ! docker exec keycloak-export bash -c '/opt/keycloak/bin/kcadm.sh config credentials --server http://$HOSTNAME:8080/auth --realm master'; do
  sleep 2
done

sleep 2

docker stop keycloak-export

sleep 2

docker run -ti --rm \
  --name keycloak-export \
  -v "${GIT_ROOT}/src/test/resources/import-files/exported-realm/${KEYCLOAK_VERSION}/:/tmp/export/" \
  -v "${MKTMP}:/opt/keycloak/imex" \
  -e ROOT_LOGLEVEL=ERROR \
  -e KEYCLOAK_LOGLEVEL=ERROR \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin123 \
  --entrypoint bash \
  "quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}" \
  -c "timeout 30 /opt/keycloak/bin/kc.sh export --dir /tmp/export --users realm_file"

rm -rf "${MKTMP}"
