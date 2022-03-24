#!/usr/bin/env bash

set -e

_sedi() {
    if [ "$(uname)" = "Darwin" ]; then
        sed -i "" "$@"
    else
        sed -i "$@"
    fi
}

if [ -n "${1:-}" ]; then
  ./mvnw versions:set-property -Dproperty=keycloak.version -DnewVersion="$1"
fi

GIT_ROOT="$(git rev-parse --show-toplevel)"
KEYCLOAK_VERSION_OLD="$(tail -n1 "${GIT_ROOT}/.env" | cut -d= -f2)"
KEYCLOAK_VERSION_NEW=$(./mvnw -f "${GIT_ROOT}" help:evaluate -Dexpression=keycloak.version -q -DforceStdout)
_sedi "s/KEYCLOAK_VERSION=.*/KEYCLOAK_VERSION=${KEYCLOAK_VERSION_NEW}/" "${GIT_ROOT}/.env"
_sedi "s/KEYCLOAK_VERSION=.*/KEYCLOAK_VERSION=${KEYCLOAK_VERSION_NEW}/" "${GIT_ROOT}/Dockerfile"

"${GIT_ROOT}/contrib/scripts/generate-export-realm.sh"

if [[ "${KEYCLOAK_VERSION_OLD%%.*}" -eq "${KEYCLOAK_VERSION_NEW%%.*}" ]]; then
  _sedi "s/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_OLD}/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_NEW}/" "${GIT_ROOT}/.github/workflows/ci.yaml"
else
  KEYCLOAK_VERSION_NEW="${KEYCLOAK_VERSION_NEW%%.*}"
  KEYCLOAK_VERSION_UNSUPPORTED=$((KEYCLOAK_VERSION_NEW-4))

  _sedi "/- KEYCLOAK_VERSION: ${KEYCLOAK_VERSION_UNSUPPORTED}/d" "${GIT_ROOT}/.github/workflows/ci.yaml"
fi

echo "Upgrade done"
