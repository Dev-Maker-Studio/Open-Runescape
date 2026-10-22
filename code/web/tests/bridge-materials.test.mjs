import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { setImmediate } from "node:timers/promises";
import vm from "node:vm";

const source = (await readFile(new URL("../static/bridge.js", import.meta.url), "utf8"))
  .replace(/^import [^\n]*\n/gm, "")
  .replace(/^export /gm, "");
const relay = "ws://127.0.0.1:8110/js5";
const terrainArgs = [relay, "3200", "3200", "72", "12850,12851,13106,13107"];
const secret = "FAKE_PRIVATE_MATERIAL_FAILURE";
const unavailable = "material metadata probe unavailable";
const independentUnavailable = "independent material metadata unavailable";
const plain = (value) => JSON.parse(JSON.stringify(value));

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function materialRecord(id) {
  return Object.freeze({
    id, detailsOnly: false, isSmall: true, skipTriangles: false,
    brightness: -128, alpha: 127, effectId: -1, effectParam1: 0, colorHsl: -32768,
    textureSpeedU: -5, textureSpeedV: 6, aBool2087: false, isBrickTile: true,
    mipmapping: -1, repeatS: true, repeatT: false, hdr: true, combineMode: 255,
    effectParam2: -2147483648, effectCombiner: 0,
  });
}

function materialOwner() {
  const calls = {
    closes: 0, frees: 0, closed: false, states: 0, ids: [], receivers: [],
    records: new Map([0, 42, 65535].map((id) => [id, materialRecord(id)])),
  };
  const owner = {
    state() {
      assert.equal(this, owner);
      calls.states++;
      calls.onState?.();
      return {
        accepted: !calls.closed, closed: calls.closed, phase: calls.closed ? "closed" : "ready",
        scope: "material-metadata-only", authenticated: false,
        texturePixelsDecoded: false, renderedWorld: false, error: null,
      };
    },
    material(id) {
      assert.equal(this, owner);
      if (calls.closed) throw new Error(secret);
      calls.ids.push(id);
      calls.receivers.push(this);
      calls.onMaterial?.();
      return calls.records.get(id) ?? null;
    },
    close() {
      assert.equal(this, owner);
      calls.closes++;
      if (calls.closed) return;
      calls.closed = true;
      calls.frees++;
      calls.onClose?.();
    },
  };
  return { owner, calls };
}

function diagnosticOwner(scope) {
  const calls = { closes: 0, frees: 0, closed: false };
  const owner = {
    state() { return { accepted: !calls.closed, closed: calls.closed, scope, renderedWorld: false }; },
    close() {
      calls.closes++;
      if (calls.closed) return;
      calls.closed = true;
      calls.frees++;
    },
  };
  return { owner, calls };
}

function fixture() {
  const state = { materialLoads: [], floorLoads: [], terrainLoads: [], accounts: [], controllers: [], unexpected: [], logs: [] };
  const noop = () => {};
  const forbidden = (name) => (...values) => {
    state.unexpected.push({ name, values });
    assert.fail(`unexpected ${name}`);
  };
  const loader = (kind) => (options) => {
    const loading = deferred();
    state[`${kind}Loads`].push({ options, ...loading });
    return state[`${kind}Loader`] ? state[`${kind}Loader`](options, loading) : loading.promise;
  };
  const context = vm.createContext({
    URL,
    URLSearchParams,
    AbortController: class extends AbortController {
      constructor() { super(); state.controllers.push(this); }
    },
    window: {},
    location: { search: "", href: "http://localhost/" },
    console: Object.fromEntries(["log", "info", "warn", "error", "debug"].map((name) => [name, (...values) => state.logs.push(values)])),
    addEventListener: noop,
    connect: forbidden("connect"),
    disconnect: forbidden("disconnect"),
    streamStats: () => ({}),
    openWorldMaterials: loader("material"),
    openWorldFloors: loader("floor"),
    openWorldTerrain: loader("terrain"),
    openOriginalSession: async (options) => {
      const session = materialOwner();
      const snapshot = session.owner.state;
      session.owner.state = function () {
        return { ...snapshot.call(this), world: { connected: !session.calls.closed } };
      };
      state.accounts.push({ options, ...session });
      return session.owner;
    },
    takePrivateLobbyConfig: forbidden("takePrivateLobbyConfig"),
    openLobbySession: forbidden("openLobbySession"),
    observeBrowserLoginOptions: forbidden("observeBrowserLoginOptions"),
    browserLoginProvenance: forbidden("browserLoginProvenance"),
    assetProbe: forbidden("assetProbe"),
    coreScriptProbe: forbidden("coreScriptProbe"),
    gameHandshakeProbe: forbidden("gameHandshakeProbe"),
    protocolProbe: forbidden("protocolProbe"),
    scriptGraphProbe: forbidden("scriptGraphProbe"),
    fetch: forbidden("fetch"),
    WebSocket: class { constructor(...values) { forbidden("WebSocket")(...values); } },
  });
  new vm.Script(source, { filename: "bridge.js" }).runInContext(context);
  return { state, run: context.window.openrs.run, snapshot: context.window.openrs.state };
}

