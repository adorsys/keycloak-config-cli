#!/usr/bin/env bash

set -xeo pipefail

export SPRING_PROFILES_ACTIVE=dev

./target/keycloak-config-cli-native --keycloak.availability-check.enabled=true
./target/keycloak-config-cli-native
./target/keycloak-config-cli-native --import.force=true

while read -r file; do
  ./target/keycloak-config-cli-native --import.path="${file}"
done < <(
  find src/test/resources/import-files \
    -type f \
    -name '*.json' \
    ! -path '*/cli/*' \
    -and ! -path '*exported-realm*' \
    -and ! -path '*parallel*' \
    -and ! -name '*invalid*' \
    -and ! -name '*try*' | sort -n
)
