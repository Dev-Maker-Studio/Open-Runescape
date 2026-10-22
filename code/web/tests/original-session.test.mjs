import test from "node:test";
import assert from "node:assert/strict";
import { openOriginalSession } from "../static/original-session.js";
import { fetchLoginCrcs, PUBLIC_LOGIN_DEFAULTS } from "../static/login-config.js";

function fixture({ failLobby = false, failWorld = false, failHandoff = false } = {}) {
  const calls = [];
  const state = {
    worldList: { version: 0, worlds: [{ worldNumber: 22, flags: 0 }] }, requests: [], stateReads: 0,
    initialWorld: { base: { x: 7904, y: 8456 }, mapSize: 104, mapKind: "standard", regionIds: [31620, 31621, 31876, 31877, 32132, 32133], chunkCount: 0 },
    terrainOpens: [], terrainClosed: 0, floorOpens: [], floorsClosed: 0, materialOpens: [], materialsClosed: 0,
  };
  let lobbyClosed = 0;
  let worldClosed = 0;
  const world = {
    ready: failWorld ? Promise.reject(new Error("FAKE_WORLD_SECRET")) : Promise.resolve({ worldReady: true, initialWorld: state.initialWorld }),
    state: () => ({ connected: !worldClosed, worldReady: !worldClosed, ...(state.inventoryEnabled ? { inventory: { containers: 1, slots: 8, tooltipBytes: 4, updates: 2, fullUpdates: 1, partialUpdates: 1, stops: 0 } } : {}) }),
    close: () => { worldClosed++; },
    varp: (id) => id * 2,
    container: (id, negativeKey) => {
      if (worldClosed) throw new Error("original world session is closed");
      if (!Number.isSafeInteger(id) || id < 0 || id > 65535 || typeof negativeKey !== "boolean") throw new Error("invalid inventory query");
      state.containerRead = [id, negativeKey];
      state.onContainer?.();
      if (worldClosed) throw new Error("original world session is closed");
      return id === 0 ? null : Object.freeze({ id, negativeKey, slots: 8 });
    },
    skill: (id) => {
      if (worldClosed) throw new Error("original world session is closed");
      if (!Number.isInteger(id) || id < 0 || id > 24) throw new Error("bounded original skill ID required");
      state.skillRead = id;
      return Object.freeze({ baseLevel: 1, experience: -5, visibleLevel: 255 });
    },
    playerStats: () => {
      if (worldClosed) throw new Error("original world session is closed");
      return Object.freeze({ skillCount: 25 });
    },
    item: (id, negativeKey, slot) => {
      if (worldClosed) throw new Error("original world session is closed");
      if (!Number.isSafeInteger(id) || id < 0 || id > 65535 || typeof negativeKey !== "boolean" || !Number.isSafeInteger(slot) || slot < 0 || slot > 65534) throw new Error("invalid inventory query");
      state.itemRead = [id, negativeKey, slot];
      state.onItem?.();
      if (worldClosed) throw new Error("original world session is closed");
      return id === 0 || slot >= 8 ? null : Object.freeze({ itemId: -1, amount: -2147483648, tooltip: "test" });
    },
  };
  world.ready.catch(() => {});
  const lobby = {
    ready: failLobby ? Promise.reject(new Error("FAKE_LOBBY_SECRET")) : Promise.resolve({ accepted: true }),
    state: () => { state.stateReads++; return { connected: !lobbyClosed, worldList: state.worldList }; },
    requestWorldList: (version) => { state.requests.push(version); state.onRequest?.(); },
    close: () => { lobbyClosed++; },
    startWorld: (relay, config, options) => {
      calls.push({ kind: "world", relay, config: JSON.parse(config), options });
      if (failHandoff) throw new Error("FAKE_TOKEN");
      state.inventoryEnabled = options?.inventory === true;
      return world;
    },
  };
  lobby.ready.catch(() => {});
  return {
    calls,
    state,
    closed: () => ({ lobbyClosed, worldClosed }),
    options: {
      fetchLayout: async () => ({ varpCount: 2719, definitionFiles: 2700 }),
      openFloors: async (options) => {
        state.floorOpens.push(options);
        const owner = {
          state: () => ({ accepted: !state.floorsClosed, closed: !!state.floorsClosed, scope: "floor-definitions-only", renderedWorld: false }),
          underlay: (id) => ({ id, present: true }),
          overlay: (id) => ({ id, overlayId: id, present: true }),
          close: () => { state.floorsClosed++; },
        };
        state.floorOwner = owner;
        return owner;
      },
      openMaterials: async (options) => {
        state.materialOpens.push(options);
        const owner = {
          state: () => ({ accepted: !state.materialsClosed, closed: !!state.materialsClosed, scope: "material-metadata-only", authenticated: false, texturePixelsDecoded: false, renderedWorld: false }),
          material: (id) => {
            if (state.materialsClosed) throw new Error("world material metadata owner is closed");
            if (!Number.isSafeInteger(id) || id < 0 || id > 65535) throw new Error("invalid world material id");
            state.materialRead = id;
            state.onMaterial?.();
            return id === 1 ? null : Object.freeze({ id, colorHsl: -32768, effectParam2: -2147483648 });
          },
          close: () => { state.materialsClosed++; },
        };
        state.materialOwner = owner;
        return owner;
      },
      openTerrain: async (options) => {
        state.terrainOpens.push(options);
        return {
          state: () => ({ accepted: !state.terrainClosed, closed: !!state.terrainClosed, scope: "surface-only", effectsDecoded: false, renderedWorld: false }),
          height: (...args) => { state.heightRead = args; return -320; },
          sceneHeight: (...args) => { state.sceneRead = args; return -640; },
          sceneTile: (...args) => {
            if (state.terrainClosed) throw new Error("world terrain owner is closed");
            state.tileRead = args;
            state.onTile?.();
            return Object.hasOwn(state, "tileValue") ? state.tileValue : Object.freeze({ underlayId: 255, overlayId: 0, renderFlags: 32, overlayShape: 11, overlayRotation: 3 });
          },
          paletteColor: (...args) => { state.paletteRead = args; return 12345; },
          close: () => { state.terrainClosed++; },
        };
      },
      takeConfig: async () => JSON.stringify({ lobby: { password: "FAKE_ONLY", seed: [1, 2, 3, 4] }, world: { seed: [5, 6, 7, 8] } }),
      openLobby: async (relay, config) => { calls.push({ kind: "lobby", relay, config: JSON.parse(config) }); return lobby; },
    },
  };
}

