import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { setImmediate } from "node:timers/promises";
import vm from "node:vm";

const source = (await readFile(new URL("../static/bridge.js", import.meta.url), "utf8"))
  .replace(/^import [^\n]*\n/gm, "")
  .replace(/^export /gm, "");
const args = ["ws://127.0.0.1:8110/js5", "3200", "3200", "72", "12850,12851,13106,13107"];
const secret = "FAKE_PRIVATE_SERVER_FAILURE";
const plain = (value) => JSON.parse(JSON.stringify(value));

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function terrainOwner(palette = false) {
  const state = { closes: 0, frees: 0, closed: false, paletteCalls: [], tileCalls: [], mapSize: 72 };
  return {
    calls: state,
    close() {
      state.closes++;
      if (state.closed) return;
      state.closed = true;
      state.frees++;
      state.onClose?.();
    },
    state() {
      state.onState?.();
      return { accepted: !state.closed, closed: state.closed, scope: "surface-only", effectsDecoded: false, renderedWorld: false, ...(palette ? { palette: { scope: "underlay-packed-hsl", rgbPrepared: false, renderedWorld: false } } : {}) };
    },
    height() { if (state.closed) throw new Error("world terrain owner is closed"); return -432; },
    sceneHeight() { if (state.closed) throw new Error("world terrain owner is closed"); return -876; },
    sceneTile(plane, x, y) {
      if (state.closed) throw new Error("world terrain owner is closed");
      state.tileCalls.push([plane, x, y]);
      if (x >= state.mapSize || y >= state.mapSize) throw new Error("invalid world terrain coordinates");
      state.onTile?.();
      return Object.hasOwn(state, "tileValue") ? state.tileValue : Object.freeze({ underlayId: 255, overlayId: 0, renderFlags: 32, overlayShape: 11, overlayRotation: 3 });
    },
    ...(palette ? { paletteColor(plane, x, y) {
      if (state.closed) throw new Error("world terrain owner is closed");
      state.paletteCalls.push([plane, x, y]);
      if (x >= 72 || y >= 72) throw new Error("invalid world terrain coordinates");
      state.onPalette?.();
      return state.paletteValue === undefined ? 12345 : state.paletteValue;
    } } : {}),
  };
}

function floorOwner() {
  const calls = { closes: 0, frees: 0, closed: false, underlay: [], overlay: [] };
  return {
    calls,
    state() {
      calls.onState?.();
      return { accepted: !calls.closed, closed: calls.closed, scope: "floor-definitions-only", authenticated: false, renderedWorld: false, opcode8SideEffectsApplied: false };
    },
    underlay(id) {
      if (calls.closed) throw new Error("world floor definitions owner is closed");
      calls.underlay.push(id);
      calls.onUnderlay?.();
      return { id, present: false, rgb: 0, texture: -1, scale: 512 };
    },
    overlay(id) {
      if (calls.closed) throw new Error("world floor definitions owner is closed");
      calls.overlay.push(id);
      calls.onOverlay?.();
      return { id, present: false, overlayId: id, tileColorHsl: 0, minimapColorHsl: -1 };
    },
    composeTerrain() { assert.fail("bridge must pass the owner to terrain, not touch its private probe"); },
    close() {
      calls.closes++;
      if (calls.closed) return;
      calls.closed = true;
      calls.frees++;
      calls.onClose?.();
    },
  };
}

function fixture() {
  const state = { loads: [], floorLoads: [], materialLoads: [], accounts: [], logs: [] };
  const noop = () => {};
  const context = vm.createContext({
    URL,
    URLSearchParams,
    AbortController,
    window: {},
    location: { search: "", href: "http://localhost/" },
    console: Object.fromEntries(["log", "info", "warn", "error", "debug"].map((name) => [name, (...values) => state.logs.push(values)])),
    addEventListener: noop,
    connect: noop,
    disconnect: noop,
    streamStats: () => ({}),
    openWorldTerrain: (options) => {
      const loading = deferred();
      state.loads.push({ options, ...loading });
      return state.loader ? state.loader(options, loading) : loading.promise;
    },
    openWorldFloors: (options) => {
      const loading = deferred();
      state.floorLoads.push({ options, ...loading });
      return state.floorLoader ? state.floorLoader(options, loading) : loading.promise;
    },
    openWorldMaterials: (options) => {
      const loading = deferred();
      state.materialLoads.push({ options, ...loading });
      return state.materialLoader ? state.materialLoader(options, loading) : loading.promise;
    },
    openOriginalSession: async (options) => {
      const account = { options, closed: 0, state: () => ({ world: { connected: account.closed === 0 }, renderedWorld: false }), close() { account.closed++; } };
      state.accounts.push(account);
      return account;
    },
  });
  new vm.Script(source, { filename: "bridge.js" }).runInContext(context);
  return { state, run: context.window.openrs.run, snapshot: context.window.openrs.state };
}

async function loading(f) {
  await setImmediate();
  assert.ok(f.state.loads.length);
  return f.state.loads.at(-1);
}

async function retained(f, owner = terrainOwner()) {
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  load.resolve(owner);
  const result = await pending;
  assert.equal(result.ok, true);
  assert.equal(result.data.accepted, true);
  return owner;
}