async function loading(f, kind = "material") {
  await setImmediate();
  assert.ok(f.state[`${kind}Loads`].length);
  return f.state[`${kind}Loads`].at(-1);
}

async function retained(f, resource = materialOwner(), route = relay) {
  const pending = f.run("materials-probe", [route]);
  const load = await loading(f);
  load.resolve(resource.owner);
  const result = await pending;
  assert.equal(result.ok, true);
  assert.equal(result.data.accepted, true);
  return resource;
}

async function retainedDiagnostic(f, kind) {
  const resource = diagnosticOwner(kind === "floor" ? "floor-definitions-only" : "surface-only");
  const pending = f.run(kind === "floor" ? "floors-probe" : "terrain-probe", kind === "floor" ? [relay] : terrainArgs);
  (await loading(f, kind)).resolve(resource.owner);
  assert.equal((await pending).ok, true);
  return resource;
}

async function commandOwner(f, command) {
  if (command === "material") return { ...await retained(f), closeCommand: "materials-close", error: unavailable };
  assert.equal((await f.run("standalone-login")).ok, true);
  return { ...f.state.accounts[0], closeCommand: "standalone-session-close", error: independentUnavailable };
}

function failure(result, error = unavailable) {
  assert.deepEqual(plain(result), { ok: false, error, frames: 0 });
}

function cancelled(result) {
  failure(result, "material metadata probe cancelled");
}

function safe(f, ...results) {
  assert.deepEqual(f.state.unexpected, []);
  assert.deepEqual(f.state.logs, []);
  const snapshot = f.snapshot();
  assert.equal(snapshot.lastError, null);
  assert.equal(JSON.stringify([snapshot, results, f.state.logs]).includes(secret), false);
  assert.equal(JSON.stringify(snapshot).includes("signal"), false);
}

test("materials probe retains account-free metadata with only a validated route and a private abort signal", async () => {
  const f = fixture();
  assert.deepEqual(plain(await f.run("materials-state")), { ok: true, data: null, frames: 0 });
  assert.equal(f.snapshot().materialDefinitions, null);
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  assert.deepEqual(Object.keys(load.options).sort(), ["js5Relay", "signal"]);
  assert.equal(load.options.js5Relay, relay);
  assert.ok(load.options.signal instanceof AbortSignal);
  assert.equal(load.options.signal.aborted, false);
  assert.equal(f.state.controllers.length, 1);
  const resource = materialOwner();
  assert.deepEqual(Object.keys(resource.owner).sort(), ["close", "material", "state"]);
  load.resolve(resource.owner);
  const result = await pending;
  assert.deepEqual(plain(result), {
    ok: true,
    data: { accepted: true, closed: false, phase: "ready", scope: "material-metadata-only", authenticated: false, texturePixelsDecoded: false, renderedWorld: false, error: null },
    frames: 0,
  });
  assert.deepEqual(plain((await f.run("materials-state")).data), plain(result.data));
  assert.deepEqual(plain(f.snapshot().materialDefinitions), plain(result.data));
  assert.equal(f.state.accounts.length, 0);
  assert.equal(f.state.floorLoads.length, 0);
  assert.equal(f.state.terrainLoads.length, 0);
  assert.equal(resource.calls.closes, 0);
  safe(f, result);
  await f.run("materials-close");
});

test("material reads return the exact frozen record or null without adding a wrapper or rendering claims", async () => {
  for (const command of ["material", "standalone-material"]) {
    const f = fixture();
    const resource = await commandOwner(f, command);
    for (const id of [0, 42, 65535]) {
      const result = await f.run(command, [String(id)]);
      assert.equal(result.ok, true);
      assert.equal(result.data, resource.calls.records.get(id));
      assert.ok(Object.isFrozen(result.data));
      assert.equal(Object.getPrototypeOf(result.data), Object.prototype);
      assert.deepEqual(plain(result.data), plain(materialRecord(id)));
      assert.equal(Object.hasOwn(result.data, "material"), false);
      assert.equal(Object.hasOwn(result.data, "renderedWorld"), false);
    }
    const missing = await f.run(command, ["1"]);
    assert.deepEqual(plain(missing), { ok: true, data: null, frames: 0 });
    assert.deepEqual(resource.calls.ids, [0, 42, 65535, 1]);
    assert.ok(resource.calls.receivers.every((receiver) => receiver === resource.owner));
    assert.equal(resource.calls.closed, false);
    assert.equal(f.state.accounts.length, command === "material" ? 0 : 1);
    assert.equal(f.state.materialLoads.length, command === "material" ? 1 : 0);
    assert.equal(f.state.floorLoads.length, 0);
    assert.equal(f.state.terrainLoads.length, 0);
    safe(f, missing);
    await f.run(resource.closeCommand);
  }
});

