#!/bin/bash

set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPS_DIR="$APP_DIR/deps"
BUILD_DIR="$APP_DIR/build"
INSTALL_DIR="$APP_DIR/full"
LIB_DIR="$APP_DIR/lib"
GGML_CPU_ARM_ARCH="armv8-a"

mkdir -p "$DEPS_DIR" "$BUILD_DIR" "$INSTALL_DIR" "$LIB_DIR"

echo "Building jwt-cpp..."
cd "$DEPS_DIR/jwt-cpp"
cmake . -DCMAKE_INSTALL_PREFIX="$INSTALL_DIR"
cmake --build .
cmake --install .

cd "$APP_DIR"

ARCH=$(uname -m)
if [ "$ARCH" = "amd64" ]; then
    echo "Building for x86_64 (amd64)..."
    cmake -S . -B "$BUILD_DIR" -DCMAKE_BUILD_TYPE=Release -DLLAMA_CURL=ON -DGGML_NATIVE=OFF -DGGML_BACKEND_DL=ON -DGGML_CPU_ALL_VARIANTS=ON -DCMAKE_PREFIX_PATH="$INSTALL_DIR"
elif [ "$ARCH" = "arm64" ]; then
    echo "Building for ARM64..."
    cmake -S . -B "$BUILD_DIR" -DCMAKE_BUILD_TYPE=Release -DLLAMA_CURL=ON -DGGML_NATIVE=OFF -DGGML_CPU_ARM_ARCH=${GGML_CPU_ARM_ARCH} -DCMAKE_PREFIX_PATH="$INSTALL_DIR"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

echo "Compiling the application..."
cmake --build "$BUILD_DIR" -j

echo "Copying libraries..."
mkdir -p "$LIB_DIR"
find "$BUILD_DIR" -name "*.so" -exec cp {} "$LIB_DIR" \;

echo "Copying binaries..."
mkdir -p "$INSTALL_DIR"
cp "$BUILD_DIR/bin/"* "$INSTALL_DIR"

echo "Running the application..."
cd "$INSTALL_DIR"
./server -m $APP_DIR/models/Mistral-7B-Instruct-v0.3.Q8_0.gguf -c 8192
# ./server -m $APP_DIR/models/qwen2.5-14b-instruct-q5_k_m-00001-of-00003.gguf -c 8192

