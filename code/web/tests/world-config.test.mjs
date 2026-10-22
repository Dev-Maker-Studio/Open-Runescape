import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { loadWorldVarpLayout } from "../static/world-config.js";
import { PUBLIC_LOGIN_DEFAULTS } from "../static/login-config.js";

const relay = "ws://127.0.0.1:8110/js5";
const unavailable = { message: "verified world varp definitions unavailable" };

function fixture(ids = [0, 7, 40]) {
  const files = new Map(ids.map((id) => [id, new Uint8Array([id & 255, 7, 27])]));
  const arrays = [...files.values()];
  const result = {
    report: {
      revision: 727, subrevision: 1, prefetchCount: 27, index: 2, archive: 16,
      authenticated: false, referenceCrcVerified: true, referenceWhirlpoolVerified: true,
      archiveCrcVerified: true, archiveWhirlpoolVerified: false,
      fileSlots: ids.length ? ids.at(-1) + 1 : 0,
      containerBytes: 128, expandedBytes: ids.length * 3,
      files: ids.map((id) => ({ id, bytes: files.get(id).length })),
    },
    files,
  };
  const calls = [];
  const probe = async (...args) => { calls.push(args); return result; };
  return { result, arrays, calls, probe };
}

function released(f) {
  assert.equal(f.result.files.size, 0);
  for (const bytes of f.arrays) assert.ok(bytes.every((byte) => byte === 0));
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

test("world varp layout uses verified sparse file slots rather than the definition count", async () => {
  const f = fixture();
  const result = await loadWorldVarpLayout({ js5Relay: relay, probe: f.probe, timeoutMs: 1234 });
  assert.deepEqual(result, {
    varpCount: 41, definitionFiles: 3, source: "js5-index-2-archive-16-file-slots", authenticated: false,
  });
  assert.equal(Object.isFrozen(result), true);
  assert.equal(f.calls.length, 1);
  const [url, token, index, archive, options] = f.calls[0];
  assert.equal(url, relay);
  assert.equal(token, PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
  assert.equal(index, 2);
  assert.equal(archive, 16);
  assert.equal(options.timeoutMs, 1234);
  assert.ok(options.signal instanceof AbortSignal);
  assert.equal(options.signal.aborted, true);
  released(f);
});

test("empty archives and maximum sparse and dense varp spans stay bounded", async () => {
  for (const ids of [[], [65535], Array.from({ length: 65536 }, (_, id) => id)]) {
    const f = fixture(ids);
    const result = await loadWorldVarpLayout({ js5Relay: relay, probe: f.probe });
    assert.equal(result.varpCount, ids.length ? 65536 : 0);
    assert.equal(result.definitionFiles, ids.length);
    released(f);
  }
});

test("exact loopback JS5 routes are accepted without selecting other paths or hosts", async () => {
  for (const js5Relay of ["ws://localhost/js5", "wss://localhost:444/js5", "ws://[::1]:8110/js5", "wss://127.0.0.1/js5"]) {
    const f = fixture();
    await loadWorldVarpLayout({ js5Relay, probe: f.probe });
    assert.equal(f.calls[0][0], new URL(js5Relay).href);
    released(f);
  }
  for (const js5Relay of [
    undefined, "", "not a URL", "https://localhost/js5", "file:///js5", "ws://example.invalid/js5",
    "ws://localhost/world", "ws://localhost/js5/", "ws://localhost/%6as5", "ws://localhost/js5?index=2",
    "ws://localhost/js5#fragment", "ws://user:password@localhost/js5", "ws://localhost.evil.invalid/js5",
  ]) {
    const f = fixture();
    await assert.rejects(loadWorldVarpLayout({ js5Relay, probe: f.probe }), unavailable);
    assert.equal(f.calls.length, 0);
  }
});

test("bounded integer timeouts are checked before probe allocation", async () => {
  for (const timeoutMs of [0, -1, 30001, 1.5, NaN, Infinity, "100", null]) {
    const f = fixture();
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, timeoutMs, probe: f.probe }), unavailable);
    assert.equal(f.calls.length, 0);
  }
  for (const timeoutMs of [1, 30000]) {
    const f = fixture();
    await loadWorldVarpLayout({ js5Relay: relay, timeoutMs, probe: f.probe });
    assert.equal(f.calls[0][4].timeoutMs, timeoutMs);
    released(f);
  }
});

