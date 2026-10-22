import test from "node:test";
import assert from "node:assert/strict";
import { coreScriptProbe } from "../static/js5-probe.js";
import { createServer } from "node:http";
import { once } from "node:events";

const origin = "http://127.0.0.1:8092";

test("supplied script bytes reach wasm through bounded same-origin fixture fetch", async () => {
  const report = await coreScriptProbe("/files/test.bin", {
    origin,
    fetcher: async (url, options) => {
      assert.equal(url, `${origin}/files/test.bin`);
      assert.equal(options.cache, "no-store");
      assert.equal(options.redirect, "error");
      assert.equal(options.credentials, "omit");
      assert.ok(options.signal);
      return new Response(new ReadableStream({ start(controller) {
        controller.enqueue(new Uint8Array([1, 2]));
        controller.enqueue(new Uint8Array([3]));
        controller.close();
      } }));
    },
    load: async () => ({ runCoreScript(bytes) {
      assert.deepEqual([...bytes], [1, 2, 3]);
      return JSON.stringify({ scope: "pure-core-no-host", longs: ["9223372036854775807"] });
    } }),
  });
  assert.deepEqual(report, { scope: "pure-core-no-host", longs: ["9223372036854775807"] });
});

test("script fetch cancels oversized body before allocating wasm state", async () => {
  let cancelled = false;
  let loaded = false;
  await assert.rejects(coreScriptProbe("/files/test.bin", {
    origin,
    fetcher: async () => new Response(new ReadableStream({
      start(controller) { controller.enqueue(new Uint8Array(64 * 1024 + 1)); },
      cancel() { cancelled = true; },
    })),
    load: async () => { loaded = true; },
  }), /byte limit/);
  assert.equal(cancelled, true);
  assert.equal(loaded, false);
});

test("real fixture fetch rejects redirects before requesting the destination", async (t) => {
  let destinationRequests = 0;
  const server = createServer((request, response) => {
    if (request.url === "/files/redirect") {
      response.writeHead(302, { Location: "/not-a-fixture" });
    } else { destinationRequests++; response.writeHead(200); }
    response.end();
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => { server.closeAllConnections(); server.close(); });
  await assert.rejects(coreScriptProbe("/files/redirect", {
    origin: `http://127.0.0.1:${server.address().port}`,
    load: async () => { throw new Error("must not instantiate redirected script"); },
  }), /fetch failed/);
  assert.equal(destinationRequests, 0);
});

test("script diagnostic cannot fetch external or arbitrary page resources", async () => {
  let fetched = false;
  for (const path of ["https://example.com/files/test.bin", "/files/../bridge.js", "/bridge.js", "/files/test.bin?token=x", "/files/test.bin#x"]) {
    await assert.rejects(coreScriptProbe(path, {
      origin,
      fetcher: async () => { fetched = true; },
    }), /same-origin/);
  }
  assert.equal(fetched, false);
});