test("busy material probes allocate nothing and do not cancel or replace the active attempt", async () => {
  const f = fixture();
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  for (const route of [relay, "ws://example.com/js5", undefined]) {
    failure(await f.run("materials-probe", [route]), "material metadata probe already running");
  }
  assert.equal(f.state.materialLoads.length, 1);
  assert.equal(f.state.controllers.length, 1);
  assert.equal(load.options.signal.aborted, false);
  const resource = materialOwner();
  load.resolve(resource.owner);
  assert.equal((await pending).ok, true);
  assert.equal(resource.calls.closes, 0);
  safe(f);
  await f.run("materials-close");
});

test("same-turn materials-close cancels before the injected loader starts and releases busy", async () => {
  const f = fixture();
  const pending = f.run("materials-probe", [relay]);
  assert.deepEqual(plain(await f.run("materials-close")), { ok: true, data: null, frames: 0 });
  cancelled(await pending);
  assert.equal(f.state.controllers[0].signal.aborted, true);
  assert.equal(f.state.materialLoads.length, 0);
  assert.equal(f.snapshot().materialDefinitions, null);
  const next = await retained(f);
  await f.run("materials-close");
  assert.equal(next.calls.frees, 1);
  safe(f);
});

test("closing an ignored-signal material load settles promptly and discards its late owner exactly once", async () => {
  const f = fixture();
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  assert.equal((await f.run("materials-close")).data, null);
  assert.equal(load.options.signal.aborted, true);
  cancelled(await pending);
  assert.equal(f.snapshot().materialDefinitions, null);
  const late = materialOwner();
  load.resolve(late.owner);
  await setImmediate();
  assert.equal(late.calls.closes, 1);
  assert.equal(late.calls.frees, 1);
  assert.equal(late.calls.states, 0);
  assert.equal(f.snapshot().materialDefinitions, null);
  await f.run("materials-close");
  assert.equal(late.calls.closes, 1);
  const next = await retained(f);
  await f.run("materials-close");
  assert.equal(next.calls.frees, 1);
  safe(f);
});

test("late cancelled material results and rejections cannot replace or close a newer retained owner", async () => {
  for (const rejection of [false, true]) {
    const f = fixture();
    const first = f.run("materials-probe", [relay]);
    const oldLoad = await loading(f);
    await f.run("materials-close");
    cancelled(await first);
    const current = await retained(f);
    const late = materialOwner();
    if (rejection) oldLoad.reject(new Error(secret));
    else oldLoad.resolve(late.owner);
    await setImmediate();
    assert.equal(late.calls.closes, rejection ? 0 : 1);
    assert.equal(late.calls.frees, rejection ? 0 : 1);
    assert.equal(current.calls.closes, 0);
    assert.equal(f.snapshot().materialDefinitions.accepted, true);
    assert.equal((await f.run("material", ["42"])).data, current.calls.records.get(42));
    await f.run("materials-close");
    assert.equal(current.calls.frees, 1);
    safe(f);
  }
});

test("a late old material owner cannot clear a newer attempt's busy guard or abort controller", async () => {
  const f = fixture();
  const first = f.run("materials-probe", [relay]);
  const oldLoad = await loading(f);
  await f.run("materials-close");
  cancelled(await first);
  const next = f.run("materials-probe", [relay]);
  const nextLoad = await loading(f);
  assert.notEqual(nextLoad.options.signal, oldLoad.options.signal);
  const late = materialOwner();
  oldLoad.resolve(late.owner);
  await setImmediate();
  assert.equal(late.calls.frees, 1);
  failure(await f.run("materials-probe", [relay]), "material metadata probe already running");
  assert.equal(f.state.materialLoads.length, 2);
  assert.equal(nextLoad.options.signal.aborted, false);
  await f.run("materials-close");
  cancelled(await next);
  assert.equal(nextLoad.options.signal.aborted, true);
  const nextLate = materialOwner();
  nextLoad.resolve(nextLate.owner);
  await setImmediate();
  assert.equal(nextLate.calls.frees, 1);
  assert.equal(f.snapshot().materialDefinitions, null);
  safe(f);
});

test("closing after material resolution but before publication wins and frees the resolved owner once", async () => {
  const f = fixture();
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  const resource = materialOwner();
  load.resolve(resource.owner);
  await f.run("materials-close");
  cancelled(await pending);
  await setImmediate();
  assert.equal(resource.calls.closes, 1);
  assert.equal(resource.calls.frees, 1);
  assert.equal(f.snapshot().materialDefinitions, null);
  safe(f);
});

