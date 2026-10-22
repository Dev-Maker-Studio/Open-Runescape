import test from "node:test";
import assert from "node:assert/strict";
import { relayUrls } from "../static/relay-ports.js";

test("default relay ports are 8110 for lobby and world and 8111 for JS5", () => {
  assert.deepEqual(relayUrls("?world=1", "127.0.0.1"), {
    lobby: "ws://127.0.0.1:8110/lobby",
    world: "ws://127.0.0.1:8110/world",
    js5: "ws://127.0.0.1:8111/js5",
    accountJs5: "ws://127.0.0.1:8110/js5",
  });
});

test("query ports select private relays", () => {
  assert.deepEqual(relayUrls("?world=1&lobbyPort=18110&js5Port=18111", "localhost"), {
    lobby: "ws://localhost:18110/lobby",
    world: "ws://localhost:18110/world",
    js5: "ws://localhost:18111/js5",
    accountJs5: "ws://localhost:18110/js5",
  });
});

test("ipv6 loopback is bracketed", () => {
  assert.equal(relayUrls("", "::1").js5, "ws://[::1]:8111/js5");
});

test("invalid ports are rejected", () => {
  for (const bad of ["0", "80", "65536", "abc", "18110x", "-1", ""]) {
    assert.throws(() => relayUrls(`?lobbyPort=${bad}`, "127.0.0.1"), /lobbyPort must be a port/);
    assert.throws(() => relayUrls(`?js5Port=${bad}`, "127.0.0.1"), /js5Port must be a port/);
  }
});
