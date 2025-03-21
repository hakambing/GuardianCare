#!/bin/bash

# GuardianCare Kubernetes Deployment Script

# Set the namespace
NAMESPACE="guardiancare"

# Function to display usage information
usage() {
  echo "Usage: $0 [options]"
  echo "Options:"
  echo "  -h, --help                 Display this help message"
  echo "  -n, --namespace NAME       Set the namespace (default: guardiancare)"
  echo "  -a, --all                  Deploy all services (including ASR, LLM, and Check-in)"
  echo "  -c, --core                 Deploy only core services (Auth, Elderly, Envoy, Notification)"
  echo "  --namespace-only           Deploy only the namespace"
  echo "  --config-only              Deploy only ConfigMaps and Secrets"
  echo "  --deployments-only         Deploy only Deployments"
  echo "  --services-only            Deploy only Services"
  echo "  --hpa-only                 Deploy only HPAs"
  echo "  --delete                   Delete all resources in the namespace"
  echo "  --delete-pods [POD_NAME]   Delete all pods or a specific pod"
  exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    -h|--help)
      usage
      ;;
    -n|--namespace)
      NAMESPACE="$2"
      shift
      shift
      ;;
    -a|--all)
      DEPLOY_ALL=true
      shift
      ;;
    -c|--core)
      DEPLOY_CORE=true
      shift
      ;;
    --namespace-only)
      NAMESPACE_ONLY=true
      shift
      ;;
    --config-only)
      CONFIG_ONLY=true
      shift
      ;;
    --deployments-only)
      DEPLOYMENTS_ONLY=true
      shift
      ;;
    --services-only)
      SERVICES_ONLY=true
      shift
      ;;
    --hpa-only)
      HPA_ONLY=true
      shift
      ;;
    --delete)
      DELETE=true
      shift
      ;;
    --delete-pods)
      DELETE_PODS=true
      POD_NAME="$2"
      shift
      if [[ "$1" != -* && "$1" != "" ]]; then
        shift
      fi
      ;;
    *)
      echo "Unknown option: $key"
      usage
      ;;
  esac
done

# Set default if no specific option is provided
if [[ -z $DEPLOY_ALL && -z $DEPLOY_CORE && -z $NAMESPACE_ONLY && -z $CONFIG_ONLY && -z $DEPLOYMENTS_ONLY && -z $SERVICES_ONLY && -z $HPA_ONLY && -z $DELETE && -z $DELETE_PODS ]]; then
  DEPLOY_CORE=true
fi

# Function to check if kubectl is installed
check_kubectl() {
  if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl is not installed or not in the PATH"
    exit 1
  fi
}

# Function to check if the Kubernetes cluster is accessible
check_cluster() {
  if ! kubectl cluster-info &> /dev/null; then
    echo "Error: Unable to connect to Kubernetes cluster"
    exit 1
  fi
}

# Function to deploy namespace
deploy_namespace() {
  echo "Creating namespace: $NAMESPACE"
  kubectl apply -f 00-namespace.yaml
  echo "Namespace created successfully"
}

# Function to deploy ConfigMaps and Secrets
deploy_config() {
  echo "Deploying ConfigMaps and Secrets"
  kubectl apply -f 01-configmaps.yaml
  kubectl apply -f 02-secrets.yaml
  echo "ConfigMaps and Secrets deployed successfully"
}

# Function to deploy all Deployments
deploy_all_deployments() {
  echo "Deploying all services"
  kubectl apply -f 03-deployments/
  echo "Services deployed successfully"
}

# Function to deploy core Deployments
deploy_core_deployments() {
  echo "Deploying core services"
  kubectl apply -f 03-deployments/auth-service.yaml
  kubectl apply -f 03-deployments/elderly-service.yaml
  kubectl apply -f 03-deployments/envoy.yaml
  kubectl apply -f 03-deployments/notification-service.yaml
  echo "Core services deployed successfully"
}

# Function to deploy all Services
deploy_all_services() {
  echo "Deploying all service endpoints"
  kubectl apply -f 04-services/
  echo "Service endpoints deployed successfully"
}

# Function to deploy core Services
deploy_core_services() {
  echo "Deploying core service endpoints"
  kubectl apply -f 04-services/auth-service.yaml
  kubectl apply -f 04-services/elderly-service.yaml
  kubectl apply -f 04-services/envoy.yaml
  kubectl apply -f 04-services/notification-service.yaml
  echo "Core service endpoints deployed successfully"
}

# Function to deploy all HPAs
deploy_all_hpa() {
  echo "Deploying all Horizontal Pod Autoscalers"
  kubectl apply -f 05-hpa/
  echo "HPAs deployed successfully"
}

# Function to deploy core HPAs
deploy_core_hpa() {
  echo "Deploying core Horizontal Pod Autoscalers"
  kubectl apply -f 05-hpa/auth-service-hpa.yaml
  kubectl apply -f 05-hpa/elderly-service-hpa.yaml
  kubectl apply -f 05-hpa/notification-service-hpa.yaml
  echo "Core HPAs deployed successfully"
}

# Function to delete all resources
delete_all() {
  echo "Deleting all resources in namespace: $NAMESPACE"
  kubectl delete -f 05-hpa/ --ignore-not-found
  kubectl delete -f 04-services/ --ignore-not-found
  kubectl delete -f 03-deployments/ --ignore-not-found
  kubectl delete -f 02-secrets.yaml --ignore-not-found
  kubectl delete -f 01-configmaps.yaml --ignore-not-found
  kubectl delete -f 00-namespace.yaml --ignore-not-found
  echo "All resources deleted successfully"
}

# Function to delete pods
delete_pods() {
  if [[ -z $1 ]]; then
    echo "Deleting all pods in namespace: $NAMESPACE"
    kubectl delete pods --all -n $NAMESPACE
    echo "All pods deleted successfully"
  else
    echo "Deleting pod: $1 in namespace: $NAMESPACE"
    kubectl delete pod $1 -n $NAMESPACE
    echo "Pod $1 deleted successfully"
  fi
}

# Main execution
check_kubectl
check_cluster

if [[ $DELETE == true ]]; then
  delete_all
  exit 0
fi

if [[ $DELETE_PODS == true ]]; then
  delete_pods "$POD_NAME"
  exit 0
fi

if [[ $NAMESPACE_ONLY == true ]]; then
  deploy_namespace
elif [[ $CONFIG_ONLY == true ]]; then
  deploy_config
elif [[ $DEPLOYMENTS_ONLY == true ]]; then
  if [[ $DEPLOY_ALL == true ]]; then
    deploy_all_deployments
  else
    deploy_core_deployments
  fi
elif [[ $SERVICES_ONLY == true ]]; then
  if [[ $DEPLOY_ALL == true ]]; then
    deploy_all_services
  else
    deploy_core_services
  fi
elif [[ $HPA_ONLY == true ]]; then
  if [[ $DEPLOY_ALL == true ]]; then
    deploy_all_hpa
  else
    deploy_core_hpa
  fi
elif [[ $DEPLOY_ALL == true ]]; then
  deploy_namespace
  deploy_config
  deploy_all_deployments
  deploy_all_services
  deploy_all_hpa
elif [[ $DEPLOY_CORE == true ]]; then
  deploy_namespace
  deploy_config
  deploy_core_deployments
  deploy_core_services
  deploy_core_hpa
fi

echo "Deployment completed successfully"
echo "To check the status of your pods, run: kubectl get pods -n $NAMESPACE"
echo "To check the status of your services, run: kubectl get services -n $NAMESPACE"
echo "To check the status of your HPAs, run: kubectl get hpa -n $NAMESPACE"
