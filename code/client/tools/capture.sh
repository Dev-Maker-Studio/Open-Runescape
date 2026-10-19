#!/usr/bin/env bash
# Launch the client on a given toolkit, auto-login, capture the REAL window, exit.
#
# Captures what is actually composited on screen -- not a GL backbuffer readback --
# so it shows exactly what a human would see, including anything the renderer fails
# to draw.
#
# Usage:  tools/capture.sh <toolkit-id> <output.png> [settle-seconds]
#           toolkit 1 = legacy GL   (the reference; AbstractToolkit maps 1 and 5 -> "GL")
#           toolkit 3 = modern GL   (under test)
#           toolkit 0 = software (SM)
#
# DISPLAY MODE
#   Default: a private Xvfb display. The client never appears on, takes focus from,
#   or moves the pointer of the live desktop -- capturing has ZERO impact on whatever
#   you are doing. Rendering goes through Mesa llvmpipe (software), which is correct
#   but NOT hardware accelerated, so frame timings from this mode are meaningless.
#
#   HW=1 runs on the live display for PERFORMANCE measurement only. Even then it does
#   not activate the window or warp the pointer -- no focus stealing.
#
# Credentials come from ~/.darkan_login (user:pass) or DARKAN_USER/DARKAN_PASS.
# Always kills the client (and the Xvfb it started), including on failure.

set -uo pipefail

TOOLKIT="${1:?usage: capture.sh <toolkit-id> <output.png> [settle-seconds]}"
OUT="${2:?usage: capture.sh <toolkit-id> <output.png> [settle-seconds]}"
SETTLE="${3:-12}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/build/libs/darkan-client-shaded-2.0.0-all.jar"
LOG="$(mktemp -t darkan-capture-XXXXXX.log)"

[ -f "$JAR" ] || { echo "missing $JAR (run ./gradlew shadowJar)" >&2; exit 1; }

HW="${HW:-0}"
CLIENT_PID=""
XVFB_PID=""
VDISP=""

cleanup() {
  if [ -n "$CLIENT_PID" ] && kill -0 "$CLIENT_PID" 2>/dev/null; then
    kill "$CLIENT_PID" 2>/dev/null
    for _ in 1 2 3 4 5; do kill -0 "$CLIENT_PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$CLIENT_PID" 2>/dev/null
  fi
  if [ -n "$XVFB_PID" ] && kill -0 "$XVFB_PID" 2>/dev/null; then
    kill "$XVFB_PID" 2>/dev/null
    for _ in 1 2 3; do kill -0 "$XVFB_PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$XVFB_PID" 2>/dev/null
  fi
}
trap cleanup EXIT INT TERM

if [ "$HW" = "1" ]; then
  RUN_DISPLAY="${DISPLAY:?no DISPLAY set}"
  echo "mode=HARDWARE display=$RUN_DISPLAY (perf-valid; will NOT steal focus)"
else
  # Claim the first unused display number so concurrent captures cannot collide.
  for n in $(seq 90 120); do
    [ -e "/tmp/.X11-unix/X$n" ] && continue
    VDISP=":$n"; break
  done
  [ -n "$VDISP" ] || { echo "FAIL: no free X display number" >&2; exit 1; }
  Xvfb "$VDISP" -screen 0 1280x900x24 -nolisten tcp >>"$LOG" 2>&1 &
  XVFB_PID=$!
  for _ in $(seq 1 40); do
    DISPLAY="$VDISP" xdpyinfo >/dev/null 2>&1 && break
    kill -0 "$XVFB_PID" 2>/dev/null || { echo "FAIL: Xvfb died" >&2; tail -20 "$LOG" >&2; exit 1; }
    sleep 0.25
  done
  RUN_DISPLAY="$VDISP"
  echo "mode=VIRTUAL display=$VDISP (software GL — frame timings are NOT meaningful)"
  # The client blacklists Mesa/llvmpipe; allow it for headless captures only.
  EXTRA="${EXTRA:-} -Drender.allowsoftgl=true"
fi