async function tileCommandOwner(f, command) {
  const owner = terrainOwner();
  if (command === "surface-tile") {
    await retained(f, owner);
    return { owner, target: owner, closeCommand: "terrain-close", unavailable: "surface terrain tile unavailable" };
  }
  await f.run("standalone-login");
  const account = f.state.accounts[0];
  account.sceneTile = (...coordinates) => {
    if (account.closed) throw new Error("world terrain owner is closed");
    return owner.sceneTile(...coordinates);
  };
  return { owner, target: account, closeCommand: "standalone-session-close", unavailable: "independent surface terrain tile unavailable" };
}

function cancelled(result) {
  assert.deepEqual(plain(result), { ok: false, error: "surface terrain probe cancelled", frames: 0 });
}

async function loadingFloors(f) {
  await setImmediate();
  assert.ok(f.state.floorLoads.length);
  return f.state.floorLoads.at(-1);
}

async function retainedFloors(f, owner = floorOwner(), relay = args[0]) {
  const pending = f.run("floors-probe", [relay]);
  const load = await loadingFloors(f);
  load.resolve(owner);
  const result = await pending;
  assert.equal(result.ok, true);
  assert.equal(result.data.accepted, true);
  return owner;
}

async function retainedPalette(f, owner = terrainOwner(true)) {
  const pending = f.run("terrain-probe", [...args, "floors"]);
  const load = await loading(f);
  load.resolve(owner);
  const result = await pending;
  assert.equal(result.ok, true);
  return owner;
}

function floorsCancelled(result) {
  assert.deepEqual(plain(result), { ok: false, error: "floor definitions probe cancelled", frames: 0 });
}

test("bridge terrain probe passes only validated map data and a private owned abort signal", async () => {
  const f = fixture();
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  assert.equal(load.options.js5Relay, args[0]);
  assert.deepEqual(plain(load.options.initialWorld), {
    mapKind: "standard", base: { x: 3200, y: 3200 }, mapSize: 72,
    regionIds: [12850, 12851, 13106, 13107],
  });
  assert.ok(load.options.signal instanceof AbortSignal);
  assert.equal(load.options.signal.aborted, false);
  const owner = terrainOwner();
  load.resolve(owner);
  const result = await pending;
  assert.equal(result.ok, true);
  assert.equal(result.data.scope, "surface-only");
  assert.equal(result.data.effectsDecoded, false);
  assert.equal(result.data.renderedWorld, false);
  assert.equal(f.snapshot().surfaceTerrain.accepted, true);
  assert.equal(JSON.stringify(f.snapshot()).includes("signal"), false);
  assert.equal(f.state.accounts.length, 0);
  await f.run("terrain-close");
});

test("busy rejection does not cancel or replace the original terrain probe", async () => {
  const f = fixture();
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  const busy = await f.run("terrain-probe", args);
  assert.equal(busy.ok, false);
  assert.equal(busy.error, "surface terrain probe already running");
  assert.equal(f.state.loads.length, 1);
  assert.equal(load.options.signal.aborted, false);
  const owner = terrainOwner();
  load.resolve(owner);
  assert.equal((await pending).ok, true);
  assert.equal(owner.calls.closes, 0);
  await f.run("terrain-close");
});

test("same-turn terrain-close cancels before even an injected loader starts", async () => {
  const f = fixture();
  const pending = f.run("terrain-probe", args);
  const closed = await f.run("terrain-close");
  assert.equal(closed.ok, true);
  assert.equal(closed.data, null);
  cancelled(await pending);
  assert.equal(f.state.loads.length, 0);
  assert.equal(f.snapshot().surfaceTerrain, null);
  const next = await retained(f);
  await f.run("terrain-close");
  assert.equal(next.calls.frees, 1);
});

test("closing a pending ignored-signal load releases busy promptly and closes a late owner exactly once", async () => {
  const f = fixture();
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  const close = await f.run("terrain-close");
  assert.equal(close.ok, true);
  assert.equal(close.data, null);
  assert.equal(load.options.signal.aborted, true);
  cancelled(await pending);
  assert.equal(f.snapshot().surfaceTerrain, null);
  const late = terrainOwner();
  load.resolve(late);
  await setImmediate();
  assert.equal(late.calls.closes, 1);
  assert.equal(late.calls.frees, 1);
  assert.equal(f.snapshot().surfaceTerrain, null);
  const current = await retained(f);
  await f.run("terrain-close");
  assert.equal(current.calls.frees, 1);
});

test("late cancelled owners cannot replace a subsequently retained terrain probe", async () => {
  const f = fixture();
  const first = f.run("terrain-probe", args);
  const firstLoad = await loading(f);
  await f.run("terrain-close");
  cancelled(await first);
  const current = await retained(f);
  const late = terrainOwner();
  firstLoad.resolve(late);
  await setImmediate();
  assert.equal(late.calls.closes, 1);
  assert.equal(late.calls.frees, 1);
  assert.equal(current.calls.closes, 0);
  assert.equal(f.snapshot().surfaceTerrain.accepted, true);
  assert.equal((await f.run("surface-height", ["0", "0", "0"])).data.height, -876);
  await f.run("terrain-close");
});

test("closing after resolution but before publication wins and cannot publish the resolved owner", async () => {
  const f = fixture();
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  const owner = terrainOwner();
  load.resolve(owner);
  await f.run("terrain-close");
  cancelled(await pending);
  await setImmediate();
  assert.equal(owner.calls.closes, 1);
  assert.equal(owner.calls.frees, 1);
  assert.equal(f.snapshot().surfaceTerrain, null);
});

