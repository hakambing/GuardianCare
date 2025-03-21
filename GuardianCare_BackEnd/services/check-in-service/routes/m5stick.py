import os
import requests
from pathlib import Path
from flask import Blueprint, request, Response, jsonify
from utils import *
from config import ASR_SERVICE_URL, ASR_CALLBACK_URL

m5stick_bp = Blueprint('m5stick', __name__)

@m5stick_bp.route('/audio/stream', methods=['POST'])
@jwt_required
def stream():
    try:
        data = request.get_data()
        log_message(f"Got {len(data)} I2S bytes", prefix="M5Stick Audio Stream")
        
        output_path = f'./data/{request.user["userId"]}/i2s.raw'
        output_file = Path(output_path)
        output_file.parent.mkdir(exist_ok=True, parents=True)
        with open(output_path, 'ab') as f:
            f.write(data)
        
        return 'OK'
            
    except Exception as e:
        log_message(str(e), prefix="M5Stick Stream Error")
        return Response(str(e), status=500)

@m5stick_bp.route('/audio/stop', methods=['POST'])
@jwt_required
def end_stream():    
    try:
        output_path = f'./data/{request.user["userId"]}/i2s.raw'
        output_file = Path(output_path)
        output_file.parent.mkdir(exist_ok=True, parents=True)
        with open(output_path, 'rb') as f:
            raw_data = f.read()
        wav_file_path = convert_raw_to_wav(raw_data, wav_file_path=f'./data/{request.user["userId"]}/audio.wav', gain = M5STICK_MIC_GAIN)
        os.remove(output_path)
            
        log_message("Sending audio to ASR service for transcription...", prefix="M5Stick Audio Stop")
        
        with open(wav_file_path, 'rb') as wav_file:
            headers = {"Authorization": request.headers.get("Authorization")}
            files = {'file': wav_file}
            data = {'callback_url': ASR_CALLBACK_URL}
            response = requests.post(f"{ASR_SERVICE_URL}/transcribe", files=files, data=data, headers=headers)
        
        if response.status_code != 202:
            raise Exception(f"ASR service returned status code {response.status_code}: {response.text}")
        
        return jsonify({"message": "Audio processing in progress"}), 202
        
    except Exception as e:
        log_message(str(e), prefix="M5Stick Stop Stream Error")
        return Response(str(e), status=500)
    
    
@m5stick_bp.route('/fall', methods=['POST'])
@jwt_required
def fall():    
    try:
        headers = {"Content-Type": "application/json"}
        data = {
            "elderly_id": request.user["userId"],
            "summary": "User's fall tracker has detected a fall.",
            "priority": 4,
            "mood": -3,
            "status": "fall detected",
            "transcript": None,
            "created_at": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "updated_at": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
        }
        log_message(data, prefix="M5StickC Fall")
        response = requests.post(f"{AUTH_SERVICE_URL}/api/checkins", json=data, headers=headers)
        if response.status_code >= 300:
            raise Exception(
                f"M5StickC Fall returned status code {response.status_code}: {response.text}")
                
        return jsonify({"status": "success", "message": "Check-in processed", "result": "Fall successfully logged"}), 200
    
    except Exception as e:
        log_message(str(e), "M5StickC Fall Error")
        return jsonify({"status": "error", "message": str(e)}), 500
    
    
@m5stick_bp.route('/emergency', methods=['POST'])
@jwt_required
def emergency():    
    try:
        headers = {"Content-Type": "application/json"}
        data = {
            "elderly_id": request.user["userId"],
            "summary": "User has reported an emergency on their fall tracker.",
            "priority": 4,
            "mood": -3,
            "status": "Emergency",
            "transcript": None,
            "created_at": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "updated_at": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
        }
        log_message(data, prefix="M5StickC Emergency")
        response = requests.post(f"{AUTH_SERVICE_URL}/api/checkins", json=data, headers=headers)
        if response.status_code >= 300:
            raise Exception(
                f"M5StickC Emergency returned status code {response.status_code}: {response.text}")
                
        return jsonify({"status": "success", "message": "Check-in processed", "result": "Emergency successfully logged"}), 200
    
    except Exception as e:
        log_message(str(e), "M5StickC Emergency Error")
        return jsonify({"status": "error", "message": str(e)}), 500
