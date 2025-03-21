FROM python:3.10-slim-buster

# Set the working directory within the container
WORKDIR /app

# Copy the application code into the container
COPY . /app

RUN apt-get update && \
    apt-get install -y \
    build-essential git ffmpeg

# Create data directory for audio files
RUN mkdir -p /app/data

RUN pip install --no-cache-dir -r requirements.txt

EXPOSE 6001

CMD ["python", "-u", "/app/app.py"]

