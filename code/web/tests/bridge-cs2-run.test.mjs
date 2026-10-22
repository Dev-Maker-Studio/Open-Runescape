import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { setImmediate } from "node:timers/promises";
import vm from "node:vm";
import { cs2RunProbe } from "../static/js5-probe.js";

const source = (await readFile(new URL("../static/bridge.js", import.meta.url), "utf8")).replace(/^import [^\n]*\n/gm, "").replace(/^export /gm, "");
const relay = "ws://127.0.0.1:8111/js5";

function bridge({ session = { varp: () => 0 }, result = { status: "ok", ints: [1] }, fail } = {}) {
  const calls = [];
  const noop = () => {};
  const context = vm.createContext({
    URL, URLSearchParams, AbortController, performance, setTimeout, clearTimeout, setInterval, clearInterval,
    devicePixelRatio: 1, window: {}, location: { search: "", href: "http://localhost/" },
    addEventListener: noop, connect: noop, streamStats: () => ({}), disconnect: noop,
    console: { log: noop, info: noop, warn: noop, error: noop, debug: noop },
    requestAnimationFrame: noop,
    cs2RunProbe: async (...args) => {
      calls.push(args);
      if (fail) throw new Error(fail);
      return result;
    },
  });
  new vm.Script(source, { filename: "bridge.js" }).runInContext(context);
  new vm.Script("globalThis.__st = st;", { filename: "probe.js" }).runInContext(context);
  context.__st.original = session;
  return { calls, st: context.__st, run: context.window.openrs.run };
}

test("cs2-run passes a validated relay, script, arguments and the live session to the browser VM", async () => {
  const session = { varp: () => 0 };
  const b = bridge({ session });
  const result = await b.run("cs2-run", [relay, "9", "[1,-2]", "[\"a\",null]"]);
  assert.equal(result.ok, true, result.error);
  assert.deepEqual(result.data, { status: "ok", ints: [1] });
  assert.equal(b.calls.length, 1);
  const [url, root, passed, options] = b.calls[0];
  assert.equal(url, relay);
  assert.equal(root, 9);
  assert.equal(passed, session);
  assert.deepEqual(JSON.parse(JSON.stringify(options)), { ints: [1, -2], strings: ["a", null] });
  assert.equal(b.st.cs2RunBusy, false);
});

test("cs2-run defaults to no arguments", async () => {
  const b = bridge();
  assert.equal((await b.run("cs2-run", [relay, "0"])).ok, true);
  assert.deepEqual(JSON.parse(JSON.stringify(b.calls[0][3])), { ints: [], strings: [] });
});

test("cs2-run refuses bad input without loading anything and never throws", async () => {
  const b = bridge();
  assert.equal((await b.run("cs2-run", ["ws://example.com/js5", "1"])).error, "exact loopback JS5 relay required");
  for (const id of ["-1", "1.5", "100000", "x", 7]) {
    assert.equal((await b.run("cs2-run", [relay, id])).error, "bounded script ID required");
  }
  for (const args of [["{", "[]"], ["[]", "nope"]]) {
    assert.equal((await b.run("cs2-run", [relay, "1", ...args])).error, "script arguments must be JSON arrays");
  }
  assert.equal((await b.run("cs2-run", [relay, "1", "x".repeat(16385)])).error, "script arguments must be JSON arrays");
  assert.equal(b.calls.length, 0);
});

test("cs2-run without a live session runs stateless", async () => {
  const b = bridge();
  b.st.original = null;
  assert.equal((await b.run("cs2-run", [relay, "1"])).ok, true);
  assert.equal(b.calls[0][2], null);
  b.st.original = {};
  assert.equal((await b.run("cs2-run", [relay, "1"])).ok, true);
  assert.equal(b.calls[1][2], null);
});

test("cs2-run reports probe failures and clears the busy flag", async () => {
  const b = bridge({ fail: "script archive absent from reference table" });
  const result = await b.run("cs2-run", [relay, "1"]);
  assert.equal(result.ok, false);
  assert.equal(result.error, "script archive absent from reference table");
  assert.equal(b.st.cs2RunBusy, false);
  b.st.cs2RunBusy = true;
  assert.equal((await b.run("cs2-run", [relay, "1"])).error, "cs2 run already running");
});

function probeFixture() {
  const state = { phase: 0, freed: 0, closed: 0 };
  class Probe {
    constructor(...args) { state.args = args; }
    outgoing() { return new Uint8Array([15]); }
    feed() { state.phase++; }
    complete() { return state.phase === 2; }
    run(session, ints, strings) { assert.equal(state.freed, 0); state.run = [session, ints, strings]; return JSON.stringify({ status: "ok", ints: [42] }); }
    free() { state.freed++; }
    finish() { if (!this.complete()) throw new Error("incomplete script graph"); }
  }
  class Socket {
    constructor() { state.socket = this; }
    send() {}
    close() { state.closed++; }
  }
  return { state, options: { load: async () => Probe, WebSocketClass: Socket, timeoutMs: 1000 } };
}

test("cs2RunProbe runs the loaded graph against the session before freeing the wasm probe", async () => {
  const f = probeFixture();
  const session = { varp: () => 0 };
  const pending = cs2RunProbe(relay, 3, session, { ...f.options, ints: [5], strings: ["x"] });
  await setImmediate();
  f.state.socket.onopen();
  f.state.socket.onmessage({ data: new ArrayBuffer(1) });
  f.state.socket.onmessage({ data: new ArrayBuffer(1) });
  assert.deepEqual(await pending, { status: "ok", ints: [42] });
  assert.equal(f.state.args[1], 3);
  assert.equal(f.state.run[0], session);
  assert.equal(f.state.run[1], "[5]");
  assert.equal(f.state.run[2], "[\"x\"]");
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closed, 1);
});

test("cs2RunProbe without a session runs the stateless entry point", async () => {
  const f = probeFixture();
  f.state.run = null;
  class Stateless {
    constructor() {}
    outgoing() { return new Uint8Array([15]); }
    feed() { f.state.phase++; }
    complete() { return f.state.phase === 2; }
    runStateless(ints, strings) { f.state.run = [ints, strings]; return JSON.stringify({ status: "ok", scope: "stateless" }); }
    free() {}
    finish() {}
  }
  const pending = cs2RunProbe(relay, 3, null, { ...f.options, load: async () => Stateless, ints: [7], strings: [null] });
  await setImmediate();
  f.state.socket.onopen();
  f.state.socket.onmessage({ data: new ArrayBuffer(1) });
  f.state.socket.onmessage({ data: new ArrayBuffer(1) });
  assert.deepEqual(await pending, { status: "ok", scope: "stateless" });
  assert.deepEqual(f.state.run, ["[7]", "[null]"]);
});

test("cs2RunProbe rejects wrapping arguments and a malformed session before opening a socket", async () => {
  const f = probeFixture();
  const session = { varp: () => 0 };
  await assert.rejects(cs2RunProbe(relay, -1, session, f.options), /script identity/);
  await assert.rejects(cs2RunProbe(relay, 1, session, { ...f.options, ints: [2 ** 31] }), /integer arguments/);
  await assert.rejects(cs2RunProbe(relay, 1, session, { ...f.options, strings: [1] }), /string arguments/);
  await assert.rejects(cs2RunProbe(relay, 1, {}, f.options), /world session/);
  assert.equal(f.state.socket, undefined);
});
