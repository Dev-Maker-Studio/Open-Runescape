import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";

const source = (await readFile(new URL("../static/bridge.js", import.meta.url), "utf8")).replace(/^import [^\n]*\n/gm, "").replace(/^export /gm, "");
const secret = "FAKE_PRIVATE_STATS_FAILURE";
const plain = (value) => JSON.parse(JSON.stringify(value));

function fixture({ withStats = true } = {}) {
  const calls = { skills: [], stats: 0, logs: [], closes: 0, opened: [] };
  const noop = () => {};
  const owner = {
    state: () => ({ world: { connected: calls.closes === 0 } }),
    close: () => { calls.closes++; },
    ...(withStats ? {
      skill: (id) => {
        calls.skills.push(id);
        if (calls.closes) throw new Error(secret);
        return Object.freeze({ baseLevel: 99, experience: -2147483648, visibleLevel: 255 });
      },
      playerStats: function () {
        calls.stats++;
        if (this !== owner || calls.closes) throw new Error(secret);
        return Object.freeze({ skillCount: 25, runEnergy: 255 });
      },
    } : {}),
  };
  const context = vm.createContext({
    URL, URLSearchParams, AbortController, window: {}, location: { search: "", href: "http://localhost/" },
    addEventListener: noop, connect: noop, disconnect: noop, streamStats: () => ({}),
    console: Object.fromEntries(["log", "info", "warn", "error", "debug"].map((name) => [name, (...args) => calls.logs.push(args)])),
    openOriginalSession: async (options) => { calls.opened.push(options); return owner; },
  });
  new vm.Script(source, { filename: "bridge.js" }).runInContext(context);
  return { calls, run: context.window.openrs.run };
}

test("player stats harness is unavailable without an account session", async () => {
  const f = fixture();
  assert.deepEqual(plain(await f.run("world-stats")), { ok: false, error: "independent player stats unavailable", frames: 0 });
  assert.deepEqual(plain(await f.run("world-skill", ["0"])), { ok: false, error: "independent player skill unavailable", frames: 0 });
});

test("player stats harness delegates canonical skill ids and returns frozen snapshots without raw handles", async () => {
  const f = fixture();
  assert.equal((await f.run("standalone-login")).ok, true);
  const stats = await f.run("world-stats");
  assert.deepEqual(plain(stats.data), { skillCount: 25, runEnergy: 255 });
  assert.ok(Object.isFrozen(stats.data));
  for (const id of ["0", "24"]) {
    const result = await f.run("world-skill", [id]);
    assert.deepEqual(plain(result.data), { baseLevel: 99, experience: -2147483648, visibleLevel: 255 });
    assert.ok(Object.isFrozen(result.data));
  }
  assert.deepEqual(f.calls.skills, [0, 24]);
  assert.equal(f.calls.stats, 1);
  assert.deepEqual(f.calls.logs, []);
});

test("standalone login forwards the account js5 relay and keeps the defaults", async () => {
  const f = fixture();
  assert.equal((await f.run("standalone-login", ["ws://h:1/lobby", "ws://h:1/world", "ws://h:2/js5", "ws://h:1/js5"])).ok, true);
  assert.equal(f.calls.opened[0].js5Relay, "ws://h:1/js5");
  assert.equal(f.calls.opened[0].worldJs5Relay, "ws://h:2/js5");
  const g = fixture();
  await g.run("standalone-login");
  assert.equal(g.calls.opened[0].js5Relay, "ws://127.0.0.1:8110/js5");
});

test("player stats harness rejects invalid arguments before delegation", async () => {
  const f = fixture();
  await f.run("standalone-login");
  for (const args of [[], ["-1"], ["25"], ["01"], ["1.5"], ["0x1"], [" 1"], ["1", "2"], [1], [null], ["100000"]]) {
    assert.equal((await f.run("world-skill", args)).error, "bounded skill ID required");
  }
  assert.equal((await f.run("world-stats", ["x"])).error, "world stats takes no arguments");
  assert.deepEqual(f.calls.skills, []);
  assert.equal(f.calls.stats, 0);
});

test("player stats harness hides private failures and missing delegates", async () => {
  const f = fixture();
  await f.run("standalone-login");
  await f.run("standalone-session-close");
  const stats = await f.run("world-stats");
  const skill = await f.run("world-skill", ["3"]);
  assert.deepEqual([stats.error, skill.error], ["independent player stats unavailable", "independent player skill unavailable"]);
  assert.equal(JSON.stringify([stats, skill]).includes(secret), false);
  const legacy = fixture({ withStats: false });
  await legacy.run("standalone-login");
  assert.equal((await legacy.run("world-stats")).error, "independent player stats unavailable");
  assert.equal((await legacy.run("world-skill", ["0"])).error, "independent player skill unavailable");
});
