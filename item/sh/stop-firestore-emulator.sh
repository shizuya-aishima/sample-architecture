#!/bin/bash
# Stop mock firestore

PID=$(lsof -t -i :9000 -s tcp:LISTEN)
if [ ! -z "$PID" ]; then
  echo "Stopping mock Firestore server"
  kill "$PID"
fi