test("cancellation during the final owner snapshot revokes publication and frees once", async () => {
  const f = fixture();
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  const owner = terrainOwner();
  owner.calls.onState = () => { void f.run("terrain-close"); };
  load.resolve(owner);
  cancelled(await pending);
  assert.equal(owner.calls.closes, 1);
  assert.equal(owner.calls.frees, 1);
  assert.equal(f.snapshot().surfaceTerrain, null);
});

test("cooperative cancellation and rejected ignored-signal loaders return static errors without late publication", async () => {
  for (const cooperative of [false, true]) {
    const f = fixture();
    if (cooperative) f.state.loader = (options, load) => {
      options.signal.addEventListener("abort", () => load.reject(new Error(secret)), { once: true });
      return load.promise;
    };
    const pending = f.run("terrain-probe", args);
    const load = await loading(f);
    await f.run("terrain-close");
    cancelled(await pending);
    if (!cooperative) load.reject(new Error(secret));
    await setImmediate();
    assert.equal(f.snapshot().surfaceTerrain, null);
    assert.equal(JSON.stringify(f.state.logs).includes(secret), false);
    const owner = await retained(f);
    await f.run("terrain-close");
    assert.equal(owner.calls.frees, 1);
  }
});

test("loader and snapshot failures are static, discard allocated owners and release busy", async () => {
  for (const point of ["load", "snapshot"]) {
    const f = fixture();
    const pending = f.run("terrain-probe", args);
    const load = await loading(f);
    const owner = terrainOwner();
    if (point === "load") load.reject(new Error(secret));
    else {
      owner.calls.onState = () => { throw new Error(secret); };
      load.resolve(owner);
    }
    const result = await pending;
    assert.equal(result.ok, false);
    assert.equal(result.error, "surface terrain probe unavailable");
    assert.equal(f.snapshot().surfaceTerrain, null);
    assert.equal(owner.calls.closes, point === "snapshot" ? 1 : 0);
    const next = await retained(f);
    await f.run("terrain-close");
    assert.equal(next.calls.frees, 1);
  }
});

test("retained terrain-close is idempotent, retains closed safe state and revokes accessors", async () => {
  const f = fixture();
  const owner = await retained(f);
  assert.equal((await f.run("terrain-height", ["12850", "0", "0", "0"])).data.height, -432);
  const first = await f.run("terrain-close");
  const second = await f.run("terrain-close");
  assert.equal(first.ok, true);
  assert.equal(first.data.closed, true);
  assert.equal(first.data.accepted, false);
  assert.deepEqual(plain(second), plain(first));
  assert.equal(owner.calls.frees, 1);
  assert.equal(f.snapshot().surfaceTerrain.renderedWorld, false);
  assert.equal((await f.run("terrain-height", ["12850", "0", "0", "0"])).error, "world terrain owner is closed");
  assert.equal((await f.run("surface-height", ["0", "0", "0"])).error, "world terrain owner is closed");
});

test("replacing retained terrain closes the previous owner before starting another independent probe", async () => {
  const f = fixture();
  const first = await retained(f);
  const pending = f.run("terrain-probe", args);
  const next = await loading(f);
  assert.equal(first.calls.frees, 1);
  assert.equal(f.snapshot().surfaceTerrain, null);
  const second = terrainOwner();
  next.resolve(second);
  assert.equal((await pending).ok, true);
  assert.equal(second.calls.closes, 0);
  await f.run("terrain-close");
  assert.equal(second.calls.frees, 1);
});

test("late and retained cleanup failures remain static and cannot re-publish or double-free owners", async () => {
  for (const retainedFirst of [false, true]) {
    const f = fixture();
    const pending = f.run("terrain-probe", args);
    const load = await loading(f);
    const owner = terrainOwner();
    owner.calls.onClose = () => { throw new Error(secret); };
    if (retainedFirst) {
      load.resolve(owner);
      assert.equal((await pending).ok, true);
      const close = await f.run("terrain-close");
      assert.equal(close.ok, false);
      assert.equal(close.error, "surface terrain probe unavailable");
      assert.equal(f.snapshot().surfaceTerrain.closed, true);
    } else {
      await f.run("terrain-close");
      cancelled(await pending);
      load.resolve(owner);
      await setImmediate();
      assert.equal(f.snapshot().surfaceTerrain, null);
      assert.equal(owner.calls.closes, 1);
    }
    await f.run("terrain-close");
    assert.equal(owner.calls.frees, 1);
    assert.equal(JSON.stringify(f.state.logs).includes(secret), false);
  }
});

test("terrain cancellation stays independent from the account diagnostic and never requests accounts", async () => {
  const f = fixture();
  const accountResult = await f.run("standalone-login");
  assert.equal(accountResult.ok, true);
  assert.equal(f.state.accounts.length, 1);
  const account = f.state.accounts[0];
  const pending = f.run("terrain-probe", args);
  const load = await loading(f);
  assert.notEqual(load.options.signal, account.options.signal);
  await f.run("terrain-close");
  cancelled(await pending);
  assert.equal(account.options.signal.aborted, false);
  assert.equal(account.closed, 0);
  assert.equal(f.state.accounts.length, 1);
  const late = terrainOwner();
  load.resolve(late);
  await setImmediate();
  assert.equal(late.calls.frees, 1);
  const terrain = await retained(f);
  await f.run("standalone-session-close");
  assert.equal(account.closed, 1);
  assert.equal(terrain.calls.closed, false);
  await f.run("terrain-close");
});

