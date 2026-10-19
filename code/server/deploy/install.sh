#!/usr/bin/env bash
# Sets up one Darkan server directory and its systemd unit on a host.
#   deploy/install.sh <name> <lobby|world> <release|dev> [git-url] [gitlab-project-id]
# Run as trent. Creates /home/trent/<name> as a shallow clone, writes .env and .env.unit from the
# examples when they do not exist yet, and installs the darkan@ unit. Secrets are never written by
# this script: fill them into the two env files afterwards.
set -euo pipefail
NAME="${1:?name}"; KIND="${2:?lobby|world}"; CHANNEL="${3:?release|dev}"
REPO="${4:-git@gitlab.com:darkanrs/server.git}"; PROJECT_ID="${5:-67255206}"
DIR="/home/trent/$NAME"

[ -d "$DIR/.git" ] || git clone --depth 1 -b dev "$REPO" "$DIR"
cd "$DIR"
[ -f .env ] || cp "deploy/env.$KIND.example" .env
if [ ! -f .env.unit ]; then
  cat > .env.unit <<UNIT
DARKAN_KIND=$KIND
AUTO_UPDATE_CHANNEL=$CHANNEL
AUTO_UPDATE=true
DARKAN_GITLAB_PROJECT_ID=$PROJECT_ID
GITLAB_ACCESS_TOKEN=
JAVA_HOME=$HOME/.sdkman/candidates/java/26.0.2-tem
DARKAN_JAVA_VM_ARGS="-Xms1G -Xmx4G -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
UNIT
  chmod 600 .env.unit
fi
chmod 600 .env
sudo cp deploy/darkan@.service /etc/systemd/system/darkan@.service
sudo systemctl daemon-reload
sudo systemctl enable "darkan@$NAME" >/dev/null
echo "Installed darkan@$NAME in $DIR. Fill in .env and .env.unit, then: sudo systemctl start darkan@$NAME"