test("integrity revision and identity fields fail closed and wipe transferred definitions", async () => {
  const cases = [
    ["revision", 728], ["revision", "727"], ["subrevision", 0], ["prefetchCount", 26],
    ["index", 12], ["archive", 15], ["authenticated", true], ["authenticated", undefined],
    ["referenceCrcVerified", false], ["referenceCrcVerified", 1],
    ["referenceWhirlpoolVerified", false], ["archiveCrcVerified", false],
    ["archiveWhirlpoolVerified", "false"], ["archiveWhirlpoolVerified", undefined],
  ];
  for (const [key, value] of cases) {
    const f = fixture();
    f.result.report[key] = value;
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, probe: f.probe }), unavailable);
    released(f);
  }
  const f = fixture();
  f.result.report.archiveWhirlpoolVerified = true;
  await loadWorldVarpLayout({ js5Relay: relay, probe: f.probe });
  released(f);
});

test("file span container and expanded-byte bounds reject unsafe report shapes", async () => {
  const cases = [
    ["fileSlots", -1], ["fileSlots", 65537], ["fileSlots", 41.5], ["fileSlots", "41"],
    ["fileSlots", NaN], ["fileSlots", 3], ["fileSlots", 40], ["fileSlots", 42],
    ["containerBytes", 4], ["containerBytes", 12 * 1024 * 1024 + 1], ["containerBytes", Infinity],
    ["expandedBytes", -1], ["expandedBytes", 8 * 1024 * 1024 + 1], ["expandedBytes", 8],
    ["expandedBytes", 9.5], ["files", null], ["files", {}], ["files", []], ["files", new Array(65537)],
  ];
  for (const [key, value] of cases) {
    const f = fixture();
    f.result.report[key] = value;
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, probe: f.probe }), unavailable);
    released(f);
  }
  for (const containerBytes of [5, 12 * 1024 * 1024]) {
    const f = fixture();
    f.result.report.containerBytes = containerBytes;
    f.result.report.expandedBytes = 8 * 1024 * 1024;
    await loadWorldVarpLayout({ js5Relay: relay, probe: f.probe });
    released(f);
  }
});

test("sparse file IDs must be increasing unique bounded and exactly match the returned map", async () => {
  const mutations = [
    (f) => { f.result.report.files[0].id = -1; },
    (f) => { f.result.report.files[0].id = 0.5; },
    (f) => { f.result.report.files[0].id = "0"; },
    (f) => { f.result.report.files[2].id = 65536; },
    (f) => { f.result.report.files.reverse(); },
    (f) => { f.result.report.files[1].id = 0; },
    (f) => { f.result.report.files[1].bytes++; },
    (f) => { f.result.report.files[1].bytes = -1; },
    (f) => { f.result.report.files[1].bytes = "3"; },
    (f) => { f.result.report.files[1].bytes = NaN; },
    (f) => { f.result.report.files[1].bytes = 8 * 1024 * 1024 + 1; },
    (f) => { f.result.report.files[1] = null; },
    (f) => { f.result.files.delete(7); },
    (f) => { f.result.files.delete(7); f.result.files.set(8, f.arrays[1]); },
    (f) => { f.result.files.set(9, new Uint8Array([9])); },
    (f) => { f.result.files.set(7, [1, 2, 3]); },
  ];
  for (const mutate of mutations) {
    const f = fixture();
    mutate(f);
    const held = [...f.result.files.values()].filter((bytes) => bytes instanceof Uint8Array);
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, probe: f.probe }), unavailable);
    assert.equal(f.result.files.size, 0);
    for (const bytes of held) assert.ok(bytes.every((byte) => byte === 0));
  }
  for (const malformed of [null, undefined, {}, { report: null, files: new Map() }, { report: {}, files: {} }]) {
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, probe: async () => malformed }), unavailable);
  }
  for (const files of [null, undefined, [], {}, new Set()]) {
    const f = fixture();
    f.result.files = files;
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, probe: f.probe }), unavailable);
  }
});

