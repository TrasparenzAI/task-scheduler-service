#!/bin/bash

# Task Scheduler Service for Linux installation script
#
# See https://github.com/trasparenzai/task-scheduler-service/ for more details.
#
# This script is meant for quick & easy install via:
#   $ curl -fsSL https://raw.githubusercontent.com/trasparenzai/task-scheduler-service/main/first-setup.sh -o first-setup.sh && sh first-setup.sh

# NOTE: Make sure to verify the contents of the script
#       you downloaded matches the contents of first-setup.sh
#       located at https://github.com/trasparenzai/task-scheduler-service/first-setup.sh
#
# This script need docker and docker compose plugin to be installed successfully.

INSTALL_DIR=${INSTALL_DIR:-.}

echo "Task scheduler service installation script."
echo "Before running this script install docker, docker compose and add current user to docker group." 
read -p "Press enter to continue, Ctrl+C to abort" ready
command -v docker -v >/dev/null 2>&1 || { echo >&2 "docker not found.  Aborting."; exit 1; }
command -v docker compose version >/dev/null 2>&1 || { echo >&2 "docker compose plugin not found.  Aborting."; exit 1; }

cd $INSTALL_DIR

curl https://raw.githubusercontent.com/trasparenzai/task-scheduler-service/main/docker-compose.yml -o docker-compose.yml
curl https://raw.githubusercontent.com/trasparenzai/task-scheduler-service/main/.env -o .env

# Avvio del task-scheduler-service 
docker compose up -d