test("cancellation during the final material snapshot prevents publication and frees once", async () => {
  const f = fixture();
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  const resource = materialOwner();
  const closes = [];
  resource.calls.onState = () => { closes.push(f.run("materials-close")); };
  load.resolve(resource.owner);
  cancelled(await pending);
  assert.equal(closes.length, 1);
  assert.deepEqual(plain(await closes[0]), { ok: true, data: null, frames: 0 });
  assert.equal(resource.calls.closes, 1);
  assert.equal(resource.calls.frees, 1);
  assert.equal(f.snapshot().materialDefinitions, null);
  const next = await retained(f);
  await f.run("materials-close");
  assert.equal(next.calls.frees, 1);
  safe(f);
});

test("cooperative and ignored-signal rejected material loads produce only static cancellation errors", async () => {
  for (const cooperative of [false, true]) {
    const f = fixture();
    if (cooperative) f.state.materialLoader = (options, load) => {
      options.signal.addEventListener("abort", () => load.reject(new Error(secret)), { once: true });
      return load.promise;
    };
    const pending = f.run("materials-probe", [relay]);
    const load = await loading(f);
    await f.run("materials-close");
    const result = await pending;
    cancelled(result);
    if (!cooperative) load.reject(new Error(secret));
    await setImmediate();
    assert.equal(f.snapshot().materialDefinitions, null);
    const next = await retained(f);
    await f.run("materials-close");
    assert.equal(next.calls.frees, 1);
    safe(f, result);
  }
});

test("material loader throws, rejections and final snapshot failures discard owners and release busy", async () => {
  for (const point of ["throw", "reject", "snapshot", "state-getter"]) {
    const f = fixture();
    if (point === "throw") f.state.materialLoader = () => { throw new Error(secret); };
    const pending = f.run("materials-probe", [relay]);
    const load = await loading(f);
    const resource = materialOwner();
    if (point === "reject") load.reject(new Error(secret));
    else if (point === "snapshot" || point === "state-getter") {
      if (point === "snapshot") resource.calls.onState = () => { throw new Error(secret); };
      else Object.defineProperty(resource.owner, "state", { get() { throw new Error(secret); } });
      load.resolve(resource.owner);
    }
    const result = await pending;
    failure(result);
    assert.equal(resource.calls.closes, ["snapshot", "state-getter"].includes(point) ? 1 : 0);
    assert.equal(resource.calls.frees, ["snapshot", "state-getter"].includes(point) ? 1 : 0);
    assert.equal(f.snapshot().materialDefinitions, null);
    f.state.materialLoader = undefined;
    const next = await retained(f);
    await f.run("materials-close");
    assert.equal(next.calls.frees, 1);
    safe(f, result);
  }
});

test("materials-close is idempotent for ownership, preserves safe closed history and revokes reads", async () => {
  const f = fixture();
  assert.deepEqual(plain(await f.run("materials-close")), { ok: true, data: null, frames: 0 });
  const resource = await retained(f);
  assert.equal((await f.run("material", ["0"])).data, resource.calls.records.get(0));
  const first = await f.run("materials-close");
  const second = await f.run("materials-close");
  assert.equal(first.ok, true);
  assert.equal(first.data.closed, true);
  assert.equal(first.data.accepted, false);
  assert.equal(first.data.scope, "material-metadata-only");
  assert.equal(first.data.texturePixelsDecoded, false);
  assert.equal(first.data.renderedWorld, false);
  assert.deepEqual(plain(second), plain(first));
  assert.equal(resource.calls.frees, 1);
  assert.deepEqual(plain((await f.run("materials-state")).data), plain(first.data));
  assert.deepEqual(plain(f.snapshot().materialDefinitions), plain(first.data));
  failure(await f.run("material", ["0"]));
  assert.deepEqual(resource.calls.ids, [0]);
  assert.equal(f.state.accounts.length, 0);
  safe(f, first, second);
});

test("replacement closes retained materials before calling the next loader without opening other diagnostics", async () => {
  const f = fixture();
  const first = await retained(f);
  f.state.materialLoader = (_options, load) => {
    assert.equal(first.calls.frees, 1);
    assert.equal(f.snapshot().materialDefinitions, null);
    return load.promise;
  };
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  assert.equal(first.calls.closes, 1);
  const second = materialOwner();
  load.resolve(second.owner);
  assert.equal((await pending).ok, true);
  assert.equal(second.calls.closes, 0);
  assert.equal((await f.run("material", ["0"])).data, second.calls.records.get(0));
  assert.equal(f.state.accounts.length, 0);
  assert.equal(f.state.floorLoads.length, 0);
  assert.equal(f.state.terrainLoads.length, 0);
  await f.run("materials-close");
  assert.equal(first.calls.frees, 1);
  assert.equal(second.calls.frees, 1);
  safe(f);
});

