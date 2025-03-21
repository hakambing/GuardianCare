from flask import request, jsonify
from functools import wraps
import requests
import datetime
import wave
import jwt
import json
import numpy as np

from config import *

def log_message(message, prefix=""):
    if prefix:
        print(f"[{prefix}] {message}")
    else:
        print(f"[LOG] {message}")


def convert_raw_to_wav(raw_data, wav_file_path="./data/audio.wav", gain=M5STICK_MIC_GAIN, sample_rate=M5STICK_MIC_SAMPLE_RATE, target_sample_rate=16000):
    audio_data = np.frombuffer(raw_data, dtype=np.int16)
    audio_data = np.clip(audio_data * gain, -32768, 32767).astype(np.int16)

    with wave.open(wav_file_path, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(sample_rate)  # Sample rate
        wav_file.writeframes(audio_data.tobytes())

    return wav_file_path


def llm_grammar():
    return json.loads('''
    {
        "type": "object",
        "properties": {
            "summary": {
                "type": "string",
                "minLength": 1,
                "maxLength": 300
            },
            "priority": {
                "type": "integer",
                "minimum": 0,
                "maximum": 4
            },
            "mood": {
                "type": "integer",
                "minimum": -3,
                "maximum": 3
            },
            "status": {
                "type": "string",
                "minLength": 1,
                "maxLength": 30
            },
            "transcript": {
                "type": "string"
            }
        },
        "required": ["summary", "priority", "mood", "status", "transcript"],
        "additionalProperties": false
    }
    ''')


def check_in_prompt(transcription, prompt_file_path="prompt.md"):
    try:
        with open(prompt_file_path, "r", encoding="utf-8") as f:
            system_prompt = f.read()
    except Exception as e:
        print(f"Error reading {prompt_file_path}: {e}")
        raise Exception("Failed to read prompt file")

    user_prompt = f"nUser's transcription:\n\"{transcription}\""
    
    return f"""
    [INST]
    <<SYS>>
    {system_prompt}
    <</SYS>>
    {user_prompt}
    [/INST]
    """



def generate_jwt(payload, expires_in=3600):
    payload["exp"] = datetime.datetime.utcnow(
    ) + datetime.timedelta(seconds=expires_in)
    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")


def verify_jwt(token):
    try:
        return jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        return {"error": "Token has expired"}
    except jwt.InvalidTokenError:
        return {"error": "Invalid token"}


def jwt_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get("Authorization")

        if not auth_header or not auth_header.startswith("Bearer "):
            return jsonify({"error": "Authorization header required"}), 401

        token = auth_header.split(" ")[1]
        decoded_token = verify_jwt(token)

        if "error" in decoded_token:
            return jsonify(decoded_token), 401

        request.user = decoded_token
        return f(*args, **kwargs)

    return decorated_function
