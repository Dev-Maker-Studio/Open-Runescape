#!/usr/bin/env bash
# The local world (own cache from cache-source) on the DEV lobby, and the client for the user to look.
# usage: dev-world.sh start|stop|restart|status|client
#   .env in the server root: CACHE_SOURCE_PATH=./cache-source, a WORLD_ID nobody else uses,
#   SOCIAL_GATEWAY_URL=ws://dev.darkan.org:4040/social/ws, SOCIAL_GATEWAY_TOKEN and WORLD_LOGIN_TOKEN_SECRET
#   copied from root@10.69.69.22:/home/trent/dev-lobby/.env, MONGO_URI the dev database.
#   The lobby flags the world as own-cache when its index CRCs differ, and the client fetches JS5 from it.
# Restart after every pack: the world reads its cache CRCs at startup.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
JAR=${JAR:-$ROOT/world/build/libs/world-server-3.0.0.jar}
LOG=${LOG:-$ROOT/data/dev-world.log}
LOBBY_HOST=${LOBBY_HOST:-dev.darkan.org}; LOBBY_PORT=${LOBBY_PORT:-43594}; LOBBY_API=${LOBBY_API:-4040}
WORLD_ID=$(sed -n 's/^WORLD_ID=//p' "$ROOT/.env" | tail -1)
pid() { pgrep -f "^java -jar .*$(basename "$JAR")" || true; }
listed() {
  curl -s -m 5 "http://$LOBBY_HOST:$LOBBY_API/worldlist" | python3 -c "
import sys, json
for w in json.load(sys.stdin):
    m = w['metadata']
    if m['number'] == $WORLD_ID:
        print('lobby lists world %d %s at %s:%d customCache=%s offline=%s' % (m['number'], m['activity'], m['ipAddress'], m['port'], w['customCache'], w['offline']))
        break
else:
    print('world $WORLD_ID is not in the lobby world list')"
}
start() {
  [ -n "$(pid)" ] && { echo "world already running, pid $(pid)"; return; }
  [ -f "$JAR" ] || { echo "missing $JAR (remote-build.sh :world:shadowJar, then scp it back)"; exit 1; }
  (cd "$ROOT" && setsid nohup java -jar "$JAR" > "$LOG" 2>&1 < /dev/null &)
  for _ in $(seq 1 180); do grep -q "SocialClient connected\|Exception in\|Error" "$LOG" 2>/dev/null && break; sleep 1; done
  sleep 2
  grep -E "Cache build|World server started|SocialClient connected|Exception|Error" "$LOG" | tail -5
  listed
}
stop() { p=$(pid); [ -n "$p" ] && kill $p && echo "stopped world pid $p" || echo "world not running"; }
case ${1:-} in
  start) start ;;
  stop) stop ;;
  restart) stop; sleep 2; start ;;
  status) p=$(pid); [ -n "$p" ] && echo "world running, pid $p, log $LOG" || echo "world not running"; listed ;;
  client)
    CLIENT=$(ls -v /home/trent/darkan/darkan-bot/build/libs/darkan-client-shaded-*-all.jar | tail -1)
    AUTO=${AUTOLOGIN:+-Dautologin=$AUTOLOGIN}
    [ -f "$HOME/.darkan_login" ] && [ -n "$AUTO" ] && echo "note: ~/.darkan_login exists; a saved login plus -Dautologin logs into the world as the wrong account"
    (setsid nohup java -Dprod=false $AUTO -Xmx2g -jar "$CLIENT" "$LOBBY_HOST" "$LOBBY_PORT" > "$ROOT/data/dev-client.log" 2>&1 < /dev/null &)
    echo "client started on $DISPLAY against $LOBBY_HOST:$LOBBY_PORT; pick world $WORLD_ID" ;;
  *) echo "usage: dev-world.sh start|stop|restart|status|client"; exit 2 ;;
esac