test("late and retained material cleanup exceptions stay static without double-freeing or republishing", async () => {
  for (const retainFirst of [false, true]) {
    const f = fixture();
    const pending = f.run("materials-probe", [relay]);
    const load = await loading(f);
    const resource = materialOwner();
    resource.calls.onClose = () => { throw new Error(secret); };
    if (retainFirst) {
      load.resolve(resource.owner);
      assert.equal((await pending).ok, true);
      failure(await f.run("materials-close"));
      assert.equal(f.snapshot().materialDefinitions.closed, true);
      failure(await f.run("material", ["0"]));
    } else {
      await f.run("materials-close");
      cancelled(await pending);
      load.resolve(resource.owner);
      await setImmediate();
      assert.equal(resource.calls.closes, 1);
      assert.equal(f.snapshot().materialDefinitions, null);
    }
    await f.run("materials-close");
    assert.equal(resource.calls.frees, 1);
    safe(f);
  }
});

test("throwing close getters are contained on explicit close and replacement and do not start a loader", async () => {
  const f = fixture();
  const resource = await retained(f);
  const close = resource.owner.close;
  let reads = 0;
  Object.defineProperty(resource.owner, "close", { configurable: true, get() { reads++; throw new Error(secret); } });
  const closed = await f.run("materials-close");
  failure(closed);
  const replacement = await f.run("materials-probe", [relay]);
  failure(replacement);
  assert.equal(reads, 2);
  assert.equal(f.state.materialLoads.length, 1);
  assert.equal(resource.calls.closes, 0);
  assert.equal(f.snapshot().materialDefinitions.accepted, true);
  Object.defineProperty(resource.owner, "close", { configurable: true, writable: true, value: close });
  const next = await retained(f);
  assert.equal(resource.calls.frees, 1);
  await f.run("materials-close");
  assert.equal(next.calls.frees, 1);
  safe(f, closed, replacement);
});

test("close getters throwing during cancelled or failed unpublished cleanup cannot leak or retain owners", async () => {
  for (const cancel of [false, true]) {
    const f = fixture();
    const pending = f.run("materials-probe", [relay]);
    const load = await loading(f);
    const resource = materialOwner();
    let reads = 0;
    Object.defineProperty(resource.owner, "close", { get() { reads++; throw new Error(secret); } });
    if (cancel) {
      await f.run("materials-close");
      cancelled(await pending);
    } else resource.calls.onState = () => { throw new Error(secret); };
    load.resolve(resource.owner);
    if (!cancel) failure(await pending);
    await setImmediate();
    assert.equal(reads, 1);
    assert.equal(f.snapshot().materialDefinitions, null);
    const next = await retained(f);
    await f.run("materials-close");
    assert.equal(next.calls.frees, 1);
    safe(f);
  }
});

test("retained state methods and getters throwing during state or close return static material errors", async () => {
  for (const getter of [false, true]) {
    const f = fixture();
    const resource = await retained(f);
    const snapshot = resource.owner.state;
    if (getter) Object.defineProperty(resource.owner, "state", { configurable: true, get() { throw new Error(secret); } });
    else resource.calls.onState = () => { throw new Error(secret); };
    const state = await f.run("materials-state");
    const close = await f.run("materials-close");
    failure(state);
    failure(close);
    assert.equal(resource.calls.frees, 1);
    if (getter) Object.defineProperty(resource.owner, "state", { configurable: true, writable: true, value: snapshot });
    else resource.calls.onState = undefined;
    assert.equal((await f.run("materials-state")).data.closed, true);
    assert.equal((await f.run("materials-close")).ok, true);
    assert.equal(resource.calls.frees, 1);
    safe(f, state, close);
  }
});

test("global state commands and direct snapshots sanitize retained material state getter and method failures", async () => {
  for (const getter of [false, true]) {
    const f = fixture();
    const resource = await retained(f);
    const before = await f.run("state");
    assert.equal(before.ok, true);
    assert.equal(before.data.materialDefinitions.accepted, true);
    const snapshot = resource.owner.state;
    if (getter) Object.defineProperty(resource.owner, "state", { configurable: true, get() { throw new Error(secret); } });
    else resource.calls.onState = () => { throw new Error(secret); };
    const result = await f.run("state");
    failure(result);
    assert.throws(() => f.snapshot(), (error) => {
      assert.equal(error.message, unavailable);
      assert.equal(String(error.stack).includes(secret), false);
      return true;
    });
    assert.equal(resource.calls.closes, 0);
    assert.deepEqual(resource.calls.ids, []);
    assert.equal(f.state.materialLoads.length, 1);
    assert.equal(f.state.accounts.length, 0);
    if (getter) Object.defineProperty(resource.owner, "state", { configurable: true, writable: true, value: snapshot });
    else resource.calls.onState = undefined;
    assert.equal(f.snapshot().materialDefinitions.accepted, true);
    assert.equal((await f.run("state")).data.materialDefinitions.accepted, true);
    await f.run("materials-close");
    assert.equal(resource.calls.frees, 1);
    safe(f, result);
  }
});

