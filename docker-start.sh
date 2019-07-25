#!/bin/sh

# Create a new image version with latest code changes.
docker build . --tag pleo-antaeus

# Build the code.
docker run \
  --publish 7000:7000 \
  --rm \
  --interactive \
  --tty \
  pleo-antaeus
