@echo off
echo Building Docker images for GuardianCare services...

set BASE_DIR=D:\Code\GuardianCare_BackEnd
echo Base directory: %BASE_DIR%

echo Building auth-service...
docker build -t auth-service:latest "%BASE_DIR%\services\auth-service"

echo Building elderly-management-service...
docker build -t elderly-management-service:latest "%BASE_DIR%\services\elderly-management-service"
echo Tagging as elderly-service:latest
docker tag elderly-management-service:latest elderly-service:latest

echo Building notification-service...
docker build -t notification-service:latest "%BASE_DIR%\services\notification-service"

echo All services built successfully
echo Docker images ready for Kubernetes deployment
echo To deploy to Kubernetes, run: deploy.bat