# EXTRA carries additional -D flags (e.g. EXTRA="-Drender.cullface=false") so a
# hypothesis can be A/B'd across two captures without a rebuild.
# shellcheck disable=SC2086
DISPLAY="$RUN_DISPLAY" java -XX:+UseZGC -Xmx4g -Xms1g -Dtoolkit="$TOOLKIT" -Dautologin=true ${WINDOW:+-Dwindow.size=$WINDOW} ${EXTRA:-} -jar "$JAR" >> "$LOG" 2>&1 &
CLIENT_PID=$!
echo "client pid=$CLIENT_PID toolkit=$TOOLKIT log=$LOG"

# Wait for AutoLogin to report it reached the world (or to tell us why it can't).
for _ in $(seq 1 120); do
  grep -q "\[AutoLogin\] READY" "$LOG" && break
  if grep -q "\[AutoLogin\] NO CREDENTIALS" "$LOG"; then
    echo "FAIL: no credentials. Put 'user:pass' in ~/.darkan_login" >&2
    grep "\[AutoLogin\]" "$LOG" >&2
    exit 2
  fi
  kill -0 "$CLIENT_PID" 2>/dev/null || { echo "FAIL: client exited early" >&2; tail -20 "$LOG" >&2; exit 3; }
  sleep 2
done

if ! grep -q "\[AutoLogin\] READY" "$LOG"; then
  echo "FAIL: never reached world" >&2
  grep "\[AutoLogin\]" "$LOG" | tail -10 >&2
  exit 4
fi

# Optional window size for the benchmark (WINDOW=WxH): resize OUR client's window
# through the window manager before the settle (no input goes to the game).
if [ -n "${WINDOW:-}" ]; then
  # Resize, then resize again a little later: the client occasionally misses the
  # first one while it is still loading and keeps drawing at the old size.
  for pass in 1 2; do
    sleep 4
    for w in $(DISPLAY="$RUN_DISPLAY" xdotool search --all --pid "$CLIENT_PID" --classname "com-darkan-bot-BotKt" 2>/dev/null); do
      geo=$(DISPLAY="$RUN_DISPLAY" xdotool getwindowgeometry "$w" 2>/dev/null | grep -o '[0-9]\+x[0-9]\+')
      case "$geo" in 1x1|"") ;; *) DISPLAY="$RUN_DISPLAY" xdotool windowsize --sync "$w" "${WINDOW%x*}" "${WINDOW#*x}" 2>/dev/null; break;; esac
    done
  done
fi

# Let the region stream in and the scene settle before grabbing.
sleep "$SETTLE"

# The real client window. NOTE: matching on the title "Darkan" also matches the
# unrelated Darkan-launcher; match the class instead, and skip the 1x1 stub.
WID=""
# Prefer the window owned by OUR client: another client of the same class (the
# user's own session) may be open on the live display.
for w in $(DISPLAY="$RUN_DISPLAY" xdotool search --all --pid "$CLIENT_PID" --classname "com-darkan-bot-BotKt" 2>/dev/null) $(DISPLAY="$RUN_DISPLAY" xdotool search --classname "com-darkan-bot-BotKt" 2>/dev/null); do
  geo=$(DISPLAY="$RUN_DISPLAY" xdotool getwindowgeometry "$w" 2>/dev/null | grep -o '[0-9]\+x[0-9]\+')
  case "$geo" in 1x1|"") ;; *) WID="$w"; break;; esac
done
[ -n "$WID" ] || { echo "FAIL: client window not found" >&2; exit 5; }

# Park the pointer off the game area: hovering the UI raises tooltips and hover
# highlights (a Worn Equipment tooltip and the world map both crept into captures).
# On the virtual display this is invisible; on hardware it is skipped entirely so
# the live pointer is never moved.
if [ "$HW" != "1" ]; then
  DISPLAY="$RUN_DISPLAY" xdotool mousemove 5 5 2>/dev/null
  sleep 1
fi

DISPLAY="$RUN_DISPLAY" magick import -window "$WID" "$OUT" \
  || { echo "FAIL: capture failed" >&2; exit 6; }

echo "captured -> $OUT ($(file -b "$OUT"))"
grep -E "\[ModernPerf\]|\[ModernDiag\]" "$LOG" | tail -2

# The legacy toolkit prints no ModernPerf/ModernDiag line, so the grep above would
# otherwise make a successful capture exit non-zero.
exit 0
