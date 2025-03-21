import os

# Configurations
FLASK_HOST = "0.0.0.0"                                              # Flask host
FLASK_PORT = os.environ.get("CHECK_IN_SERVICE_PORT", 6000)                                                # Flask port
FLASK_THREADED = True                                               # Flask threaded option
FLASK_DEBUG = True                                                  # Flask debug option
M5STICK_MIC_GAIN = 12
M5STICK_MIC_SAMPLE_RATE = 4000
LLM_CONTEXT_LENGTH = int(os.environ.get("LLM_CONTEXT_SIZE", 512))

SECRET_KEY = os.environ.get("JWT_SECRET", "")

# Service URLs - can be overridden with environment variables
AUTH_SERVICE_URL = os.environ.get("AUTH_SERVICE_URL", "http://auth-service:3000")
NOTIFICATION_SERVICE_URL = os.environ.get("NOTIFICATION_SERVICE_URL", "http://notification-service:3002")
CHECK_IN_SERVICE_URL = os.environ.get("CHECK_IN_SERVICE_URL", "http://check-in-service:6000")
ASR_SERVICE_URL = os.environ.get("ASR_SERVICE_URL", "http://asr-service:6001")
LLM_SERVICE_URL = os.environ.get("LLM_SERVICE_URL", "http://llm-service:6002")

ASR_CALLBACK_URL = f"{CHECK_IN_SERVICE_URL}/asr/callback"
LLM_CALLBACK_URL = f"{CHECK_IN_SERVICE_URL}/llm/callback"