test("exact loopback JS5 route validation precedes controller allocation, loader calls and retained close", async () => {
  const f = fixture();
  const resource = await retained(f);
  let coercions = 0;
  const nonliteral = { toString() { coercions++; throw new Error(secret); }, valueOf() { coercions++; throw new Error(secret); } };
  const invalid = [
    undefined, null, 0, true, 1n, Symbol(), {}, [], nonliteral, new String(relay),
    "", "/js5", "http://localhost/js5", "https://localhost/js5", "WS://localhost/js5",
    "ws://example.com/js5", "ws://127.1/js5", "ws://127.0.0.2/js5", "ws://0x7f000001/js5", "ws://2130706433/js5",
    "ws://localhost./js5", "ws://LOCALHOST/js5", "ws://localhost.evil/js5", "ws://[::ffff:127.0.0.1]/js5",
    "ws://localhost/world", "ws://localhost/js5/", "ws://localhost/js5?", "ws://localhost/js5#",
    `ws://localhost/js5?token=${secret}`, `ws://localhost/js5#${secret}`, `ws://user:${secret}@localhost/js5`,
    "ws://localhost:0/js5", "ws://localhost:00000/js5", "ws://localhost:65536/js5", "ws://localhost:99999/js5", "ws://localhost:123456/js5",
    "ws://localhost:-1/js5", "ws://localhost:+1/js5", "ws://localhost:/js5", "ws://localhost/x/../js5", "ws://localhost/%6as5",
    " ws://localhost/js5", "ws://localhost/js5 ", "ws://localhost/js5\n", "ws://localhost/js5\r", "ws://local\thost/js5", "ws://localhost/" + "x".repeat(513),
  ];
  for (const route of invalid) failure(await f.run("materials-probe", [route]), "exact loopback JS5 relay required");
  assert.equal(coercions, 0);
  assert.equal(f.state.controllers.length, 1);
  assert.equal(f.state.materialLoads.length, 1);
  assert.equal(resource.calls.closes, 0);
  assert.equal(f.snapshot().materialDefinitions.accepted, true);
  assert.equal(f.state.accounts.length, 0);
  safe(f);
  await f.run("materials-close");
  const empty = fixture();
  failure(await empty.run("materials-probe", [undefined]), "exact loopback JS5 relay required");
  assert.equal(empty.state.controllers.length, 0);
  assert.equal(empty.state.materialLoads.length, 0);
  assert.equal(empty.snapshot().materialDefinitions, null);
  safe(empty);
});

test("literal ws and wss loopback JS5 routes are normalized and accepted without account access", async () => {
  for (const route of ["ws://localhost/js5", "wss://localhost:8110/js5", "ws://127.0.0.1:1/js5", "wss://127.0.0.1:65535/js5", "ws://[::1]/js5", "wss://[::1]:8110/js5", "ws://localhost:00080/js5"]) {
    const f = fixture();
    const resource = await retained(f, materialOwner(), route);
    assert.equal(f.state.materialLoads[0].options.js5Relay, new URL(route).href);
    assert.equal(f.state.accounts.length, 0);
    await f.run("materials-close");
    assert.equal(resource.calls.frees, 1);
    safe(f);
  }
});

test("both material commands require canonical bounded decimal string IDs without coercion or delegation", async () => {
  for (const command of ["material", "standalone-material"]) {
    const f = fixture();
    const resource = await commandOwner(f, command);
    let coercions = 0;
    let getterReads = 0;
    const method = resource.owner.material;
    Object.defineProperty(resource.owner, "material", { configurable: true, get() { getterReads++; return method; } });
    const invalid = [
      undefined, null, 0, 1, -1, 65535, 65536, NaN, Infinity, false, true, 1n, Symbol(), [], {}, new String("0"),
      { toString() { coercions++; throw new Error(secret); }, valueOf() { coercions++; throw new Error(secret); } },
      "", "-1", "-0", "+0", "+1", "00", "01", "00000", "0.0", "1.5", "1e0", "0x0", "0b0", "0o0",
      " 0", "0 ", "0\n", "0\r", "\t0", "NaN", "Infinity", "65536", "99999", "100000", "4294967296", "9007199254740992", "1".repeat(100), "１", "١",
    ];
    for (const id of invalid) failure(await f.run(command, [id]), "bounded material ID required");
    failure(await f.run(command), "bounded material ID required");
    assert.equal(coercions, 0);
    assert.equal(getterReads, 0);
    assert.deepEqual(resource.calls.ids, []);
    assert.equal(resource.calls.closes, 0);
    for (const id of ["0", "9", "10", "65535"]) assert.equal((await f.run(command, [id])).ok, true);
    assert.equal(getterReads, 4);
    assert.deepEqual(resource.calls.ids, [0, 9, 10, 65535]);
    assert.equal(f.state.accounts.length, command === "material" ? 0 : 1);
    assert.equal(f.state.materialLoads.length, command === "material" ? 1 : 0);
    safe(f);
    await f.run(resource.closeCommand);
  }
});

