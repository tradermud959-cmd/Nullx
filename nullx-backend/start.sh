#!/data/data/com.termux/files/usr/bin/bash

echo "Starting NullX Backend..."

echo "Checking Ollama..."
if curl -s -f -o /dev/null "http://127.0.0.1:11434/"; then
    echo "Ollama Ready"
else
    echo "Ollama is not running. Attempting to start Ollama in the background..."
    ollama serve > /dev/null 2>&1 &
    sleep 3
    if curl -s -f -o /dev/null "http://127.0.0.1:11434/"; then
        echo "Ollama Ready"
    else
        echo "[ERROR] Failed to start Ollama. Please start it manually."
    fi
fi

export MODEL="llama3.2:1b"
echo "Loading model $MODEL"

# Determine local IP (for Termux)
IP=$(ifconfig 2>/dev/null | grep -oE 'inet [0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' | grep -v '127.0.0.1' | awk '{print $2}' | head -n 1)
if [ -z "$IP" ]; then
    IP="127.0.0.1"
fi

export PORT=5000

npm start
