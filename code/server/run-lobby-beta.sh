#!/usr/bin/env bash
export DARKAN_KIND=lobby AUTO_UPDATE_CHANNEL="${AUTO_UPDATE_CHANNEL:-dev}"
exec "$(dirname "$0")/deploy/run.sh"
