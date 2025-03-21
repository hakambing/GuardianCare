import os

SECRET_KEY = os.environ.get("JWT_SECRET", "")

PORT = int(os.environ.get("ASR_SERVICE_PORT", 6001))
MODEL_NAME = os.getenv("ASR_MODEL", "large")
MODEL_PATH = os.getenv("ASR_MODEL_PATH", os.path.join(os.path.expanduser("~"), ".cache", "whisper"))