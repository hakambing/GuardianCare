from flask import Blueprint, request, jsonify
import json
import requests
from utils import *

llm_bp = Blueprint('llm', __name__)

@llm_bp.route('/callback', methods=['POST'])
@jwt_required
def llm_callback():
    try:
        raw_data = request.get_data().decode("utf-8")
        parsed_data = json.loads(raw_data)
        raw_content = parsed_data.get("content", "")
        content_dict = json.loads(raw_content)
        headers = {"Content-Type": "application/json"}
        data = {
            "elderly_id": request.user["userId"],
            "summary": content_dict["summary"],
            "priority": content_dict["priority"],
            "mood": content_dict["mood"],
            "status": content_dict["status"],
            "transcript": content_dict["transcript"],
            "created_at": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "updated_at": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
        }
        log_message(data, prefix="LLM Check-in Data (DB)")
        response = requests.post(f"{AUTH_SERVICE_URL}/api/checkins", json=data, headers=headers)
        if response.status_code >= 300:
            raise Exception(
                f"LLM service returned status code {response.status_code}: {response.text}")
                
        return jsonify({"status": "success", "message": "Check-in processed", "result": content_dict}), 200
    
    except Exception as e:
        log_message(str(e), "LLM Callback Error")
        return jsonify({"status": "error", "message": str(e)}), 500
