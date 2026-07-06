#!/data/data/com.termux/files/usr/bin/bash

echo "======================================"
echo "  Installing NullX Backend in Termux"
echo "======================================"

echo "[*] Updating Termux packages..."
pkg update -y && pkg upgrade -y

echo "[*] Installing Node.js..."
pkg install nodejs -y

echo "[*] Installing curl and wget..."
pkg install curl wget -y

echo "[*] Installing dependencies..."
npm install

echo "[*] Installation Complete!"
echo "To start the backend, run: ./start.sh"