test("one private bootstrap supplies distinct lobby/world configurations without adding a token getter", async () => {
  const f = fixture();
  const session = await openOriginalSession(f.options);
  assert.equal(f.calls.length, 2);
  assert.equal(f.calls[0].kind, "lobby");
  assert.equal(f.calls[0].relay, "ws://127.0.0.1:8110/lobby");
  assert.equal(f.calls[1].kind, "world");
  assert.equal(f.calls[1].relay, "ws://127.0.0.1:8110/world");
  assert.deepEqual(f.calls[1].config, { seed: [5, 6, 7, 8] });
  const report = session.state();
  assert.equal(report.scope, "original-login-and-initial-world-only");
  assert.equal(report.gameplay, false);
  assert.equal(report.renderedWorld, false);
  assert.equal(f.calls[1].options, undefined);
  assert.equal(JSON.stringify(report).includes("FAKE_ONLY"), false);
  assert.deepEqual(Object.keys(session).sort(), ["close", "state"]);
  session.close();
  assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
});

test("lobby, handoff, and initial-world failures close owned sessions with static errors", async () => {
  for (const failure of [{ failLobby: true }, { failHandoff: true }, { failWorld: true }]) {
    const f = fixture(failure);
    await assert.rejects(openOriginalSession(f.options), /^Error: original session did not reach initial world state$/);
    assert.equal(f.closed().lobbyClosed, 1);
    assert.equal(f.closed().worldClosed, failure.failWorld ? 1 : 0);
  }
});

test("cancellation before bootstrap or while awaiting lobby prevents a later world login", async () => {
  const first = new AbortController();
  let opened = 0;
  const pending = openOriginalSession({ signal: first.signal, takeConfig: () => new Promise(() => {}), openLobby: async () => { opened++; } });
  first.abort("FAKE_PRIVATE_REASON");
  await assert.rejects(pending, /^Error: original session did not reach initial world state$/);
  assert.equal(opened, 0);

  const abort = new AbortController();
  let accept;
  let worldStarted = 0;
  let closed = 0;
  let notify;
  const allocated = new Promise((resolve) => { notify = resolve; });
  const lobby = {
    ready: new Promise((resolve) => { accept = resolve; }),
    close: () => { closed++; },
    startWorld: () => { worldStarted++; throw new Error("must not start"); },
  };
  const session = openOriginalSession({
    signal: abort.signal,
    takeConfig: async () => JSON.stringify({ lobby: {}, world: {} }),
    openLobby: async () => { notify(); return lobby; },
  });
  await allocated;
  await new Promise((resolve) => setImmediate(resolve));
  abort.abort("FAKE_PRIVATE_REASON");
  await assert.rejects(session, /^Error: original session did not reach initial world state$/);
  accept();
  await new Promise((resolve) => setImmediate(resolve));
  assert.ok(closed >= 1);
  assert.equal(worldStarted, 0);
});

test("browser account envelope derives current configurations before opening lobby", async () => {
  const f = fixture();
  const account = { identity: { kind: "username", username: "FAKE_USER" }, password: "FAKE_ONLY" };
  let derived;
  const session = await openOriginalSession({
    ...f.options,
    fontIndex: 32,
    worldId: 22,
    takeConfig: async () => JSON.stringify({ browserAccount: account }),
    observeOptions: async ({ fontIndex, worldId, signal }) => {
      assert.equal(fontIndex, 32);
      assert.equal(worldId, 22);
      assert.equal(signal, undefined);
      return { fontIndex, worldId, viewport: { width: 765, height: 503 } };
    },
    buildConfig: async (input, options) => {
      assert.deepEqual(input, account);
      assert.equal(options.js5Relay, "ws://127.0.0.1:8110/js5");
      assert.deepEqual(options.viewport, { width: 765, height: 503 });
      assert.equal(f.calls.length, 0);
      derived = { lobby: { password: input.password, seed: [1, 2, 3, 4] }, world: { worldId: 22, seed: [5, 6, 7, 8] } };
      return derived;
    },
  });
  assert.equal(f.calls.length, 2);
  assert.deepEqual(f.calls[0].config.seed, [1, 2, 3, 4]);
  assert.deepEqual(f.calls[1].config.seed, [5, 6, 7, 8]);
  assert.deepEqual(derived.lobby.seed, [0, 0, 0, 0]);
  assert.equal(derived.lobby.password, null);
  assert.equal(JSON.stringify(session.state()).includes("FAKE_ONLY"), false);
  session.close();
});

