#!/usr/bin/env bash
# Screenshots the real 727 client on a PRIVATE Xvfb (never the user's desktop), against any lobby.
# usage: client-shot.sh <out.png> <host> <port> [settle-seconds]
#   INPUT="<xdotool commands>"  run against the private display after the settle (e.g. the first-run
#                               "Auto Setup" click: "mousemove 386 320 mousedown 1 sleep 0.3 mouseup 1")
#   SETTLE2, GAP                 seconds after INPUT, and between the three captures (<out>-1..3.png)
# GLX is disabled on purpose: with it, Auto Setup picks the OpenGL toolkit and every frame is black.
# The client home is a scratch dir (CLIENT_HOME) so the user's own ~/.darkanrs cache and prefs are untouched.
set -uo pipefail
OUT=$1; HOST=$2; PORT=$3; SETTLE=${4:-45}
S=${CLIENT_HOME:-/tmp/darkan-client-shot}
mkdir -p "$S"
JAR=/home/trent/darkan/darkan-bot/build/libs/darkan-client-shaded-2.0.0-all.jar
for n in 90 91 92 93 94; do [ -e /tmp/.X11-unix/X$n ] || { DISP=$n; break; }; done
Xvfb :$DISP -screen 0 1280x960x24 -extension GLX -nolisten tcp >/dev/null 2>&1 &
XPID=$!
sleep 1.5
mkdir -p "$S/clienthome"
DISPLAY=:$DISP java -Duser.home="$S/clienthome" -Dprod=false ${JAVA_EXTRA:-} -Xmx2g -jar "$JAR" "$HOST" "$PORT" > "$S/client.log" 2>&1 &
JPID=$!
sleep "$SETTLE"
if [ -n "${INPUT:-}" ]; then DISPLAY=:$DISP xdotool $INPUT; sleep "${SETTLE2:-8}"; fi
for i in 1 2 3; do DISPLAY=:$DISP import -window root "${OUT%.png}-$i.png"; sleep "${GAP:-10}"; done
DISPLAY=:$DISP xdotool search --name "Darkan" getwindowgeometry %@ > "$S/client-geom.txt" 2>&1
DISPLAY=:$DISP xdotool search --name "" getwindowname %@ 2>/dev/null | head -5 > "$S/client-windows.txt"
kill $JPID 2>/dev/null; sleep 2; kill -9 $JPID 2>/dev/null
kill $XPID 2>/dev/null
echo "captured $OUT"
