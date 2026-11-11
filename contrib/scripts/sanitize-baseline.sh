#!/bin/sh

# Sanitize a prepared baseline before adding it to src/main/resources/baseline.
# Write to a different file: redirecting onto the input would truncate it.
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 baseline.json > sanitized-baseline.json" >&2
  exit 1
fi

jq 'walk(if type == "object" then del(.privateKey) else . end)' "$1"
