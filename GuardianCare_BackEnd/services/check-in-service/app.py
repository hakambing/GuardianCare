from flask import Flask
from flask_cors import CORS
from routes.m5stick import m5stick_bp
from routes.mobile import mobile_bp
from routes.asr import asr_bp
from routes.llm import llm_bp
from config import *

app = Flask(__name__)
CORS(app)

app.register_blueprint(m5stick_bp, url_prefix="/m5stick")
app.register_blueprint(mobile_bp, url_prefix="/mobile")
app.register_blueprint(asr_bp, url_prefix="/asr")
app.register_blueprint(llm_bp, url_prefix="/llm")

if __name__ == '__main__':
    app.run(host=FLASK_HOST, port=FLASK_PORT,
            threaded=FLASK_THREADED, debug=FLASK_DEBUG)
