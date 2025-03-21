from flask import Flask, request, jsonify
import tempfile
import os
import whisper
import threading
import requests

from config import MODEL_NAME, MODEL_PATH, PORT
from utils import *

app = Flask(__name__)

model = whisper.load_model(name=MODEL_NAME, download_root=MODEL_PATH)

def process_transcription(file_path, callback_url, auth_header):
    try:
        result = model.transcribe(file_path, fp16=False, language="en")
        transcription = result["text"]
        headers = {"Authorization": auth_header}
        data = {"transcription": transcription}
        response = requests.post(callback_url, json=data, headers=headers)
        
        print(f"Callback Response: {response.status_code} - {response.text}")

    except Exception as e:
        print(f"Error in transcription: {e}")

    finally:
        if os.path.exists(file_path):
            os.remove(file_path)

@app.route('/transcribe', methods=['POST'])
@jwt_required
def transcribe():
    auth_header = request.headers.get("Authorization")
    
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    
    callback_url = request.form.get("callback_url")
    
    if not callback_url:
        return jsonify({"error": "Missing callback_url"}), 400
    
    temp_dir = tempfile.mkdtemp()
    temp_path = os.path.join(temp_dir, "audio.wav")
    
    try:
        file.save(temp_path)
        threading.Thread(target=process_transcription, args=(temp_path, callback_url, auth_header)).start()

        return jsonify({"message": "Transcription in progress"}), 202  # 202 Accepted
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=PORT)
