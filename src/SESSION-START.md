# Session Start

Paste the prompt below into a new Claude Code session opened in `/data/projects/Personal/OpenRS-Web`.

```text
You are continuing work on OpenRS Web in /data/projects/Personal/OpenRS-Web.

Read these first, in this order, and follow them:
1. _APP-SPECIFICATION.md and _MARKDOWN-FORMAT.md in the project root. Never edit files that start with "_" unless I ask. No comments in code, least code, memory and CPU efficient.
2. CLAUDE.md in the project root.
3. src/HARNESS.md, then the src/ documents it lists when a task touches them.

Step 1, check the environment before anything else:
- Run: touch ~/sandbox-probe && echo OK && rm ~/sandbox-probe
- If it fails with "Read-only file system", this session's Bash runs in a sandbox. Tell me straight away and stop. In a sandbox the client cannot run, so live testing is impossible.
- Otherwise build and run the harness doctor:
  cd code/harness && cargo build --release && ./target/release/openrs doctor
- Report every FAIL and WARN line with its fix.

Step 2, run the live verification:
  cd code/harness && ./target/release/openrs verify
- It starts the local lobby and world on the shared packed cache (assets/cache/.packed), logs a local account into the world, and runs the complete native and browser verification groups, stopping at the first failure. The report is in code/harness/target/verify/.
- verify takes about 5 minutes, longer than one Bash command may run, so start it detached and poll the log:
  setsid nohup sh -c './target/release/openrs verify > target/verify.log 2>&1; echo verify_exit=$? >> target/verify.log' >/dev/null 2>&1 < /dev/null &
- The latest integrated result is 55 live steps passed, plus separate streamed-game and temporal tests. Read src/HARNESS.md and use openrs verify list for the current step list.
- Fix each failure at its root cause, rebuild, and rerun verify until it passes. Tell me what failed and what you changed.
- Compare measurements with the Verified Results in src/HARNESS.md and update them when they change.

Step 3, remaining work after the harness checks:
1. Read src/FRAME-STREAM.md and src/GAMES.md. The selected architecture is a game host outside the browser, streaming ORF3 resources and frames over WebSocket and receiving sequenced input. The test games are implemented; the Kotlin game-host adapter is still needed.
2. Use openrs twin --frames N --every K and openrs equiv --frames N --target browser for temporal rendering work. Preserve same-frame screenshots and records; do not dismiss missing data or alpha-state differences as conventions.
3. Improve rendering parity (offscreen targets, water, particles, reflections and bloom) using measured artifacts. A harness verification pass does not mean rendering is pixel-equivalent.

Rules:
- Use the local lobby for testing. Never create accounts on the shared lobby lobby.devmakerstudio.com, and never change the server's login or per-IP limits.
- Never inject input into my real desktop. Kill processes only by recorded PID.
- Keep the docs in src/ current with every change, following _MARKDOWN-FORMAT.md.
- Ask me before any git commit or push.
```

# Expected Results

| Step | Expected result |
| --- | --- |
| Sandbox probe | Prints `OK` |
| `openrs doctor` | No FAIL lines, a stale jar or closed server ports are warnings |
| `openrs verify` | Every current native and browser step passes, or the first failing step names what to fix |
