#!/usr/bin/env bash
export DARKAN_KIND=world AUTO_UPDATE_CHANNEL="${AUTO_UPDATE_CHANNEL:-dev}"
exec "$(dirname "$0")/deploy/run.sh"
