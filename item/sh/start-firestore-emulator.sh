#!/bin/bash
# Launch firestore emulator on port 9000

waitport() {
  while ! nc -z localhost $1; do
    sleep 1
  done
}

PID=$(lsof -t -i :9000 -s tcp:LISTEN)
if [ -z "$PID" ]; then
  echo "Starting mock Firestore server on port 9000"
  nohup gcloud beta emulators firestore start \
    --host-port=127.0.0.1:9000 \
    > /tmp/mock-firestore-logs &
  waitport 9000
else
  echo "There is an instance of Firestore already running on port 9000"
fi
