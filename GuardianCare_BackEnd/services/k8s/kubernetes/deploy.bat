@echo off
REM GuardianCare Kubernetes Deployment Script for Windows

REM Set the namespace
set NAMESPACE=guardiancare

REM Parse command line arguments
set DEPLOY_ALL=
set DEPLOY_CORE=
set NAMESPACE_ONLY=
set CONFIG_ONLY=
set DEPLOYMENTS_ONLY=
set SERVICES_ONLY=
set HPA_ONLY=
set DELETE=

:parse_args
if "%~1"=="" goto check_args
if "%~1"=="-h" goto usage
if "%~1"=="--help" goto usage
if "%~1"=="-n" (
    set NAMESPACE=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--namespace" (
    set NAMESPACE=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-a" (
    set DEPLOY_ALL=true
    shift
    goto parse_args
)
if "%~1"=="--all" (
    set DEPLOY_ALL=true
    shift
    goto parse_args
)
if "%~1"=="-c" (
    set DEPLOY_CORE=true
    shift
    goto parse_args
)
if "%~1"=="--core" (
    set DEPLOY_CORE=true
    shift
    goto parse_args
)
if "%~1"=="--namespace-only" (
    set NAMESPACE_ONLY=true
    shift
    goto parse_args
)
if "%~1"=="--config-only" (
    set CONFIG_ONLY=true
    shift
    goto parse_args
)
if "%~1"=="--deployments-only" (
    set DEPLOYMENTS_ONLY=true
    shift
    goto parse_args
)
if "%~1"=="--services-only" (
    set SERVICES_ONLY=true
    shift
    goto parse_args
)
if "%~1"=="--hpa-only" (
    set HPA_ONLY=true
    shift
    goto parse_args
)
if "%~1"=="--delete" (
    set DELETE=true
    shift
    goto parse_args
)
if "%~1"=="--delete-pods" (
    set DELETE_PODS=true
    set POD_NAME=%~2
    shift
    if not "%~1"=="" if not "%~1:~0,1%"=="-" shift
    goto parse_args
)
echo Unknown option: %~1
goto usage

