#!/usr/bin/env bash

set -xeo pipefail

export SPRING_PROFILES_ACTIVE=dev

./target/keycloak-config-cli-native
./target/keycloak-config-cli-native
./target/keycloak-config-cli-native --import.force=true

while read -r file; do
  ./target/keycloak-config-cli-native --import.path="${file}"
done < <(
  find src/test/resources/import-files \
    -type f \
    ! -path '*cli*' \
    -and ! -path '*exported-realm*' \
    -and ! -name '*invalid*' \
    -and ! -name '*try*' | sort -n
)
