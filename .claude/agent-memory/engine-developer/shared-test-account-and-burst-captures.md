---
name: shared-test-account-and-burst-captures
description: The one test account (testes) is shared with every other agent session; concurrent HW captures kick each other and move the character. Burst-capture script pattern for per-frame flicker bugs.
metadata:
  type: project
---

Only one account exists for headless/HW captures (`~/.darkan_login` = testes). Other agent
sessions in the same checkout use it too (2026-09-08: a roof-bug agent and I logged each other
out mid-capture, and its login moved the character to its own teleport spot, so my "re-login
at the cliff" run came up at 2894,3427). `pkill` from another session also killed one of my
clients (log stopped right after "Entering render mode").

**Why:** the server allows one client per account and remembers the last position, and
server-side state like `;;pnpc` is lost on re-login (had to re-send `pnpc 50`).

**How to apply:**
- Before every launch: `ps -eo args | grep '[d]arkan-client-shaded.*-Dtoolkit'`; skip if busy.
- Put the teleport in the SAME run (`-Dautologin.command=tele+X+Y+Z;other+cmd`), never rely on
  the previous run's position.
- `-Dautologin.ifbutton=182:6:1` presses the logout button in-game (goes through the lobby,
  AutoLogin re-logs automatically, one-shot hooks do not re-fire) — a real "log out, log back
  in" repro inside one JVM.
- For flicker bugs take N captures 50-100 ms apart (`magick import -window` in a loop) and
  diff a crop with `magick compare -metric AE`; a single capture cannot show a per-frame
  alternation. 16 frames caught 2 "wrong" frames for darkan-bot#22.
- Never diff captures taken from two different jars unless the jar is the variable.
