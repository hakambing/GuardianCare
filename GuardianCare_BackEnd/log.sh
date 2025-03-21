#!/bin/bash

DIR="./services/k8s"
cd "$DIR"

if [ -z "$1" ]; then
    echo "Please enter a service name."
else
    SERVICE_NAME=$1
    docker compose logs -t -f "$SERVICE_NAME"
fi