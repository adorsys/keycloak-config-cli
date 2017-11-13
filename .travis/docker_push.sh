#!/usr/bin/env bash
set -e

docker login -u $DOCKER_HUB_USER -p $DOCKER_HUB_PASSWORD
docker push $DOCKER_IMAGE_NAME:${TRAVIS_TAG}
