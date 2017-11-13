#!/usr/bin/env bash
set -e

TAG_PATTERN="^v([[:digit:]]+\.)+[[:digit:]]+(-[[:digit:]]+)?$"

if [[ ${TRAVIS_TAG} =~ ${TAG_PATTERN} ]]; then
  echo "RELEASE TAG -> publish $TRAVIS_TAG to mvn central";
  mvn --settings .travis/settings.xml package gpg:sign deploy -Prelease -DskipTests -B -U;
else
  echo "NO RELEASE TAG -> don't publish to mvn central";
fi