test("floor diagnostics retain an account-free owner with private cancellation and safe public state", async () => {
  const f = fixture();
  assert.equal((await f.run("floors-state")).data, null);
  const pending = f.run("floors-probe", [args[0]]);
  const load = await loadingFloors(f);
  assert.deepEqual(Object.keys(load.options).sort(), ["js5Relay", "signal"]);
  assert.equal(load.options.js5Relay, args[0]);
  assert.ok(load.options.signal instanceof AbortSignal);
  assert.equal(load.options.signal.aborted, false);
  const owner = floorOwner();
  load.resolve(owner);
  const result = await pending;
  assert.equal(result.ok, true);
  assert.equal(result.data.scope, "floor-definitions-only");
  assert.equal(result.data.authenticated, false);
  assert.equal(result.data.renderedWorld, false);
  assert.equal(result.data.opcode8SideEffectsApplied, false);
  assert.equal((await f.run("floors-state")).data.accepted, true);
  assert.equal(f.snapshot().floorDefinitions.accepted, true);
  assert.equal(JSON.stringify(f.snapshot()).includes("signal"), false);
  assert.deepEqual(plain((await f.run("floor-underlay", ["0"])).data), { id: 0, present: false, rgb: 0, texture: -1, scale: 512 });
  assert.deepEqual(plain((await f.run("floor-overlay", ["65535"])).data), { id: 65535, present: false, overlayId: 65535, tileColorHsl: 0, minimapColorHsl: -1 });
  assert.deepEqual(owner.calls.underlay, [0]);
  assert.deepEqual(owner.calls.overlay, [65535]);
  assert.equal(f.state.accounts.length, 0);
  assert.equal(f.state.loads.length, 0);
  assert.deepEqual(f.state.logs, []);
  await f.run("floors-close");
});

test("bridge rejects nonliteral or decorated JS5 relays before invoking either injected loader or closing owners", async () => {
  const f = fixture();
  const floors = await retainedFloors(f);
  const terrain = await retained(f);
  for (const relay of [undefined, null, 123, {}, "/js5", "ws://example.com/js5", "http://localhost/js5", "ws://127.1/js5", "ws://0x7f000001/js5", "ws://2130706433/js5", "ws://localhost./js5", "ws://LOCALHOST/js5", "ws://localhost.evil/js5", "ws://localhost/world", "ws://localhost/js5/", "ws://localhost/js5?", "ws://localhost/js5#", "ws://user:password@localhost/js5", "ws://localhost:0/js5", "ws://localhost:65536/js5", "ws://localhost/x/../js5", "ws://localhost/%6as5", " ws://localhost/js5", "ws://localhost/js5\n"]) {
    for (const [command, values] of [["floors-probe", [relay]], ["terrain-probe", [relay, ...args.slice(1)]]]) {
      const result = await f.run(command, values);
      assert.equal(result.ok, false);
      assert.equal(result.error, "exact loopback JS5 relay required");
    }
  }
  assert.equal(f.state.floorLoads.length, 1);
  assert.equal(f.state.loads.length, 1);
  assert.equal(floors.calls.closes, 0);
  assert.equal(terrain.calls.closes, 0);
  await f.run("floors-close");
  await f.run("terrain-close");
  for (const relay of ["ws://localhost/js5", "wss://localhost:8110/js5", "ws://[::1]:8110/js5", "wss://127.0.0.1/js5"]) {
    const next = fixture();
    await retainedFloors(next, floorOwner(), relay);
    assert.equal(next.state.floorLoads[0].options.js5Relay, new URL(relay).href);
    await next.run("floors-close");
  }
});

test("floor busy rejection preserves the original attempt and same-turn close never calls a loader", async () => {
  const f = fixture();
  const pending = f.run("floors-probe", [args[0]]);
  const load = await loadingFloors(f);
  const busy = await f.run("floors-probe", [args[0]]);
  assert.equal(busy.ok, false);
  assert.equal(busy.error, "floor definitions probe already running");
  assert.equal(load.options.signal.aborted, false);
  assert.equal(f.state.floorLoads.length, 1);
  const owner = floorOwner();
  load.resolve(owner);
  assert.equal((await pending).ok, true);
  await f.run("floors-close");
  assert.equal(owner.calls.frees, 1);
  const next = fixture();
  const neverLoaded = next.run("floors-probe", [args[0]]);
  assert.equal((await next.run("floors-close")).data, null);
  floorsCancelled(await neverLoaded);
  assert.equal(next.state.floorLoads.length, 0);
  assert.equal(next.snapshot().floorDefinitions, null);
});

test("closing ignored-signal floor loads is immediate and late owners cannot replace newer retained definitions", async () => {
  const f = fixture();
  const pending = f.run("floors-probe", [args[0]]);
  const load = await loadingFloors(f);
  assert.equal((await f.run("floors-close")).data, null);
  assert.equal(load.options.signal.aborted, true);
  floorsCancelled(await pending);
  const current = await retainedFloors(f);
  const late = floorOwner();
  load.resolve(late);
  await setImmediate();
  assert.equal(late.calls.closes, 1);
  assert.equal(late.calls.frees, 1);
  assert.equal(current.calls.closes, 0);
  assert.equal(f.snapshot().floorDefinitions.accepted, true);
  await f.run("floors-close");
  assert.equal(current.calls.frees, 1);
});