:check_args
REM Set default if no specific option is provided
if not defined DEPLOY_ALL (
    if not defined DEPLOY_CORE (
        if not defined NAMESPACE_ONLY (
            if not defined CONFIG_ONLY (
                if not defined DEPLOYMENTS_ONLY (
                    if not defined SERVICES_ONLY (
                        if not defined HPA_ONLY (
                            if not defined DELETE (
                                if not defined DELETE_PODS (
                                    set DEPLOY_CORE=true
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

REM Check if kubectl is installed
kubectl version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: kubectl is not installed or not in the PATH
    exit /b 1
)

REM Check if the Kubernetes cluster is accessible
kubectl cluster-info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Unable to connect to Kubernetes cluster
    exit /b 1
)

REM Delete all resources if requested
if defined DELETE (
    call :delete_all
    goto end
)

REM Delete pods if requested
if defined DELETE_PODS (
    call :delete_pods "%POD_NAME%"
    goto end
)

REM Deploy based on options
if defined NAMESPACE_ONLY (
    call :deploy_namespace
) else if defined CONFIG_ONLY (
    call :deploy_config
) else if defined DEPLOYMENTS_ONLY (
    if defined DEPLOY_ALL (
        call :deploy_all_deployments
    ) else (
        call :deploy_core_deployments
    )
) else if defined SERVICES_ONLY (
    if defined DEPLOY_ALL (
        call :deploy_all_services
    ) else (
        call :deploy_core_services
    )
) else if defined HPA_ONLY (
    if defined DEPLOY_ALL (
        call :deploy_all_hpa
    ) else (
        call :deploy_core_hpa
    )
) else if defined DEPLOY_ALL (
    call :deploy_namespace
    call :deploy_config
    call :deploy_all_deployments
    call :deploy_all_services
    call :deploy_all_hpa
) else if defined DEPLOY_CORE (
    call :deploy_namespace
    call :deploy_config
    call :deploy_core_deployments
    call :deploy_core_services
    call :deploy_core_hpa
)

echo Deployment completed successfully
echo To check the status of your pods, run: kubectl get pods -n %NAMESPACE%
echo To check the status of your services, run: kubectl get services -n %NAMESPACE%
echo To check the status of your HPAs, run: kubectl get hpa -n %NAMESPACE%
goto end

:deploy_namespace
echo Creating namespace: %NAMESPACE%
kubectl apply -f 00-namespace.yaml
echo Namespace created successfully
exit /b 0

:deploy_config
echo Deploying ConfigMaps and Secrets
kubectl apply -f 01-configmaps.yaml
kubectl apply -f 02-secrets.yaml
echo ConfigMaps and Secrets deployed successfully
exit /b 0

:deploy_all_deployments
echo Deploying all services
kubectl apply -f 03-deployments/
echo Services deployed successfully
exit /b 0

:deploy_core_deployments
echo Deploying core services
kubectl apply -f 03-deployments/auth-service.yaml
kubectl apply -f 03-deployments/elderly-service.yaml
kubectl apply -f 03-deployments/envoy.yaml
kubectl apply -f 03-deployments/notification-service.yaml
echo Core services deployed successfully
exit /b 0

:deploy_all_services
echo Deploying all service endpoints
kubectl apply -f 04-services/
echo Service endpoints deployed successfully
exit /b 0

:deploy_core_services
echo Deploying core service endpoints
kubectl apply -f 04-services/auth-service.yaml
kubectl apply -f 04-services/elderly-service.yaml
kubectl apply -f 04-services/envoy.yaml
kubectl apply -f 04-services/notification-service.yaml
echo Core service endpoints deployed successfully
exit /b 0

:deploy_all_hpa
echo Deploying all Horizontal Pod Autoscalers
kubectl apply -f 05-hpa/
echo HPAs deployed successfully
exit /b 0

:deploy_core_hpa
echo Deploying core Horizontal Pod Autoscalers
kubectl apply -f 05-hpa/auth-service-hpa.yaml
kubectl apply -f 05-hpa/elderly-service-hpa.yaml
kubectl apply -f 05-hpa/notification-service-hpa.yaml
echo Core HPAs deployed successfully
exit /b 0

:delete_all
echo Deleting all resources in namespace: %NAMESPACE%
kubectl delete -f 05-hpa/ --ignore-not-found
kubectl delete -f 04-services/ --ignore-not-found
kubectl delete -f 03-deployments/ --ignore-not-found
kubectl delete -f 02-secrets.yaml --ignore-not-found
kubectl delete -f 01-configmaps.yaml --ignore-not-found
kubectl delete -f 00-namespace.yaml --ignore-not-found
echo All resources deleted successfully
exit /b 0

:delete_pods
if "%~1"=="" (
    echo Deleting all pods in namespace: %NAMESPACE%
    kubectl delete pods --all -n %NAMESPACE%
    echo All pods deleted successfully
) else (
    echo Deleting pod: %~1 in namespace: %NAMESPACE%
    kubectl delete pod %~1 -n %NAMESPACE%
    echo Pod %~1 deleted successfully
)
exit /b 0

:usage
echo Usage: %0 [options]
echo Options:
echo   -h, --help                 Display this help message
echo   -n, --namespace NAME       Set the namespace (default: guardiancare)
echo   -a, --all                  Deploy all services (including ASR, LLM, and Check-in)
echo   -c, --core                 Deploy only core services (Auth, Elderly, Envoy, Notification)
echo   --namespace-only           Deploy only the namespace
echo   --config-only              Deploy only ConfigMaps and Secrets
echo   --deployments-only         Deploy only Deployments
echo   --services-only            Deploy only Services
echo   --hpa-only                 Deploy only HPAs
echo   --delete                   Delete all resources in the namespace
echo   --delete-pods [POD_NAME]   Delete all pods or a specific pod
exit /b 1

:end
