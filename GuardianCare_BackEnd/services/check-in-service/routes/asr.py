from flask import Blueprint, request, jsonify
import json
from utils import *

asr_bp = Blueprint('asr', __name__)

@asr_bp.route('/callback', methods=['POST'])
@jwt_required
def asr_callback():
    try:
        data = request.json
        log_message(json.dumps(data, indent=2), prefix="ASR Callback")
        
        transcription = data.get('transcription', '')
        if not transcription:
            log_message("No ASR transcription provided", prefix="Error")
            return jsonify({"status": "error", "message": "No transcription provided"}), 400
        
        headers = {"Authorization": request.headers.get("Authorization")}
        prompt = check_in_prompt(transcription)
        payload = {
            "prompt": prompt,
            "n_predict": LLM_CONTEXT_LENGTH,
            "stream": False,
            "json_schema": llm_grammar()
        }
        payload["callback"] = f"{CHECK_IN_SERVICE_URL}/llm/callback"
        
        response = requests.post(
            f"{LLM_SERVICE_URL}/answer/callback", json=payload, headers=headers)

        if response.status_code != 202:
            raise Exception(
                f"LLM service returned status code {response.status_code}: {response.text}")

        return jsonify({"status": "success", "message": "Transcription is successfully being processed asynchronously"}), 200
    
    except Exception as e:
        log_message(str(e), prefix="Error")
        return jsonify({"status": "error", "message": str(e)}), 500
    
    
# SYNCHRNOUS LLM SERVICE IMPLEMENTATION
# response = requests.post(f"{LLM_SERVICE_URL}/answer", json=payload)

# if response.status_code >= 300:
#     raise Exception(
#         f"LLM service returned status code {response.status_code}: {response.text}")

# result = response.json()
# output_text = result.get("content", "").strip()

# log_message(output_text, prefix="LLM Output")

# return json.loads(output_text)  # Convert to dictionary