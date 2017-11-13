#!/usr/bin/env bash
set -e

mvn --settings .travis/settings.xml package gpg:sign deploy -Prelease -DskipTests -B -U;
