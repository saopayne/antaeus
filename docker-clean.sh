#!/bin/bash

# Remove the docker volume that stores cached build artifacts.
# This also stops and removes any container using the volume.
echo -n 'Clearing build cache: '
docker volume remove -f pleo-antaeus-build-cache

# Remove all pleo-antaeus images.
for image in $(docker images --quiet --filter="reference=pleo-antaeus:*"); do
  docker rmi "$image"
done