test("floor cancellation during resolution or final snapshot never publishes and frees once", async () => {
  for (const snapshot of [false, true]) {
    const f = fixture();
    const pending = f.run("floors-probe", [args[0]]);
    const load = await loadingFloors(f);
    const owner = floorOwner();
    if (snapshot) owner.calls.onState = () => { void f.run("floors-close"); };
    load.resolve(owner);
    if (!snapshot) await f.run("floors-close");
    floorsCancelled(await pending);
    await setImmediate();
    assert.equal(owner.calls.closes, 1);
    assert.equal(owner.calls.frees, 1);
    assert.equal(f.snapshot().floorDefinitions, null);
  }
});

test("floor loader, snapshot and cooperative cancellation failures remain static and release busy", async () => {
  for (const point of ["load", "snapshot", "cooperative", "late-reject"]) {
    const f = fixture();
    if (point === "cooperative") f.state.floorLoader = (options, load) => {
      options.signal.addEventListener("abort", () => load.reject(new Error(secret)), { once: true });
      return load.promise;
    };
    const pending = f.run("floors-probe", [args[0]]);
    const load = await loadingFloors(f);
    const owner = floorOwner();
    if (point === "load") load.reject(new Error(secret));
    else if (point === "snapshot") {
      owner.calls.onState = () => { throw new Error(secret); };
      load.resolve(owner);
    } else await f.run("floors-close");
    const result = await pending;
    if (["cooperative", "late-reject"].includes(point)) floorsCancelled(result);
    else {
      assert.equal(result.ok, false);
      assert.equal(result.error, "floor definitions probe unavailable");
    }
    if (point === "late-reject") load.reject(new Error(secret));
    await setImmediate();
    assert.equal(owner.calls.frees, point === "snapshot" ? 1 : 0);
    assert.equal(f.snapshot().floorDefinitions, null);
    const next = await retainedFloors(f);
    await f.run("floors-close");
    assert.equal(next.calls.frees, 1);
    assert.equal(JSON.stringify(f.state.logs).includes(secret), false);
  }
});

test("floor IDs are canonical bounded strings and invalid IDs preserve the retained owner", async () => {
  const f = fixture();
  const owner = await retainedFloors(f);
  for (const id of [undefined, null, 0, -1, 65536, "-1", "65536", "01", "+1", "1.0", "1e2", " 1", "1 ", "NaN", "Infinity", "", true, 1n, { toString() { assert.fail("must not coerce"); } }]) {
    for (const command of ["floor-underlay", "floor-overlay"]) {
      const result = await f.run(command, [id]);
      assert.equal(result.ok, false);
      assert.equal(result.error, "bounded floor definition ID required");
    }
  }
  assert.deepEqual(owner.calls.underlay, []);
  assert.deepEqual(owner.calls.overlay, []);
  assert.equal((await f.run("floor-underlay", ["65535"])).ok, true);
  assert.equal((await f.run("floor-overlay", ["0"])).ok, true);
  owner.calls.onUnderlay = () => { throw new Error(secret); };
  owner.calls.onOverlay = () => { throw new Error(secret); };
  for (const command of ["floor-underlay", "floor-overlay"]) assert.equal((await f.run(command, ["0"])).error, "floor definitions probe unavailable");
  assert.equal(f.snapshot().floorDefinitions.accepted, true);
  owner.calls.onState = () => { throw new Error(secret); };
  assert.equal((await f.run("floors-state")).error, "floor definitions probe unavailable");
  owner.calls.onState = null;
  await f.run("floors-close");
});

test("retained floor close preserves closed history and is idempotent for WASM ownership", async () => {
  const f = fixture();
  const owner = await retainedFloors(f);
  const first = await f.run("floors-close");
  const second = await f.run("floors-close");
  assert.equal(first.ok, true);
  assert.equal(first.data.closed, true);
  assert.equal(first.data.accepted, false);
  assert.deepEqual(plain(second), plain(first));
  assert.equal(owner.calls.frees, 1);
  assert.equal(f.snapshot().floorDefinitions.renderedWorld, false);
  assert.equal((await f.run("floor-underlay", ["0"])).error, "floor definitions probe unavailable");
  assert.equal((await f.run("floor-overlay", ["0"])).error, "floor definitions probe unavailable");
});

test("optional terrain palettes require literal floors, a live owner and the same fixed JS5 route", async () => {
  const f = fixture();
  const existing = await retained(f);
  const missing = await f.run("terrain-probe", [...args, "floors"]);
  assert.equal(missing.ok, false);
  assert.equal(missing.error, "retained floor definitions from the same JS5 relay required");
  const floors = await retainedFloors(f);
  for (const flag of [false, true, 0, null, "true", "false", "Floors", ""]) {
    const result = await f.run("terrain-probe", [...args, flag]);
    assert.equal(result.ok, false);
    assert.equal(result.error, "optional terrain palette argument must be floors");
  }
  const mismatched = await f.run("terrain-probe", ["ws://127.0.0.1:8111/js5", ...args.slice(1), "floors"]);
  assert.equal(mismatched.ok, false);
  assert.equal(mismatched.error, "retained floor definitions from the same JS5 relay required");
  assert.equal(existing.calls.closes, 0);
  assert.equal(f.state.loads.length, 1);
  await f.run("floors-close");
  assert.equal(floors.calls.frees, 1);
  assert.equal(existing.calls.closes, 0);
  assert.equal((await f.run("terrain-probe", [...args, "floors"])).ok, false);
  await f.run("terrain-close");
});

