# GuardianCare Kubernetes Setup

## Overview

This directory contains Kubernetes configuration for deploying the GuardianCare microservices. The setup includes:

- Core services: auth-service, elderly-service, notification-service, envoy
- Additional services: asr-service, llm-service, check-in-service

## Command Usage

### Basic Deployment

```bash
# Windows - Deploy core services only (default)
.\deploy.bat

# Linux/macOS - Deploy core services only (default)
./deploy.sh
```

### Deploy All Services

```bash
# Windows
.\deploy.bat --all

# Linux/macOS
./deploy.sh --all
```

### Deploy Specific Components

```bash
# Deploy only core services
.\deploy.bat --core

# Deploy only namespace
.\deploy.bat --namespace-only

# Deploy only ConfigMaps and Secrets
.\deploy.bat --config-only

# Deploy only Deployments
.\deploy.bat --deployments-only

# Deploy only Services
.\deploy.bat --services-only

# Deploy only HPAs
.\deploy.bat --hpa-only
```

### Delete Resources

```bash
# Delete all resources
.\deploy.bat --delete

# Delete specific pod
.\deploy.bat --delete-pods pod-name

# Delete all pods
.\deploy.bat --delete-pods
```

### Help

```bash
# Show all available options
.\deploy.bat --help
```

## Building Docker Images

```bash
# Windows
.\build-images.bat

# Linux/macOS
./build-images.sh
```

## Checking Deployment Status

```bash
# Check pods
kubectl get pods -n guardiancare

# Check services
kubectl get services -n guardiancare

# Check HPAs
kubectl get hpa -n guardiancare
