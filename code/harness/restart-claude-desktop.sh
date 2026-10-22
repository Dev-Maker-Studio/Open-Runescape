#!/usr/bin/env bash
set -u

if [[ "${RESTART_DETACHED:-}" != 1 ]]; then
  log="${XDG_RUNTIME_DIR:-/tmp}/restart-claude-desktop.log"
  RESTART_DETACHED=1 setsid -f "$0" "$@" >"$log" 2>&1 </dev/null
  echo "restarting Claude desktop in the background, log: $log"
  exit 0
fi

app=/usr/lib/claude-desktop/claude-desktop

tree() {
  local p
  for p in "$@"; do
    echo "$p"
    tree $(pgrep -P "$p")
  done
}

roots=$(pgrep -u "$USER" -x claude-desktop; pgrep -u "$USER" -f "^$app")
roots=$(echo "$roots" | sort -u | grep -v "^$$\$")
if [[ -z "$roots" ]]; then
  echo "Claude desktop is not running"
else
  pids=$(tree $roots | sort -u | grep -vx "$$")
  echo "stopping: $(echo $pids)"
  kill -TERM $pids 2>/dev/null
  for _ in $(seq 1 20); do
    alive=$(for p in $pids; do kill -0 "$p" 2>/dev/null && echo "$p"; done)
    [[ -z "$alive" ]] && break
    sleep 0.5
  done
  [[ -n "${alive:-}" ]] && { echo "force killing: $(echo $alive)"; kill -KILL $alive 2>/dev/null; }
fi

sleep 1
echo "starting Claude desktop"
nohup claude-desktop "${1:-claude://code/new?source=desktop_action}" >/dev/null 2>&1 &
disown
echo "started pid $!"