test("surface and independent scene tile commands return raw tile snapshots without claiming rendering", async () => {
  for (const command of ["surface-tile", "standalone-scene-tile"]) {
    const f = fixture();
    const { owner, closeCommand } = await tileCommandOwner(f, command);
    owner.calls.mapSize = 168;
    const expected = { underlayId: 255, overlayId: 0, renderFlags: 32, overlayShape: 11, overlayRotation: 3 };
    const result = await f.run(command, ["3", "167", "167"]);
    assert.deepEqual(plain(result), { ok: true, data: { tile: expected, effectsDecoded: false, renderedWorld: false }, frames: 0 });
    assert.equal(Object.getPrototypeOf(result.data.tile), Object.prototype);
    assert.ok(Object.isFrozen(result.data.tile));
    assert.deepEqual(owner.calls.tileCalls, [[3, 167, 167]]);
    owner.calls.tileValue = null;
    assert.deepEqual(plain(await f.run(command, ["0", "0", "0"])), { ok: true, data: { tile: null, effectsDecoded: false, renderedWorld: false }, frames: 0 });
    owner.calls.tileValue = Object.freeze({ underlayId: 0, overlayId: 255, renderFlags: 0, overlayShape: 0, overlayRotation: 0 });
    assert.deepEqual(plain((await f.run(command, ["0", "0", "0"])).data), { tile: owner.calls.tileValue, effectsDecoded: false, renderedWorld: false });
    assert.deepEqual(owner.calls.tileCalls, [[3, 167, 167], [0, 0, 0], [0, 0, 0]]);
    assert.equal(f.state.accounts.length, command === "surface-tile" ? 0 : 1);
    assert.equal(f.state.loads.length, command === "surface-tile" ? 1 : 0);
    assert.equal(f.state.floorLoads.length, 0);
    assert.equal(owner.calls.closes, 0);
    assert.deepEqual(f.state.logs, []);
    await f.run(closeCommand);
  }
});

test("both scene tile commands require canonical bounded decimal coordinates without coercion", async () => {
  for (const command of ["surface-tile", "standalone-scene-tile"]) {
    const f = fixture();
    const { owner, closeCommand } = await tileCommandOwner(f, command);
    const invalid = [
      undefined, null, 0, 1, false, true, NaN, Infinity, 1n, Symbol(),
      { toString() { assert.fail("must not coerce tile coordinates"); } },
      "", "-1", "-0", "+0", "00", "01", "0.0", "1.5", "1e0", "0x0", "0b0", "0o0", " 0", "0 ", "0\n", "0\r", "\t0", "NaN", "Infinity", "4294967296", "9007199254740992", "1".repeat(100),
    ];
    for (const value of invalid) {
      for (let at = 0; at < 3; at++) {
        const coordinates = ["0", "0", "0"];
        coordinates[at] = value;
        assert.deepEqual(plain(await f.run(command, coordinates)), { ok: false, error: "bounded terrain coordinate required", frames: 0 });
      }
    }
    for (const coordinates of [[], ["0"], ["0", "0"], ["4", "0", "0"], ["0", "168", "0"], ["0", "0", "168"]]) {
      assert.deepEqual(plain(await f.run(command, coordinates)), { ok: false, error: "bounded terrain coordinate required", frames: 0 });
    }
    assert.deepEqual(owner.calls.tileCalls, []);
    assert.equal(owner.calls.closed, false);
    assert.equal(owner.calls.closes, 0);
    assert.equal(f.state.accounts.length, command === "surface-tile" ? 0 : 1);
    assert.equal(f.state.loads.length, command === "surface-tile" ? 1 : 0);
    assert.equal(f.state.floorLoads.length, 0);
    assert.deepEqual(f.state.logs, []);
    await f.run(closeCommand);
  }
});

test("scene tile commands keep missing, closed, out-of-map and throwing owner errors static", async () => {
  for (const command of ["surface-tile", "standalone-scene-tile"]) {
    const f = fixture();
    const unavailable = command === "surface-tile" ? "surface terrain tile unavailable" : "independent surface terrain tile unavailable";
    const failure = { ok: false, error: unavailable, frames: 0 };
    assert.deepEqual(plain(await f.run(command, ["0", "0", "0"])), failure);
    const { owner, target, closeCommand } = await tileCommandOwner(f, command);
    const accessor = target.sceneTile;
    for (const value of [undefined, null, 1, "sceneTile", {}]) {
      target.sceneTile = value;
      assert.deepEqual(plain(await f.run(command, ["0", "0", "0"])), failure);
    }
    Object.defineProperty(target, "sceneTile", { configurable: true, get() { throw new Error(secret); } });
    assert.deepEqual(plain(await f.run(command, ["0", "0", "0"])), failure);
    Object.defineProperty(target, "sceneTile", { configurable: true, writable: true, value: accessor });
    assert.deepEqual(owner.calls.tileCalls, []);
    for (const coordinates of [["0", "72", "0"], ["0", "0", "72"]]) {
      assert.deepEqual(plain(await f.run(command, coordinates)), failure);
    }
    owner.calls.onTile = () => { throw new Error(secret); };
    assert.deepEqual(plain(await f.run(command, ["0", "0", "0"])), failure);
    assert.equal(owner.calls.closed, false);
    owner.calls.onTile = undefined;
    assert.equal((await f.run(command, ["0", "0", "0"])).ok, true);
    await f.run(closeCommand);
    const calls = owner.calls.tileCalls.length;
    assert.deepEqual(plain(await f.run(command, ["0", "0", "0"])), failure);
    assert.equal(owner.calls.tileCalls.length, calls);
    assert.equal(f.state.accounts.length, command === "surface-tile" ? 0 : 1);
    assert.equal(f.state.loads.length, command === "surface-tile" ? 1 : 0);
    assert.equal(f.state.floorLoads.length, 0);
    assert.equal(JSON.stringify(f.state.logs).includes(secret), false);
    assert.equal(JSON.stringify(f.snapshot()).includes(secret), false);
  }
});

