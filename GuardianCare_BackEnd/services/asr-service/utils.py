from flask import request, jsonify
from functools import wraps
import datetime
import jwt
import numpy as np

from config import *

def log_message(message, prefix=""):
    if prefix:
        print(f"[{prefix}] {message}")
    else:
        print(f"[LOG] {message}")

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