test("browser profile or JS5 configuration failure cannot allocate a game session", async () => {
  for (const failure of ["profile", "master"]) {
    const f = fixture();
    await assert.rejects(openOriginalSession({
      ...f.options,
      takeConfig: async () => JSON.stringify({ browserAccount: {} }),
      observeOptions: async () => {
        if (failure === "profile") throw new Error("FAKE_PRIVATE_REASON");
        return { fontIndex: 32 };
      },
      buildConfig: async () => { throw new Error("FAKE_PRIVATE_REASON"); },
    }), { message: "original session did not reach initial world state" });
    assert.equal(f.calls.length, 0);
  }
});

test("cancellation during browser JS5 configuration cannot start a late lobby", async () => {
  const f = fixture();
  const abort = new AbortController();
  let notify;
  let release;
  const building = new Promise((resolve) => { notify = resolve; });
  const pending = openOriginalSession({
    ...f.options,
    signal: abort.signal,
    takeConfig: async () => JSON.stringify({ browserAccount: {} }),
    observeOptions: async () => ({ fontIndex: 32 }),
    buildConfig: async (_, { signal }) => {
      assert.equal(signal, abort.signal);
      notify();
      return new Promise((resolve) => { release = resolve; });
    },
  });
  const rejected = assert.rejects(pending, { message: "original session did not reach initial world state" });
  await building;
  abort.abort("FAKE_PRIVATE_REASON");
  await rejected;
  const late = { lobby: { password: "FAKE_ONLY", seed: [1, 2, 3, 4] }, world: { identity: { kind: "username", username: "FAKE_USER" }, seed: [5, 6, 7, 8] } };
  release(late);
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(f.calls.length, 0);
  assert.deepEqual(late.lobby.seed, [0, 0, 0, 0]);
  assert.deepEqual(late.world.seed, [0, 0, 0, 0]);
  assert.equal(late.lobby.password, null);
  assert.equal(late.world.identity, null);
});

function browserFixture() {
  const f = fixture();
  f.owned = {
    lobby: { password: "FAKE_ONLY", identity: { kind: "username", username: "FAKE_USER" }, seed: [1, 2, 3, 4], indexCrcs: Array(36).fill(777) },
    world: { worldId: 22, identity: { kind: "username", username: "FAKE_USER" }, seed: [5, 6, 7, 8], clientRandomSeed: [9, 10], machineUid: [11, 12], indexCrcs: Array(36).fill(777) },
  };
  f.expected = structuredClone(f.owned);
  f.options = {
    ...f.options,
    takeConfig: async () => JSON.stringify({ browserAccount: { identity: { kind: "username", username: "FAKE_USER" }, password: "FAKE_ONLY" } }),
    observeOptions: async () => ({ fontIndex: 32, worldId: 22 }),
    buildConfig: async () => f.owned,
    fetchCrcs: async () => { throw new Error("unexpected world master probe"); },
  };
  return f;
}

const hostedMaster = () => ({
  revision: 727, subrevision: 1, prefetchCount: 27, authenticated: false, signatureBytes: 128,
  indices: Array.from({ length: 37 }, (_, index) => ({ index, crc: index % 2 ? -index - 1 : index + 100, revision: index + 1 })),
});