test("absent owners, missing records and closed owners remain distinct for both material commands", async () => {
  for (const command of ["material", "standalone-material"]) {
    const f = fixture();
    const error = command === "material" ? unavailable : independentUnavailable;
    failure(await f.run(command, ["0"]), error);
    failure(await f.run(command, ["01"]), "bounded material ID required");
    assert.equal(f.state.accounts.length, 0);
    assert.equal(f.state.materialLoads.length, 0);
    const resource = await commandOwner(f, command);
    assert.deepEqual(plain(await f.run(command, ["1"])), { ok: true, data: null, frames: 0 });
    assert.equal(resource.calls.closed, false);
    await f.run(resource.closeCommand);
    failure(await f.run(command, ["1"]), error);
    failure(await f.run(command, ["0"]), error);
    assert.deepEqual(resource.calls.ids, [1]);
    assert.equal(resource.calls.frees, 1);
    safe(f);
  }
});

test("missing, noncallable and throwing material methods and getters always use the command's static error", async () => {
  for (const command of ["material", "standalone-material"]) {
    const f = fixture();
    const resource = await commandOwner(f, command);
    const method = resource.owner.material;
    for (const value of [undefined, null, 0, "material", {}, []]) {
      resource.owner.material = value;
      failure(await f.run(command, ["0"]), resource.error);
    }
    Object.defineProperty(resource.owner, "material", { configurable: true, get() { throw new Error(secret); } });
    const getter = await f.run(command, ["0"]);
    failure(getter, resource.error);
    Object.defineProperty(resource.owner, "material", { configurable: true, writable: true, value: method });
    assert.deepEqual(resource.calls.ids, []);
    resource.calls.onMaterial = () => { throw new Error(secret); };
    const thrown = await f.run(command, ["42"]);
    failure(thrown, resource.error);
    assert.equal(resource.calls.closed, false);
    resource.calls.onMaterial = undefined;
    assert.equal((await f.run(command, ["42"])).data, resource.calls.records.get(42));
    assert.equal(resource.calls.closes, 0);
    safe(f, getter, thrown);
    await f.run(resource.closeCommand);
  }
});

test("standalone-material delegates to the existing fake session without extra accounts, configuration or probes", async () => {
  const f = fixture();
  failure(await f.run("standalone-material", ["0"]), independentUnavailable);
  assert.equal(f.state.accounts.length, 0);
  assert.equal((await f.run("standalone-login")).ok, true);
  const session = f.state.accounts[0];
  const controllers = f.state.controllers.length;
  for (const id of ["0", "42", "65535"]) {
    const result = await f.run("standalone-material", [id]);
    assert.equal(result.ok, true);
    assert.equal(result.data, session.calls.records.get(Number(id)));
  }
  assert.deepEqual(session.calls.ids, [0, 42, 65535]);
  assert.equal(f.state.accounts.length, 1);
  assert.equal(f.state.controllers.length, controllers);
  assert.equal(f.state.materialLoads.length, 0);
  assert.equal(f.state.floorLoads.length, 0);
  assert.equal(f.state.terrainLoads.length, 0);
  assert.equal(session.options.signal.aborted, false);
  assert.equal(session.calls.closes, 0);
  await f.run("standalone-session-close");
  failure(await f.run("standalone-material", ["42"]), independentUnavailable);
  assert.equal(session.calls.frees, 1);
  assert.equal(f.state.accounts.length, 1);
  safe(f);
});

test("material close and replacement leave retained floors, terrain and the fake session untouched", async () => {
  const f = fixture();
  const floors = await retainedDiagnostic(f, "floor");
  const terrain = await retainedDiagnostic(f, "terrain");
  await f.run("standalone-login");
  const session = f.state.accounts[0];
  const first = await retained(f);
  const next = await retained(f);
  assert.equal(first.calls.frees, 1);
  await f.run("materials-close");
  assert.equal(next.calls.frees, 1);
  for (const resource of [floors, terrain, session]) assert.equal(resource.calls.closes, 0);
  for (const load of [f.state.floorLoads[0], f.state.terrainLoads[0], session]) assert.equal(load.options.signal.aborted, false);
  assert.equal(f.snapshot().floorDefinitions.accepted, true);
  assert.equal(f.snapshot().surfaceTerrain.accepted, true);
  assert.equal((await f.run("standalone-material", ["0"])).data, session.calls.records.get(0));
  assert.equal(f.state.accounts.length, 1);
  assert.equal(f.state.floorLoads.length, 1);
  assert.equal(f.state.terrainLoads.length, 1);
  await f.run("floors-close");
  await f.run("terrain-close");
  await f.run("standalone-session-close");
  safe(f);
});

