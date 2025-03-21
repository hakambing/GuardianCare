FROM python:3.10-slim-buster

WORKDIR /app

COPY . /app

RUN apt-get update && \
    apt-get install -y \
    build-essential git ffmpeg

RUN pip install --upgrade pip

RUN pip install -r /app/requirements.txt  --verbose

EXPOSE 6000

CMD ["python", "-u", "/app/app.py"]




# # ------------------- Stage 1: Build Stage ------------------------------
#     FROM python:3.10 AS builder

#     WORKDIR /app
    
#     COPY . /app
    
#     RUN apt-get update && \
#         apt-get install -y \
#         build-essential git ffmpeg
    
#     RUN pip install --no-cache-dir -r requirements.txt
    
#     # ------------------- Stage 2: Final Stage ------------------------------
    
#     FROM python:3.10-slim
    
#     WORKDIR /app
    
#     COPY --from=builder /usr/local/lib/python3.10/site-packages/ /usr/local/lib/python3.10/site-packages/
    
#     COPY --from=builder /usr/local/bin/ffmpeg /usr/local/bin/
    
#     COPY --from=builder /app /app
    
#     EXPOSE 6000
    
#     CMD ["python", "-u", "/app/app.py"]