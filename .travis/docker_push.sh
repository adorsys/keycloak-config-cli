#!/usr/bin/env bash
set -e

IMAGE_NAME="${DOCKER_IMAGE_NAME}:${TRAVIS_TAG}"

ls -la ./target/*.jar

docker login -u $DOCKER_HUB_USER -p $DOCKER_HUB_PASSWORD
docker build -t $IMAGE_NAME .
docker push $IMAGE_NAME
