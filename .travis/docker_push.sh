#!/usr/bin/env bash
set -e

IMAGE_NAME="${DOCKER_IMAGE_NAME}:${TRAVIS_TAG}-${KEYCLOAK_VERSION}"

docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}
docker build -t ${IMAGE_NAME} .
docker push ${IMAGE_NAME}

if [ "${IS_LATEST}" = true ]
then
    LATEST_IMAGE_NAME="${DOCKER_IMAGE_NAME}:latest"
    docker tag ${IMAGE_NAME} ${LATEST_IMAGE_NAME}
    docker push ${LATEST_IMAGE_NAME}
fi
