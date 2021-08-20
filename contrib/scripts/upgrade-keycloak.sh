#!/usr/bin/env bash

set -e

_sedi() {
    if [ "$(uname)" = "Darwin" ]; then
        sed -i "" "$@"
    else
        sed -i "$@"
    fi
}

GIT_ROOT="$(git rev-parse --show-toplevel)"
KEYCLOAK_VERSION_OLD="$(tail -n1 "${GIT_ROOT}/.env" | cut -d= -f2)"
KEYCLOAK_VERSION_NEW=$(./mvnw -f "${GIT_ROOT}" help:evaluate -Dexpression=keycloak.version -q -DforceStdout)
_sedi "s/KEYCLOAK_VERSION=.*/KEYCLOAK_VERSION=${KEYCLOAK_VERSION_NEW}/" "${GIT_ROOT}/.env"

"${GIT_ROOT}/contrib/scripts/generate-export-realm.sh"

if [[ "${KEYCLOAK_VERSION_OLD%%.*}" -eq "${KEYCLOAK_VERSION_NEW%%.*}" ]]; then
  _sedi "s/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_OLD}/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_NEW}/" "${GIT_ROOT}/.github/workflows/ci.yaml"
  _sedi "s/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_OLD}/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_NEW}/" "${GIT_ROOT}/.github/workflows/release.yaml"
else
  KEYCLOAK_VERSION_UNSUPPORTED=$((KEYCLOAK_VERSION_NEW-4))

  _sedi "/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_UNSUPPORTED}/d" "${GIT_ROOT}/.github/workflows/ci.yaml"
  _sedi "/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_UNSUPPORTED}/d" "${GIT_ROOT}/.github/workflows/release.yaml"
fi

echo "Upgrade done"