test("floor, terrain and session closures cannot cancel a pending independent material probe", async () => {
  const f = fixture();
  await retainedDiagnostic(f, "floor");
  await retainedDiagnostic(f, "terrain");
  await f.run("standalone-login");
  const pending = f.run("materials-probe", [relay]);
  const load = await loading(f);
  for (const signal of [f.state.floorLoads[0].options.signal, f.state.terrainLoads[0].options.signal, f.state.accounts[0].options.signal]) assert.notEqual(load.options.signal, signal);
  for (const command of ["floors-close", "terrain-close", "standalone-session-close"]) {
    assert.equal((await f.run(command)).ok, true);
    assert.equal(load.options.signal.aborted, false);
  }
  const resource = materialOwner();
  load.resolve(resource.owner);
  assert.equal((await pending).ok, true);
  assert.equal((await f.run("material", ["42"])).data, resource.calls.records.get(42));
  assert.equal(resource.calls.closes, 0);
  assert.equal(f.state.accounts.length, 1);
  await f.run("materials-close");
  assert.equal(resource.calls.frees, 1);
  safe(f);
});

test("floor, terrain and session closures leave retained material metadata available", async () => {
  const f = fixture();
  await retainedDiagnostic(f, "floor");
  await retainedDiagnostic(f, "terrain");
  await f.run("standalone-login");
  const resource = await retained(f);
  for (const command of ["floors-close", "terrain-close", "standalone-session-close"]) {
    assert.equal((await f.run(command)).ok, true);
    assert.equal(resource.calls.closes, 0);
    assert.equal(f.snapshot().materialDefinitions.accepted, true);
    assert.equal((await f.run("material", ["65535"])).data, resource.calls.records.get(65535));
  }
  assert.equal(f.state.accounts.length, 1);
  assert.equal(f.state.materialLoads.length, 1);
  await f.run("materials-close");
  assert.equal(resource.calls.frees, 1);
  safe(f);
});

test("material cancellation is independent of pending floor and terrain probes and the fake session", async () => {
  const f = fixture();
  await f.run("standalone-login");
  const floors = f.run("floors-probe", [relay]);
  const terrain = f.run("terrain-probe", terrainArgs);
  const material = f.run("materials-probe", [relay]);
  const floorLoad = await loading(f, "floor");
  const terrainLoad = await loading(f, "terrain");
  const materialLoad = await loading(f);
  await f.run("materials-close");
  cancelled(await material);
  assert.equal(materialLoad.options.signal.aborted, true);
  assert.equal(floorLoad.options.signal.aborted, false);
  assert.equal(terrainLoad.options.signal.aborted, false);
  assert.equal(f.state.accounts[0].options.signal.aborted, false);
  assert.equal(f.state.accounts[0].calls.closes, 0);
  const floor = diagnosticOwner("floor-definitions-only");
  const surface = diagnosticOwner("surface-only");
  floorLoad.resolve(floor.owner);
  terrainLoad.resolve(surface.owner);
  assert.equal((await floors).ok, true);
  assert.equal((await terrain).ok, true);
  const late = materialOwner();
  materialLoad.resolve(late.owner);
  await setImmediate();
  assert.equal(late.calls.frees, 1);
  assert.equal(floor.calls.closes, 0);
  assert.equal(surface.calls.closes, 0);
  assert.equal(f.snapshot().materialDefinitions, null);
  assert.equal(f.state.accounts.length, 1);
  await f.run("floors-close");
  await f.run("terrain-close");
  await f.run("standalone-session-close");
  safe(f);
});

test("help lists all retained and standalone material commands without starting any loader or account", async () => {
  const f = fixture();
  const result = await f.run("help");
  assert.equal(result.ok, true);
  for (const command of ["materials-probe", "materials-state", "material", "materials-close", "standalone-material"]) assert.ok(result.data.includes(command));
  assert.equal(f.state.controllers.length, 0);
  assert.equal(f.state.accounts.length, 0);
  assert.equal(f.state.materialLoads.length, 0);
  assert.equal(f.state.floorLoads.length, 0);
  assert.equal(f.state.terrainLoads.length, 0);
  assert.equal(f.snapshot().materialDefinitions, null);
  safe(f, result);
});
