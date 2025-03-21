#!/bin/bash

# GuardianCare Docker Image Build Script
# This script builds Docker images for all services and tags them for Kubernetes

# Set the base directory
BASE_DIR="/d/Code/GuardianCare_BackEnd"
echo "Base directory: $BASE_DIR"

# Function to display usage information
usage() {
  echo "Usage: $0 [options]"
  echo "Options:"
  echo "  -h, --help                 Display this help message"
  echo "  -s, --service SERVICE      Build only the specified service"
  echo "  -a, --all                  Build all services (default)"
  echo "  -f, --force                Force rebuild of images (no cache)"
  exit 1
}

# Parse command line arguments
BUILD_ALL=true
SERVICE=""
FORCE_REBUILD=false

while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    -h|--help)
      usage
      ;;
    -s|--service)
      SERVICE="$2"
      BUILD_ALL=false
      shift
      shift
      ;;
    -a|--all)
      BUILD_ALL=true
      shift
      ;;
    -f|--force)
      FORCE_REBUILD=true
      shift
      ;;
    *)
      echo "Unknown option: $key"
      usage
      ;;
  esac
done

# Function to build a service
build_service() {
  local service_name="$1"
  local service_dir="$BASE_DIR/services/$service_name"
  
  if [ ! -d "$service_dir" ]; then
    echo "Error: Service directory not found: $service_dir"
    return 1
  fi
  
  if [ ! -f "$service_dir/Dockerfile" ]; then
    echo "Error: Dockerfile not found for service: $service_name"
    return 1
  fi
  
  echo "Building Docker image for $service_name..."
  
  # Check if we should force rebuild
  if [ "$FORCE_REBUILD" = true ]; then
    echo "Forcing rebuild (no cache)..."
    docker build --no-cache -t "$service_name:latest" "$service_dir"
  else
    docker build -t "$service_name:latest" "$service_dir"
  fi
  
  if [ $? -eq 0 ]; then
    echo "Successfully built image: $service_name:latest"
  else
    echo "Failed to build image for $service_name"
    return 1
  fi
  
  return 0
}

# Build services
if [ "$BUILD_ALL" = true ]; then
  echo "Building all services..."
  
  # Build auth-service
  build_service "auth-service"
  
  # Build elderly-management-service
  build_service "elderly-management-service"
  # Tag it as elderly-service for Kubernetes
  docker tag elderly-management-service:latest elderly-service:latest
  
  # Build notification-service
  build_service "notification-service"
  
  # These services are currently commented out in the Kubernetes config
  # # Build asr-service
  # build_service "asr-service"
  
  # # Build llm-service
  # build_service "llm-service"
  
  # # Build check-in-service
  # build_service "check-in-service"
  
  echo "All services built successfully"
else
  echo "Building service: $SERVICE"
  build_service "$SERVICE"
fi

echo "Docker images ready for Kubernetes deployment"
echo "To deploy to Kubernetes, run: ./deploy.sh"
