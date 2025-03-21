from flask import Blueprint, request, jsonify
import requests
import os
import time
from config import ASR_SERVICE_URL, ASR_CALLBACK_URL
from utils import * 

mobile_bp = Blueprint('mobile', __name__)

@mobile_bp.route('/audio', methods=['POST'])
@jwt_required
def transcribe_audio():
    if 'audio' not in request.files:
        return jsonify({"error": "No file part"}), 400

    raw_file = request.files['audio']
    
    if raw_file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    
    try:
        user_dir = f'./data/{request.user["userId"]}'
        os.makedirs(user_dir, exist_ok=True)    
        raw_filename = "audio.m4a"       
        raw_file_path = f'{user_dir}/{raw_filename}'     
        raw_file.save(raw_file_path)
        final_filename = "audio.wav"       
        final_file_path = f'{user_dir}/{final_filename}'     
        os.system(f'ffmpeg -y -i {raw_file_path} {final_file_path}')
        with open(final_file_path, 'rb') as audio_file:
            headers = {"Authorization": request.headers.get("Authorization")}
            files = {'file': audio_file}
            data = {
                'callback_url': ASR_CALLBACK_URL
            }
            response = requests.post(f"{ASR_SERVICE_URL}/transcribe", files=files, data=data, headers=headers)
        
        if response.status_code != 202:
            raise Exception(f"ASR service returned status code {response.status_code}: {response.text}")
        
        return jsonify({"message": "Audio processing in progress"}), 202
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@mobile_bp.route('/process-text', methods=['POST'])
@jwt_required
def process_text():
    data = request.json
    if not data or 'text' not in data:
        return jsonify({"error": "No text provided"}), 400
    
    try:
        result = process_transcription(data['text'], use_callback=True)
        return jsonify(result), 202
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500
