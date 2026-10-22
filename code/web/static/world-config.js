import { assetProbe } from "./js5-probe.js";
import { PUBLIC_LOGIN_DEFAULTS } from "./login-config.js";

function integer(value, min, max) {
  return Number.isSafeInteger(value) && value >= min && value <= max;
}

function release(result) {
  if (!(result?.files instanceof Map)) return;
  for (const bytes of result.files.values()) if (bytes instanceof Uint8Array) bytes.fill(0);
  result.files.clear();
}

function layout(result) {
  const report = result?.report;
  if (!report || report.revision !== 727 || report.subrevision !== 1 || report.prefetchCount !== 27 || report.index !== 2 || report.archive !== 16 || report.authenticated !== false || report.referenceCrcVerified !== true || report.referenceWhirlpoolVerified !== true || report.archiveCrcVerified !== true || typeof report.archiveWhirlpoolVerified !== "boolean") throw new Error();
  if (!integer(report.fileSlots, 0, 65536) || !integer(report.containerBytes, 5, 12 * 1024 * 1024) || !integer(report.expandedBytes, 0, 8 * 1024 * 1024) || !Array.isArray(report.files) || report.files.length > 65536 || !(result.files instanceof Map) || result.files.size !== report.files.length) throw new Error();
  let previous = -1;
  let bytes = 0;
  for (const file of report.files) {
    if (!integer(file.id, 0, 65535) || file.id <= previous || !integer(file.bytes, 0, 8 * 1024 * 1024)) throw new Error();
    const data = result.files.get(file.id);
    if (!(data instanceof Uint8Array) || data.length !== file.bytes) throw new Error();
    bytes += data.length;
    if (bytes > report.expandedBytes) throw new Error();
    previous = file.id;
  }
  if (previous + 1 !== report.fileSlots) throw new Error();
  return Object.freeze({ varpCount: report.fileSlots, definitionFiles: report.files.length, source: "js5-index-2-archive-16-file-slots", authenticated: false });
}

export async function loadWorldVarpLayout({ js5Relay, signal, probe = assetProbe, timeoutMs = 30000 } = {}) {
  let result;
  let timer;
  let abort;
  const owner = new AbortController();
  const cancel = () => owner.abort();
  try {
    if (signal?.aborted || !integer(timeoutMs, 1, 30000)) throw new Error();
    const url = new URL(js5Relay);
    if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== "/js5" || url.search || url.hash || url.username || url.password) throw new Error();
    signal?.addEventListener("abort", cancel, { once: true });
    if (signal?.aborted) cancel();
    const cancelled = new Promise((_, reject) => {
      abort = () => reject(new Error());
      owner.signal.addEventListener("abort", abort, { once: true });
      if (owner.signal.aborted) abort();
    });
    timer = setTimeout(cancel, timeoutMs);
    const fetching = Promise.resolve().then(() => {
      if (owner.signal.aborted) throw new Error();
      return probe(url.href, PUBLIC_LOGIN_DEFAULTS.js5AuthToken, 2, 16, { signal: owner.signal, timeoutMs });
    });
    fetching.then((late) => { if (owner.signal.aborted) release(late); }, () => {});
    result = await Promise.race([fetching, cancelled]);
    if (owner.signal.aborted) throw new Error();
    return layout(result);
  } catch {
    throw new Error("verified world varp definitions unavailable");
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", cancel);
    owner.signal.removeEventListener("abort", abort);
    owner.abort();
    release(result);
  }
}