test("palette terrain passes the retained floor owner without accounts and exposes only packed HSL readings", async () => {
  const f = fixture();
  const floors = await retainedFloors(f);
  const pending = f.run("terrain-probe", [...args, "floors"]);
  const load = await loading(f);
  assert.equal(load.options.floors, floors);
  assert.notEqual(load.options.signal, f.state.floorLoads[0].options.signal);
  assert.equal(load.options.js5Relay, f.state.floorLoads[0].options.js5Relay);
  const terrain = terrainOwner(true);
  load.resolve(terrain);
  const result = await pending;
  assert.equal(result.ok, true);
  assert.equal(result.data.palette.scope, "underlay-packed-hsl");
  assert.equal(result.data.palette.rgbPrepared, false);
  assert.deepEqual(plain((await f.run("surface-palette-color", ["0", "0", "71"])).data), { packedHsl: 12345, rgbPrepared: false, renderedWorld: false });
  assert.deepEqual(terrain.calls.paletteCalls, [[0, 0, 71]]);
  terrain.calls.paletteValue = null;
  assert.equal((await f.run("surface-palette-color", ["3", "71", "0"])).data.packedHsl, null);
  terrain.calls.paletteValue = 0;
  assert.equal((await f.run("surface-palette-color", ["0", "0", "0"])).data.packedHsl, 0);
  assert.equal(f.state.accounts.length, 0);
  assert.equal(f.snapshot().surfaceTerrain.palette.renderedWorld, false);
  await f.run("terrain-close");
  assert.equal(floors.calls.closed, false);
  assert.equal((await f.run("floor-underlay", ["0"])).ok, true);
  await f.run("floors-close");
});

test("palette coordinate validation and runtime failures are static without destroying retained ownership", async () => {
  const f = fixture();
  assert.equal((await f.run("surface-palette-color", ["0", "0", "0"])).error, "surface terrain palette unavailable");
  await retainedFloors(f);
  const terrain = await retainedPalette(f);
  for (const values of [["4", "0", "0"], ["0", "168", "0"], ["0", "0", "168"], ["0", "-1", "0"], ["0", "0.5", "0"], [0, "0", "0"], ["0", "00", "0"]]) {
    const result = await f.run("surface-palette-color", values);
    assert.equal(result.ok, false);
    assert.equal(result.error, "bounded terrain coordinate required");
  }
  assert.deepEqual(terrain.calls.paletteCalls, []);
  assert.equal((await f.run("surface-palette-color", ["0", "72", "0"])).error, "surface terrain palette unavailable");
  terrain.calls.onPalette = () => { throw new Error(secret); };
  assert.equal((await f.run("surface-palette-color", ["0", "0", "0"])).error, "surface terrain palette unavailable");
  assert.equal(terrain.calls.closed, false);
  assert.equal(f.snapshot().floorDefinitions.accepted, true);
  assert.equal(JSON.stringify(f.state.logs).includes(secret), false);
  await f.run("floors-close");
});

test("closing or replacing floors closes only their dependent retained terrain", async () => {
  for (const replace of [false, true]) {
    const f = fixture();
    const floors = await retainedFloors(f);
    const terrain = await retainedPalette(f);
    if (replace) {
      const pending = f.run("floors-probe", [args[0]]);
      const load = await loadingFloors(f);
      assert.equal(floors.calls.frees, 1);
      assert.equal(terrain.calls.frees, 1);
      const next = floorOwner();
      load.resolve(next);
      assert.equal((await pending).ok, true);
      assert.equal(next.calls.frees, 0);
    } else assert.equal((await f.run("floors-close")).ok, true);
    assert.equal(floors.calls.frees, 1);
    assert.equal(terrain.calls.frees, 1);
    assert.equal(f.snapshot().surfaceTerrain.closed, true);
    assert.equal((await f.run("surface-palette-color", ["0", "0", "0"])).error, "surface terrain palette unavailable");
    await f.run("floors-close");
    assert.equal(terrain.calls.frees, 1);
  }
});

test("closing floors cancels dependent ignored-signal terrain and destroys a late owner exactly once", async () => {
  const f = fixture();
  const floors = await retainedFloors(f);
  const pending = f.run("terrain-probe", [...args, "floors"]);
  const load = await loading(f);
  await f.run("floors-close");
  cancelled(await pending);
  assert.equal(load.options.signal.aborted, true);
  assert.equal(floors.calls.frees, 1);
  const nextFloors = await retainedFloors(f);
  const currentTerrain = await retainedPalette(f);
  const late = terrainOwner(true);
  load.resolve(late);
  await setImmediate();
  assert.equal(late.calls.closes, 1);
  assert.equal(late.calls.frees, 1);
  assert.equal(currentTerrain.calls.closed, false);
  assert.equal(nextFloors.calls.closed, false);
  assert.equal(f.snapshot().surfaceTerrain.accepted, true);
  await f.run("floors-close");
});

