import test from "node:test";
import assert from "node:assert/strict";
import { observeBrowserLoginOptions } from "../static/browser-login-profile.js";

function options() {
  return {
    navigator: { platform: "Linux x86_64", hardwareConcurrency: 8 },
    performance: { memory: { jsHeapSizeLimit: 4294705152 } },
    viewport: { width: 765, height: 503 },
    fontIndex: 32,
    worldId: 22,
  };
}

test("observed browser profile reports measured heap and native-unavailable hardware", async () => {
  const result = await observeBrowserLoginOptions(options());
  assert.equal(result.machineInfo.operatingSystem, 3);
  assert.equal(result.machineInfo.x64Os, true);
  assert.equal(result.machineInfo.javaVendor, 4);
  assert.equal(result.machineInfo.javaBuild, 0);
  assert.equal(result.machineInfo.maxMemory, 4096);
  assert.equal(result.machineInfo.processors, 8);
  assert.equal(result.machineInfo.ram, 0);
  assert.equal(result.machineInfo.cpuType, "");
  assert.deepEqual(result.viewport, { width: 765, height: 503 });
  assert.equal(result.fontIndex, 32);
  assert.equal(result.worldId, 22);
  assert.equal(result.preferences.length, 35);
  assert.equal(result.preferences[23], 2);
});

test("Client Hints distinguish ARM without inventing native CPU identity", async () => {
  const input = options();
  input.navigator.userAgentData = {
    getHighEntropyValues: async (keys) => {
      assert.deepEqual(keys, ["platform", "architecture", "bitness"]);
      return { platform: "Linux", architecture: "arm", bitness: "64" };
    },
  };
  const result = await observeBrowserLoginOptions(input);
  assert.equal(result.preferences[23], 3);
  assert.equal(result.machineInfo.x64Os, false);
  assert.equal(result.machineInfo.cpuType, "");
});

test("missing measured metadata fails closed instead of fabricating defaults", async () => {
  for (const change of [
    (input) => { input.performance = {}; },
    (input) => { input.navigator.hardwareConcurrency = undefined; },
    (input) => { input.navigator.platform = "Unknown"; },
  ]) {
    const input = options();
    change(input);
    await assert.rejects(observeBrowserLoginOptions(input), /unavailable/);
  }
});

test("failed optional Client Hints retain actual legacy platform metadata", async () => {
  const input = options();
  input.navigator.userAgentData = { getHighEntropyValues: async () => { throw new Error("not granted"); } };
  const result = await observeBrowserLoginOptions(input);
  assert.equal(result.machineInfo.operatingSystem, 3);
  assert.equal(result.machineInfo.x64Os, true);
});

test("cancellation during Client Hints does not expose its reason or publish a profile", async () => {
  const input = options();
  const abort = new AbortController();
  let release;
  input.signal = abort.signal;
  input.navigator.userAgentData = { getHighEntropyValues: () => new Promise((resolve) => { release = resolve; }) };
  const pending = observeBrowserLoginOptions(input);
  const rejected = assert.rejects(pending, { message: "browser profile cancelled" });
  abort.abort("FAKE_PRIVATE_REASON");
  await rejected;
  release({ platform: "Linux", architecture: "x86", bitness: "64" });
  await assert.rejects(observeBrowserLoginOptions(input), { message: "browser profile cancelled" });
});
