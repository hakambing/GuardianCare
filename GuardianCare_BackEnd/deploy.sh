#!/bin/bash

DIR="./services/k8s"
cd "$DIR"

# Function to redeploy the service
deploy_service() {
    echo "Deploying $SERVICE_NAME..."
    
    docker compose stop "$SERVICE_NAME"
    docker compose rm -f "$SERVICE_NAME"
    docker compose build "$SERVICE_NAME"
    docker compose up -d "$SERVICE_NAME"

    echo "$SERVICE_NAME deployed successfully!"

    docker compose logs -t -f "$SERVICE_NAME"
}

# Watch for file changes in the build context directory (if applicable)
if [ -z "$1" ]; then
    echo "Deploying all services..."
    docker compose down
    docker compose build
    docker compose up
    # docker compose -f docker-compose.no-llm.yml build
    # docker compose -f docker-compose.no-llm.yml up
else
    SERVICE_NAME=$1
    deploy_service

    # BUILD_CONTEXT=$(grep -A 2 "$SERVICE_NAME:" docker-compose.yml | grep "context:" | awk '{print $2}')

    # if [ -n "$BUILD_CONTEXT" ]; then
    #     echo "Monitoring changes in: $BUILD_CONTEXT"
    # else
    #     echo "No build context found for $SERVICE_NAME, using docker-compose up only."
    # fi

    # if [ -n "$BUILD_CONTEXT" ]; then
    #     echo "Watching for changes in $BUILD_CONTEXT..."
    #     inotifywait -m -r -e modify,create,delete "$BUILD_CONTEXT" |
    #     while read path action file; do
    #         echo "Change detected in $file. Redeploying $SERVICE_NAME..."
    #         deploy_service
    #     done
    # else
    #     # Just redeploy once if there's no local build context
    #     deploy_service
    # fi
fi