test("same-turn floor close cancels dependent terrain before its injected loader is called", async () => {
  const f = fixture();
  const floors = await retainedFloors(f);
  const pending = f.run("terrain-probe", [...args, "floors"]);
  await f.run("floors-close");
  cancelled(await pending);
  assert.equal(f.state.loads.length, 0);
  assert.equal(floors.calls.frees, 1);
  assert.equal(f.snapshot().surfaceTerrain, null);
});

test("height-only diagnostics and authenticated sessions are independent of floor cancellation and replacement", async () => {
  const f = fixture();
  await f.run("standalone-login");
  const account = f.state.accounts[0];
  await retainedFloors(f);
  const heightOnly = f.run("terrain-probe", args);
  const load = await loading(f);
  assert.equal(Object.hasOwn(load.options, "floors"), false);
  await f.run("floors-close");
  assert.equal(load.options.signal.aborted, false);
  assert.equal(account.options.signal.aborted, false);
  assert.equal(account.closed, 0);
  const terrain = terrainOwner();
  load.resolve(terrain);
  assert.equal((await heightOnly).ok, true);
  await retainedFloors(f);
  assert.equal(terrain.calls.closed, false);
  await f.run("floors-close");
  assert.equal(terrain.calls.closed, false);
  assert.equal((await f.run("surface-height", ["0", "0", "0"])).data.height, -876);
  assert.equal(f.state.accounts.length, 1);
  await f.run("standalone-session-close");
  assert.equal(terrain.calls.closed, false);
  await f.run("terrain-close");
});

test("dependent terrain cleanup exceptions still close floors and never reflect private values", async () => {
  for (const replacement of [false, true]) {
    const f = fixture();
    const floors = await retainedFloors(f);
    const terrain = await retainedPalette(f);
    floors.calls.onClose = () => { throw new Error(secret); };
    terrain.calls.onClose = () => { throw new Error(secret); };
    const result = await f.run(replacement ? "floors-probe" : "floors-close", replacement ? [args[0]] : []);
    assert.equal(result.ok, false);
    assert.equal(result.error, "floor definitions probe unavailable");
    assert.equal(floors.calls.frees, 1);
    assert.equal(terrain.calls.frees, 1);
    assert.equal(f.state.floorLoads.length, 1);
    assert.equal(f.snapshot().floorDefinitions.closed, true);
    assert.equal(f.snapshot().surfaceTerrain.closed, true);
    await f.run("floors-close");
    assert.equal(floors.calls.frees, 1);
    assert.equal(terrain.calls.frees, 1);
    assert.equal(JSON.stringify(f.state.logs).includes(secret), false);
  }
});

test("authenticated floor and palette read commands delegate without starting another account or diagnostic", async () => {
  const f = fixture();
  for (const [command, values] of [["standalone-underlay", ["0"]], ["standalone-overlay", ["0"]], ["standalone-palette-color", ["0", "0", "0"]]]) assert.equal((await f.run(command, values)).ok, false);
  assert.equal(f.state.accounts.length, 0);
  await f.run("standalone-login");
  const account = f.state.accounts[0];
  const calls = [];
  account.underlay = (id) => { calls.push(["underlay", id]); return { id, present: false }; };
  account.overlay = (id) => { calls.push(["overlay", id]); return { id, present: true }; };
  account.paletteColor = (...coordinates) => { calls.push(["palette", ...coordinates]); return 0; };
  assert.deepEqual(plain((await f.run("standalone-underlay", ["65535"])).data), { id: 65535, present: false });
  assert.deepEqual(plain((await f.run("standalone-overlay", ["0"])).data), { id: 0, present: true });
  assert.deepEqual(plain((await f.run("standalone-palette-color", ["3", "167", "0"])).data), { packedHsl: 0, rgbPrepared: false, renderedWorld: false });
  assert.deepEqual(calls, [["underlay", 65535], ["overlay", 0], ["palette", 3, 167, 0]]);
  assert.equal((await f.run("standalone-underlay", ["65536"])).error, "bounded floor definition ID required");
  assert.equal((await f.run("standalone-overlay", ["01"])).error, "bounded floor definition ID required");
  assert.equal((await f.run("standalone-palette-color", ["0", "168", "0"])).error, "bounded terrain coordinate required");
  account.underlay = () => { throw new Error(secret); };
  account.overlay = () => { throw new Error(secret); };
  account.paletteColor = () => { throw new Error(secret); };
  assert.equal((await f.run("standalone-underlay", ["0"])).error, "independent floor definitions unavailable");
  assert.equal((await f.run("standalone-overlay", ["0"])).error, "independent floor definitions unavailable");
  assert.equal((await f.run("standalone-palette-color", ["0", "0", "0"])).error, "independent surface palette unavailable");
  assert.equal(f.state.accounts.length, 1);
  assert.equal(f.state.floorLoads.length, 0);
  assert.equal(f.state.loads.length, 0);
  assert.equal(account.closed, 0);
  await f.run("standalone-session-close");
});

test("help lists account-free floor and packed-HSL commands without claiming a rendered scene", async () => {
  const f = fixture();
  const help = (await f.run("help")).data;
  for (const command of ["floors-probe", "floors-state", "floor-underlay", "floor-overlay", "floors-close", "terrain-probe", "surface-tile", "standalone-scene-tile", "surface-palette-color", "standalone-underlay", "standalone-overlay", "standalone-palette-color"]) assert.ok(help.includes(command));
  assert.equal(f.state.accounts.length, 0);
  assert.equal(f.state.floorLoads.length, 0);
  assert.equal(f.state.loads.length, 0);
  assert.deepEqual(f.state.logs, []);
});
