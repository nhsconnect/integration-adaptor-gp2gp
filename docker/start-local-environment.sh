#!/usr/bin/env bash
set -e

pushd mock-mhs-service && ./build-service-image.sh
popd

pushd service && ./build-service-image.sh
popd

docker-compose down
docker-compose up