test("account custom-cache world refreshes only world CRCs from the fixed relay with the selected native font order", async () => {
  for (const fontIndex of [32, 34]) {
    const f = browserFixture();
    f.options.observeOptions = async () => ({ fontIndex, worldId: 999 });
    f.state.worldList = { version: 5, worlds: [
      { rawId: 22, worldNumber: 23, flags: 0 },
      { rawId: 5, worldNumber: 22, flags: 0x20, host: "secret.invalid", port: 1234 },
    ] };
    let probes = 0;
    f.options.fetchCrcs = (options) => fetchLoginCrcs({ ...options, probe: async (relay, token, { signal }) => {
      probes++;
      assert.equal(relay, "ws://127.0.0.1:8111/js5");
      assert.equal(token, PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
      assert.equal(signal, undefined);
      assert.equal(options.fontIndex, fontIndex);
      assert.equal(f.calls.length, 1);
      return hostedMaster();
    } });
    const session = await openOriginalSession(f.options);
    const entries = hostedMaster().indices;
    const expectedCrcs = [...entries.slice(0, 32), entries[fontIndex], entries[33], entries[35], entries[36]].map(({ crc }) => crc >>> 0);
    assert.equal(probes, 1);
    assert.deepEqual(f.calls[0].config, f.expected.lobby);
    assert.deepEqual(f.calls[1].config, { ...f.expected.world, indexCrcs: expectedCrcs });
    assert.equal(f.calls[1].relay, "ws://127.0.0.1:8110/world");
    assert.deepEqual(f.calls[1].options, { varpCount: 2719, inventory: true, playerStats: true });
    assert.deepEqual(f.state.requests, []);
    assert.deepEqual(f.owned.world.seed, [0, 0, 0, 0]);
    assert.deepEqual(f.owned.world.clientRandomSeed, [0, 0]);
    assert.equal(JSON.stringify(session.state()).includes("FAKE_ONLY"), false);
    session.close();
  }
});

test("account shared-cache world reuses lobby CRCs and never probes a world master", async () => {
  const f = browserFixture();
  f.state.worldList.worlds[0].flags = 0x10;
  const session = await openOriginalSession({ ...f.options, worldJs5Relay: "ws://secret.invalid/js5" });
  assert.deepEqual(f.calls[0].config.indexCrcs, f.expected.lobby.indexCrcs);
  assert.deepEqual(f.calls[1].config, f.expected.world);
  assert.deepEqual(f.state.requests, []);
  session.close();
});

test("account variable definitions use the selected fixed JS5 route before state handoff", async () => {
  for (const flags of [0, 0x20]) {
    const f = browserFixture();
    f.state.worldList.worlds[0].flags = flags;
    if (flags) f.options.fetchCrcs = async () => Array(36).fill(123);
    let probes = 0;
    f.options.fetchLayout = async ({ js5Relay, signal }) => {
      probes++;
      assert.equal(signal, undefined);
      assert.equal(js5Relay, flags ? "ws://127.0.0.1:8111/js5" : "ws://127.0.0.1:8110/js5");
      assert.equal(f.calls.length, 1);
      return { varpCount: 2719, definitionFiles: 2700 };
    };
    const session = await openOriginalSession(f.options);
    assert.equal(probes, 1);
    assert.deepEqual(f.calls[1].options, { varpCount: 2719, inventory: true, playerStats: true });
    assert.equal(session.state().scope, "original-login-varps-and-surface-terrain");
    assert.deepEqual(session.state().definitions, { varpCount: 2719, definitionFiles: 2700, source: "js5-index-2-archive-16-file-slots", authenticated: false });
    assert.equal(session.varp(4), 8);
    assert.deepEqual(Object.keys(session).sort(), ["close", "container", "cycleInput", "item", "material", "overlay", "paletteColor", "playerStats", "sceneHeight", "sceneTile", "skill", "state", "terrainHeight", "underlay", "varp"]);
    session.close();
  }
});

test("account unavailable or invalid variable layouts cannot allocate a world", async () => {
  const invalid = [null, {}, { varpCount: 3, definitionFiles: 4 }, ...[-1, 65537, NaN, 1.5, "2719"].map((varpCount) => ({ varpCount, definitionFiles: 0 }))];
  for (const layout of invalid) {
    const f = browserFixture();
    await assert.rejects(openOriginalSession({ ...f.options, fetchLayout: async () => layout }), { message: "original session did not reach initial world state (variable layout: original variable layout unavailable)" });
    assert.equal(f.calls.length, 1);
    assert.ok(f.closed().lobbyClosed >= 1);
  }
  const f = browserFixture();
  await assert.rejects(openOriginalSession({ ...f.options, fetchLayout: async () => { throw new Error("FAKE_PRIVATE_REASON"); } }), { message: "original session did not reach initial world state (variable layout: FAKE_PRIVATE_REASON)" });
  assert.equal(f.calls.length, 1);
});

test("account cancellation during definition fetch prevents a late state handoff", async () => {
  const f = browserFixture();
  const abort = new AbortController();
  let notify;
  let release;
  const started = new Promise((resolve) => { notify = resolve; });
  const pending = openOriginalSession({ ...f.options, signal: abort.signal, fetchLayout: ({ signal }) => {
    assert.equal(signal, abort.signal);
    notify();
    return new Promise((resolve) => { release = resolve; });
  } });
  const rejected = assert.rejects(pending, { message: "original session did not reach initial world state" });
  await started;
  abort.abort("FAKE_PRIVATE_REASON");
  await rejected;
  release({ varpCount: 2719, definitionFiles: 2700 });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(f.calls.length, 1);
});

test("account world preparation waits for a later decoded snapshot and requests the native current version once", async () => {
  for (const initial of [null, { version: -123, worlds: [{ worldNumber: 23, flags: 0 }] }]) {
    const f = browserFixture();
    f.state.worldList = initial;
    f.state.onRequest = () => setTimeout(() => { f.state.worldList = { version: 9, worlds: [{ worldNumber: 22, flags: 0 }] }; }, 5);
    const session = await openOriginalSession({ ...f.options, worldListTimeoutMs: 200 });
    assert.deepEqual(f.state.requests, [initial?.version ?? 0]);
    assert.deepEqual(f.calls.map(({ kind }) => kind), ["lobby", "world"]);
    session.close();
  }
});

test("account bounded preparation rejects invalid deadline, font, or built world ID before allocating a lobby", async () => {
  for (const invalid of [
    { worldListTimeoutMs: 0 }, { worldListTimeoutMs: 30001 }, { worldListTimeoutMs: "15000" },
    { observeOptions: async () => ({ fontIndex: 33 }) },
    ...[undefined, 0, -1, 65536, "22"].map((worldId) => ({ buildConfig: async () => ({ lobby: {}, world: { worldId } }) })),
  ]) {
    const f = browserFixture();
    await assert.rejects(openOriginalSession({ ...f.options, ...invalid }), { message: "original session did not reach initial world state" });
    assert.equal(f.calls.length, 0);
  }
});

test("account missing, malformed, or duplicate world metadata fails closed without a fallback or world socket", async () => {
  for (const list of [null, { version: 1, worlds: [] }, { version: 1, worlds: [{ worldNumber: 22, flags: "32" }] }, { version: 1, worlds: [{ worldNumber: 22, flags: 0 }, { worldNumber: 22, flags: 32 }] }, { version: "FAKE_PRIVATE_REASON", worlds: [] }]) {
    const f = browserFixture();
    f.state.worldList = list;
    await assert.rejects(openOriginalSession({ ...f.options, worldListTimeoutMs: 5 }), { message: "original session did not reach initial world state" });
    assert.equal(f.calls.length, 1);
    assert.ok(f.closed().lobbyClosed >= 1);
    const reads = f.state.stateReads;
    await new Promise((resolve) => setTimeout(resolve, 30));
    assert.equal(f.state.stateReads, reads);
  }
});

test("account world master errors and invalid CRCs cannot reuse the lobby table as a fallback", async () => {
  for (const fetchCrcs of [async () => { throw new Error("FAKE_PRIVATE_REASON"); }, async () => Array(35).fill(0), async () => Array(36), async () => Array(36).fill(-1)]) {
    const f = browserFixture();
    f.state.worldList.worlds[0].flags = 0x20;
    await assert.rejects(openOriginalSession({ ...f.options, fetchCrcs }), /^Error: original session did not reach initial world state \(world CRCs: /);
    assert.equal(f.calls.length, 1);
    assert.ok(f.closed().lobbyClosed >= 1);
  }
});

test("account cancellation during world-list wait or world-master fetch prevents a late handoff", async () => {
  for (const during of ["list", "master"]) {
    const f = browserFixture();
    const abort = new AbortController();
    let notify;
    let release;
    const started = new Promise((resolve) => { notify = resolve; });
    if (during === "list") {
      f.state.worldList = null;
      f.state.onRequest = () => notify();
    } else {
      f.state.worldList.worlds[0].flags = 0x20;
      f.options.fetchCrcs = ({ signal }) => { assert.equal(signal, abort.signal); notify(); return new Promise((resolve) => { release = resolve; }); };
    }
    const pending = openOriginalSession({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, { message: "original session did not reach initial world state" });
    await started;
    abort.abort("FAKE_PRIVATE_REASON");
    await rejected;
    release?.(Array(36).fill(123));
    f.state.worldList = { version: 0, worlds: [{ worldNumber: 22, flags: 0 }] };
    await new Promise((resolve) => setTimeout(resolve, 30));
    assert.equal(f.calls.length, 1);
    assert.deepEqual(f.owned.world.seed, [0, 0, 0, 0]);
  }
});

test("legacy exact fixture mode does not wait for a world list or fetch a master", async () => {
  const f = fixture();
  f.state.worldList = null;
  const session = await openOriginalSession({ ...f.options, worldListTimeoutMs: 0, fetchCrcs: async () => { throw new Error("must not probe"); }, fetchLayout: async () => { throw new Error("must not probe"); } });
  assert.deepEqual(f.state.requests, []);
  assert.deepEqual(f.calls[1].config, { seed: [5, 6, 7, 8] });
  session.close();
});

test("account terrain ownership uses only the selected route and decoded initial map after readiness", async () => {
  for (const flags of [0, 0x20]) {
    const f = browserFixture();
    f.state.worldList.worlds[0].flags = flags;
    if (flags) f.options.fetchCrcs = async () => Array(36).fill(123);
    const session = await openOriginalSession(f.options);
    assert.equal(f.state.terrainOpens.length, 1);
    const options = f.state.terrainOpens[0];
    assert.equal(options.js5Relay, flags ? "ws://127.0.0.1:8111/js5" : "ws://127.0.0.1:8110/js5");
    assert.equal(options.initialWorld, f.state.initialWorld);
    assert.equal(options.signal, undefined);
    assert.equal(options.floors, f.state.floorOwner);
    assert.deepEqual(f.state.floorOpens, [{ js5Relay: options.js5Relay, signal: undefined }]);
    assert.deepEqual(session.underlay(7), { id: 7, present: true });
    assert.deepEqual(session.overlay(9), { id: 9, overlayId: 9, present: true });
    assert.equal(session.paletteColor(2, 3, 4), 12345);
    assert.deepEqual(f.state.paletteRead, [2, 3, 4]);
    assert.equal(session.state().floorDefinitions.accepted, true);
    assert.equal(session.terrainHeight(31876, 0, 1, 2), -320);
    assert.deepEqual(f.state.heightRead, [31876, 0, 1, 2]);
    assert.equal(session.sceneHeight(1, 104, 3), -640);
    assert.deepEqual(f.state.sceneRead, [1, 104, 3]);
    const tile = session.sceneTile(3, 103, 3);
    assert.deepEqual(tile, { underlayId: 255, overlayId: 0, renderFlags: 32, overlayShape: 11, overlayRotation: 3 });
    assert.ok(Object.isFrozen(tile));
    assert.deepEqual(f.state.tileRead, [3, 103, 3]);
    f.state.tileValue = null;
    assert.equal(session.sceneTile(0, 0, 0), null);
    assert.deepEqual(f.state.tileRead, [0, 0, 0]);
    f.state.onTile = () => { throw new Error("world terrain sample unavailable"); };
    assert.throws(() => session.sceneTile(0, 0, 0), { message: "world terrain sample unavailable" });
    assert.equal(f.state.terrainOpens.length, 1);
    assert.equal(f.state.floorOpens.length, 1);
    assert.equal(f.calls.length, 2);
    assert.equal(session.state().terrain.effectsDecoded, false);
    assert.equal(session.state().terrain.accepted, true);
    assert.equal(session.state().renderedWorld, false);
    session.close();
    session.close();
    assert.equal(f.state.terrainClosed, 1);
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(session.state().floorDefinitions.closed, true);
    assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
    assert.equal(session.state().terrain.closed, true);
    assert.throws(() => session.sceneTile(0, 0, 0), { message: "world terrain owner is closed" });
  }
});

test("failed terrain preparation closes the authenticated world instead of claiming a rendered world", async () => {
  const f = browserFixture();
  let snapshot;
  await assert.rejects(openOriginalSession({ ...f.options,
    openTerrain: async () => { throw new Error("FAKE_PRIVATE_REASON"); },
    onState: (state) => { snapshot = state; },
  }), { message: "original session did not reach initial world state (terrain: FAKE_PRIVATE_REASON)" });
  assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  assert.equal(snapshot.gameplay, false);
  assert.equal(snapshot.renderedWorld, false);
  assert.equal(f.state.floorsClosed, 1);
  assert.equal(JSON.stringify(snapshot).includes("FAKE_PRIVATE"), false);
});

test("cancellation while terrain loads closes sessions and releases a late retained owner once", async () => {
  const f = browserFixture();
  const abort = new AbortController();
  let notify;
  let release;
  let terrainClosed = 0;
  const started = new Promise((resolve) => { notify = resolve; });
  const pending = openOriginalSession({ ...f.options, signal: abort.signal, openTerrain: (options) => {
    assert.equal(options.signal, abort.signal);
    notify();
    return new Promise((resolve) => { release = resolve; });
  } });
  const rejected = assert.rejects(pending, { message: "original session did not reach initial world state" });
  await started;
  abort.abort("FAKE_PRIVATE_REASON");
  await rejected;
  assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  release({ close: () => { terrainClosed++; } });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(terrainClosed, 1);
});

test("legacy fixtures never fetch or retain terrain", async () => {
  const f = fixture();
  const session = await openOriginalSession({ ...f.options, openTerrain: async () => { throw new Error("must not fetch"); } });
  assert.equal("terrain" in session.state(), false);
  assert.equal("terrainHeight" in session, false);
  assert.equal("sceneTile" in session, false);
  session.close();
});

test("account floor failure closes sessions without attempting terrain", async () => {
  const f = browserFixture();
  await assert.rejects(openOriginalSession({ ...f.options, openFloors: async () => { throw new Error("FAKE_PRIVATE_REASON"); } }), { message: "original session did not reach initial world state (floors: FAKE_PRIVATE_REASON)" });
  assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  assert.equal(f.state.terrainOpens.length, 0);
});

test("account cancellation while floors load releases a late floor owner once", async () => {
  const f = browserFixture();
  const abort = new AbortController();
  let notify;
  let release;
  let closed = 0;
  const started = new Promise((resolve) => { notify = resolve; });
  const pending = openOriginalSession({ ...f.options, signal: abort.signal, openFloors: ({ signal }) => {
    assert.equal(signal, abort.signal);
    notify();
    return new Promise((resolve) => { release = resolve; });
  } });
  const rejected = assert.rejects(pending, { message: "original session did not reach initial world state" });
  await started;
  abort.abort("FAKE_PRIVATE_REASON");
  await rejected;
  assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  assert.equal(f.state.terrainOpens.length, 0);
  release({ close: () => { closed++; } });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(closed, 1);
});

test("account cleanup attempts all owners even when a retained owner throws", async () => {
  for (const throwing of ["terrain", "floors", "materials"]) {
    const f = browserFixture();
    const options = { ...f.options };
    const wrap = (method, name) => async (...args) => {
      const owner = await method(...args);
      return { ...owner, close: () => { owner.close(); if (throwing === name) throw new Error("FAKE_PRIVATE_REASON"); } };
    };
    options.openFloors = wrap(options.openFloors, "floors");
    options.openTerrain = wrap(options.openTerrain, "terrain");
    options.openMaterials = wrap(options.openMaterials, "materials");
    const session = await openOriginalSession(options);
    session.close();
    session.close();
    assert.equal(f.state.terrainClosed, 1);
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(f.state.materialsClosed, 1);
    assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  }
});

test("browser account handoff alone opts into inventory with exact delegated namespaces and frozen snapshots", async () => {
  const f = browserFixture();
  const session = await openOriginalSession(f.options);
  assert.deepEqual(f.calls[1].options, { varpCount: 2719, inventory: true, playerStats: true });
  assert.deepEqual(session.state().world.inventory, { containers: 1, slots: 8, tooltipBytes: 4, updates: 2, fullUpdates: 1, partialUpdates: 1, stops: 0 });
  for (const negativeKey of [false, true]) {
    const container = session.container(65535, negativeKey);
    assert.deepEqual(container, { id: 65535, negativeKey, slots: 8 });
    assert.ok(Object.isFrozen(container));
    assert.deepEqual(f.state.containerRead, [65535, negativeKey]);
    const item = session.item(65535, negativeKey, 7);
    assert.deepEqual(item, { itemId: -1, amount: -2147483648, tooltip: "test" });
    assert.ok(Object.isFrozen(item));
    assert.deepEqual(f.state.itemRead, [65535, negativeKey, 7]);
    assert.equal(session.container(0, negativeKey), null);
    assert.equal(session.item(0, negativeKey, 0), null);
    assert.equal(session.item(65535, negativeKey, 65534), null);
  }
  for (const id of [undefined, null, "0", -1, 65536, 0n, true, NaN, 1.5, { valueOf() { assert.fail("must not coerce"); } }]) {
    assert.throws(() => session.container(id, false), { message: "invalid inventory query" });
    assert.throws(() => session.item(id, false, 0), { message: "invalid inventory query" });
  }
  for (const namespace of [undefined, null, "false", 0, 1, {}, 0n]) {
    assert.throws(() => session.container(0, namespace), { message: "invalid inventory query" });
    assert.throws(() => session.item(0, namespace, 0), { message: "invalid inventory query" });
  }
  for (const slot of [undefined, null, "0", -1, 65535, true, 0n, NaN, 1.5]) assert.throws(() => session.item(1, false, slot), { message: "invalid inventory query" });
  session.close();
  assert.throws(() => session.container(1, false), { message: "original world session is closed" });
  assert.throws(() => session.item(1, false, 0), { message: "original world session is closed" });
});

test("browser account handoff delegates frozen player stats and skill snapshots and closes them", async () => {
  const f = browserFixture();
  const session = await openOriginalSession(f.options);
  assert.equal(f.calls[1].options.playerStats, true);
  const skill = session.skill(24);
  assert.deepEqual(skill, { baseLevel: 1, experience: -5, visibleLevel: 255 });
  assert.ok(Object.isFrozen(skill));
  assert.equal(f.state.skillRead, 24);
  assert.equal(session.playerStats().skillCount, 25);
  assert.ok(Object.isFrozen(session.playerStats()));
  assert.throws(() => session.playerStats(1), { message: "invalid original player stats query" });
  for (const id of [-1, 25, 1.5, "0", null, undefined]) assert.throws(() => session.skill(id), { message: "bounded original skill ID required" });
  session.close();
  assert.throws(() => session.skill(0), { message: "original world session is closed" });
  assert.throws(() => session.playerStats(), { message: "original world session is closed" });
});

test("session inventory wrappers cannot publish values from a world closed during an accessor", async () => {
  for (const method of ["container", "item"]) {
    const f = browserFixture();
    const session = await openOriginalSession(f.options);
    f.state[method === "container" ? "onContainer" : "onItem"] = () => session.close();
    assert.throws(() => session[method](1, false, 0), { message: "original world session is closed" });
    assert.equal(f.state.materialsClosed, 1);
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(f.state.terrainClosed, 1);
  }
});

test("account material metadata uses only the selected fixed route and delegates frozen native records", async () => {
  for (const flags of [0, 0x20]) {
    const f = browserFixture();
    f.state.worldList.worlds[0].flags = flags;
    if (flags) f.options.fetchCrcs = async () => Array(36).fill(123);
    const openMaterials = f.options.openMaterials;
    f.options.openMaterials = (options) => {
      assert.equal(f.state.floorOpens.length, 1);
      assert.equal(f.state.terrainOpens.length, 1);
      assert.equal(f.calls.length, 2);
      return openMaterials(options);
    };
    const session = await openOriginalSession(f.options);
    assert.ok(Object.isFrozen(session));
    assert.deepEqual(f.state.materialOpens, [{ js5Relay: flags ? "ws://127.0.0.1:8111/js5" : "ws://127.0.0.1:8110/js5", signal: undefined }]);
    assert.equal(session.state().materialDefinitions.accepted, true);
    assert.equal(session.state().materialDefinitions.authenticated, false);
    assert.equal(session.state().materialDefinitions.texturePixelsDecoded, false);
    assert.equal(session.state().materialDefinitions.renderedWorld, false);
    const value = session.material(0);
    assert.deepEqual(value, { id: 0, colorHsl: -32768, effectParam2: -2147483648 });
    assert.ok(Object.isFrozen(value));
    assert.equal(session.material(1), null);
    for (const id of [undefined, null, "0", true, 1n, -1, 65536, NaN, 1.5, { valueOf() { assert.fail("must not coerce"); } }]) assert.throws(() => session.material(id), { message: "invalid world material id" });
    f.state.onMaterial = () => { throw new Error("world material metadata record unavailable"); };
    assert.throws(() => session.material(0), { message: "world material metadata record unavailable" });
    assert.equal(f.state.materialOpens.length, 1);
    assert.equal(JSON.stringify(session.state()).includes("FAKE_"), false);
    session.close();
    session.close();
    assert.equal(f.state.materialsClosed, 1);
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(f.state.terrainClosed, 1);
    assert.equal(session.state().materialDefinitions.closed, true);
    assert.throws(() => session.material(0), { message: "world material metadata owner is closed" });
  }
});

test("material preparation failure rolls back floors, terrain and sessions with a static account-free report", async () => {
  const f = browserFixture();
  let snapshot;
  await assert.rejects(openOriginalSession({ ...f.options,
    openMaterials: async () => { throw new Error("FAKE_PRIVATE_REASON"); },
    onState: (value) => { snapshot = value; },
  }), { message: "original session did not reach initial world state (materials: FAKE_PRIVATE_REASON)" });
  assert.equal(f.state.floorsClosed, 1);
  assert.equal(f.state.terrainClosed, 1);
  assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  assert.equal(snapshot.renderedWorld, false);
  assert.equal(snapshot.gameplay, false);
  assert.equal(JSON.stringify(snapshot).includes("FAKE_"), false);
});

test("material cancellation rejects ignored loaders immediately and closes every late owner only once", async () => {
  for (const late of ["resolve", "reject"]) {
    const f = browserFixture();
    const abort = new AbortController();
    let notify;
    let release;
    let reject;
    let materialCloses = 0;
    const started = new Promise((resolve) => { notify = resolve; });
    const pending = openOriginalSession({ ...f.options, signal: abort.signal, openMaterials: (options) => {
      assert.equal(options.signal, abort.signal);
      notify();
      return new Promise((yes, no) => { release = yes; reject = no; });
    } });
    const rejected = assert.rejects(pending, { message: "original session did not reach initial world state" });
    await started;
    abort.abort("FAKE_PRIVATE_REASON");
    await rejected;
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(f.state.terrainClosed, 1);
    assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
    if (late === "resolve") release({ close() { materialCloses++; throw new Error("FAKE_PRIVATE_REASON"); } });
    else reject(new Error("FAKE_PRIVATE_REASON"));
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(materialCloses, late === "resolve" ? 1 : 0);
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(f.state.terrainClosed, 1);
  }
});

test("cancellation in a resolved preparation result cannot double close or continue to the next owner", async () => {
  for (const kind of ["Floors", "Terrain", "Materials"]) {
    const f = browserFixture();
    const abort = new AbortController();
    const key = `open${kind}`;
    const original = f.options[key];
    f.options[key] = async (options) => {
      const owner = await original(options);
      queueMicrotask(() => abort.abort("FAKE_PRIVATE_REASON"));
      return owner;
    };
    await assert.rejects(openOriginalSession({ ...f.options, signal: abort.signal }), { message: "original session did not reach initial world state" });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(f.state.floorsClosed, 1);
    assert.equal(f.state.terrainClosed, kind === "Floors" ? 0 : 1);
    assert.equal(f.state.materialsClosed, kind === "Materials" ? 1 : 0);
    assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 1 });
  }
});

test("invalid selected browser JS5 routes fail before definitions and never fall back to untrusted world metadata", async () => {
  for (const flags of [0, 0x20]) {
    for (const relay of [null, 123, {}, "ws://private.invalid/js5", "ws://user:password@localhost/js5", "ws://127.1/js5", "ws://LOCALHOST/js5", "ws://localhost/js5?secret", "ws://localhost/js5#", "ws://localhost:0/js5", "ws://localhost:65536/js5", "ws://localhost/x/../js5"]) {
      const f = browserFixture();
      f.state.worldList.worlds[0] = { worldNumber: 22, flags, host: "private.invalid", port: 1234 };
      let layoutReads = 0;
      let crcReads = 0;
      await assert.rejects(openOriginalSession({ ...f.options,
        [flags ? "worldJs5Relay" : "js5Relay"]: relay,
        fetchLayout: async () => { layoutReads++; return { varpCount: 0, definitionFiles: 0 }; },
        fetchCrcs: async () => { crcReads++; return Array(36).fill(0); },
      }), { message: "original session did not reach initial world state" });
      assert.equal(layoutReads, 0);
      assert.equal(crcReads, 0);
      assert.equal(f.calls.length, 1);
      assert.equal(f.state.materialOpens.length, 0);
      assert.equal(f.state.floorOpens.length, 0);
      assert.equal(f.state.terrainOpens.length, 0);
      assert.deepEqual(f.closed(), { lobbyClosed: 1, worldClosed: 0 });
    }
  }
});

test("legacy prebuilt configurations do not add material, floor or terrain preparation or capabilities", async () => {
  const f = fixture();
  const fail = () => { throw new Error("must not load in legacy mode"); };
  const session = await openOriginalSession({ ...f.options, openMaterials: fail, openFloors: fail, openTerrain: fail, js5Relay: "ws://unused.invalid/js5", worldJs5Relay: "ws://unused.invalid/js5" });
  assert.deepEqual(Object.keys(session).sort(), ["close", "state"]);
  assert.equal("materialDefinitions" in session.state(), false);
  assert.equal(f.state.materialOpens.length, 0);
  assert.equal(f.state.floorOpens.length, 0);
  assert.equal(f.state.terrainOpens.length, 0);
  session.close();
});

test("invalid private session envelopes never allocate a lobby session", async () => {
  for (const config of [null, [], {}, { lobby: {} }, { lobby: {}, world: {}, token: "FAKE_SECRET" }, { lobby: [], world: {} }, { lobby: {}, world: 1 }]) {
    const f = fixture();
    await assert.rejects(openOriginalSession({ ...f.options, takeConfig: async () => JSON.stringify(config) }), /did not reach initial world state/);
    assert.equal(f.calls.length, 0);
  }
});

test("a failed underwater open is reported in the state instead of silently going unlit", async () => {
  const f = browserFixture();
  const open = f.options.openTerrain;
  const session = await openOriginalSession({ ...f.options, withUnderwater: true, openTerrain: async (options) => {
    if (options.underwater) throw new Error("underwater archive missing");
    return open(options);
  } });
  const state = session.state();
  assert.equal(state.lit, false);
  assert.equal(state.underwaterError, "underwater archive missing");
  session.close();
});

test("a lit session reports no underwater error", async () => {
  const f = browserFixture();
  const session = await openOriginalSession({ ...f.options, withUnderwater: true });
  const state = session.state();
  assert.equal(state.lit, true);
  assert.equal("underwaterError" in state, false);
  session.close();
});
