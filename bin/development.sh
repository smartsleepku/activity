#!/usr/bin/env bash

# Kill the spawned process if this script is interrupted
sigint_handler()
{
  kill $PID
  exit
}

trap sigint_handler SIGINT

# Continuously observe source directory for updates and rebuild and relaunch service on file change
while true; do
    (cd $(dirname $0)/.. && ./gradlew run) &
    PID=$!
    inotifywait -e modify -e move -e create -e delete -e attrib -r $(dirname $0)/../src
    kill $PID
    # Kill anyone still using port 7000
    kill -9 `netstat -nepal | awk '$4 ~ /:7000/ { sub(/\/java/,"",$NF) ; print $NF }'`
done
