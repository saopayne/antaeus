#!/bin/bash

# Create a new image version with latest code changes
docker build . --tag pleo-antaeus

# Build the code
docker run --name pleo-anteus-app --rm -it -v pleo-antaeus-build-cache:/home/pleo/.gradle pleo-antaeus
