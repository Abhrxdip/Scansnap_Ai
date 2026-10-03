@echo off
title ScanSnap AI Backend Server
cd /d "%~dp0backend"
echo ========================================================
echo Starting ScanSnap AI Backend on http://0.0.0.0:8000
echo Network IP: 192.168.0.101
echo ========================================================
if exist ".\venv\Scripts\python.exe" (
    .\venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
) else (
    python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
)
pause