test("metadata output contains no definitions raw reports tokens or extra fields", async () => {
  const f = fixture();
  f.result.report.secret = "FAKE_PRIVATE_REPORT";
  f.result.report.files[0].definition = "FAKE_DEFINITION";
  f.result.extra = "FAKE_EXTRA";
  const result = await loadWorldVarpLayout({ js5Relay: relay, probe: f.probe });
  assert.deepEqual(Object.keys(result).sort(), ["authenticated", "definitionFiles", "source", "varpCount"]);
  assert.equal(JSON.stringify(result).includes("FAKE_"), false);
  assert.equal(Object.hasOwn(result, "files"), false);
  assert.equal(Object.hasOwn(result, "report"), false);
  released(f);
});

test("zero-byte files are valid and still contribute their sparse logical span", async () => {
  const f = fixture([400]);
  f.result.files.set(400, new Uint8Array());
  f.result.report.files[0].bytes = 0;
  f.result.report.expandedBytes = 0;
  const result = await loadWorldVarpLayout({ js5Relay: relay, probe: f.probe });
  assert.equal(result.varpCount, 401);
  assert.equal(result.definitionFiles, 1);
  assert.equal(f.result.files.size, 0);
});

test("already-aborted and same-turn cancellation never start the probe", async () => {
  for (const before of [true, false]) {
    const f = fixture();
    const controller = new AbortController();
    if (before) controller.abort();
    const pending = loadWorldVarpLayout({ js5Relay: relay, signal: controller.signal, probe: f.probe });
    const rejected = assert.rejects(pending, unavailable);
    if (!before) controller.abort();
    await rejected;
    assert.equal(f.calls.length, 0);
  }
});

test("loading cancellation aborts the owned signal and wipes definitions returned after rejection", async () => {
  const f = fixture();
  const loading = deferred();
  const controller = new AbortController();
  let owned;
  const pending = loadWorldVarpLayout({ js5Relay: relay, signal: controller.signal, probe: (...args) => { owned = args[4].signal; return loading.promise; } });
  const rejected = assert.rejects(pending, unavailable);
  await setImmediate();
  assert.equal(owned.aborted, false);
  controller.abort();
  await rejected;
  assert.equal(owned.aborted, true);
  loading.resolve(f.result);
  await setImmediate();
  released(f);
});

test("timeout aborts loading and late fulfillment is released even when the probe ignores cancellation", async (t) => {
  t.mock.timers.enable({ apis: ["setTimeout"] });
  const f = fixture();
  const loading = deferred();
  let owned;
  const pending = loadWorldVarpLayout({ js5Relay: relay, timeoutMs: 15, probe: (...args) => { owned = args[4].signal; return loading.promise; } });
  const rejected = assert.rejects(pending, unavailable);
  await Promise.resolve();
  t.mock.timers.tick(15);
  await rejected;
  assert.equal(owned.aborted, true);
  loading.resolve(f.result);
  await setImmediate();
  released(f);
});

test("synchronous probe cancellation and errors keep a static failure and release any late result", async () => {
  const f = fixture();
  const controller = new AbortController();
  await assert.rejects(loadWorldVarpLayout({
    js5Relay: relay, signal: controller.signal,
    probe: () => { controller.abort(); return f.result; },
  }), unavailable);
  await setImmediate();
  released(f);
  for (const probe of [
    () => { throw new Error("FAKE_SECRET_SYNC"); },
    async () => { throw new Error("FAKE_SECRET_ASYNC"); },
  ]) {
    await assert.rejects(loadWorldVarpLayout({ js5Relay: relay, probe }), unavailable);
  }
});

test("successful and failed completion remove caller abort subscriptions", async () => {
  for (const success of [true, false]) {
    const f = fixture();
    const controller = new AbortController();
    let adds = 0;
    let removes = 0;
    const signal = {
      get aborted() { return controller.signal.aborted; },
      addEventListener(...args) { adds++; controller.signal.addEventListener(...args); },
      removeEventListener(...args) { removes++; controller.signal.removeEventListener(...args); },
    };
    const pending = loadWorldVarpLayout({ js5Relay: relay, signal, probe: success ? f.probe : async () => { throw new Error("FAKE_SECRET"); } });
    if (success) { await pending; released(f); }
    else await assert.rejects(pending, unavailable);
    assert.equal(adds, 1);
    assert.equal(removes, 1);
    controller.abort();
  }
});
