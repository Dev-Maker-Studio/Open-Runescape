import { protocolProbe } from "./js5-probe.js";

const MAX_CONFIG_BYTES = 64 * 1024;
const MEBIBYTE = 1048576;
const CP1252_EXTRA = "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ";
const MACHINE_KEYS = [
  "operatingSystem", "x64Os", "osVendor", "javaVendor", "javaBuild", "javaSubBuild", "javaUpdate",
  "unknownFlag", "maxMemory", "processors", "ram", "cpuClock", "gpuModel", "gpuDriverVersion",
  "directxVersion", "directxLetter", "dxDriverMonth", "dxDriverYear", "cpuType", "cpuData",
  "cpuCores", "cpuFamilyId", "cpuFeatureFlags", "cpuModelInfo",
];

export const PUBLIC_LOGIN_DEFAULTS = Object.freeze({
  affiliateP: "wwGlrZHF5gKN6D3mDdihco3oPeYN2KFybL9hUUFqOvk",
  loginToken: "29EDD9FDC775629058FBBF106C5B0E0A3A8028FE0037D1737B8EC3EA2F4E8B8FD6F54EF2F4E65862",
  js5AuthToken: "ev9+VAp5/tMKeNR/7MOuH6lKWS+rGkHK",
  sessionToken: 1451434218,
  affiliate: 0,
  gameId: 0,
  language: 0,
  loginType: 38888 & 255,
});

class ConfigurationError extends Error {}

function fail(message) { throw new ConfigurationError(message); }

function record(value, keys, message) {
  if (!value || typeof value !== "object" || Array.isArray(value) || ![Object.prototype, null].includes(Object.getPrototypeOf(value))) fail(message);
  const own = Reflect.ownKeys(value);
  if (own.length !== keys.length || own.some((key) => !keys.includes(key) || !Object.hasOwn(Object.getOwnPropertyDescriptor(value, key), "value"))) fail(message);
  return value;
}

function integer(value, min, max, message) {
  if (!Number.isSafeInteger(value) || value < min || value > max) fail(message);
  return value;
}

function text(value, min, max, message) {
  if (typeof value !== "string" || value.length < min || value.length > max) fail(message);
  for (const character of value) {
    const code = character.codePointAt(0);
    if (!(code >= 1 && code <= 127 || code >= 160 && code <= 255 || CP1252_EXTRA.includes(character))) fail(message);
  }
  return value;
}

function words(value, length, max, message) {
  if (!Array.isArray(value) || value.length !== length) fail(message);
  const copy = [];
  for (let index = 0; index < length; index++) copy.push(integer(value[index], 0, max, message));
  return copy;
}

function privateAccount(input) {
  record(input, ["identity", "password"], "invalid private browser account");
  record(input.identity, ["kind", "username"], "unsupported private browser identity");
  if (input.identity.kind !== "username") fail("unsupported private browser identity");
  return {
    identity: { kind: "username", username: text(input.identity.username, 1, 320, "invalid private browser identity") },
    password: text(input.password, 1, 86, "invalid private browser password"),
  };
}

function machine(value) {
  if (value === undefined) fail("browser machine information unavailable");
  record(value, MACHINE_KEYS, "invalid browser machine information");
  const copy = {};
  for (const key of ["x64Os", "unknownFlag"]) {
    if (typeof value[key] !== "boolean") fail("invalid browser machine information");
    copy[key] = value[key];
  }
  for (const [key, min, max] of [
    ["operatingSystem", 1, 4], ["osVendor", 0, 255], ["javaVendor", 1, 4],
    ["javaBuild", 0, 255], ["javaSubBuild", 0, 255], ["javaUpdate", 0, 255],
    ["maxMemory", 1, 65535], ["processors", 0, 255], ["ram", 0, 0xffffff],
    ["cpuClock", 0, 65535], ["dxDriverMonth", 0, 12], ["dxDriverYear", 0, 65535],
    ["cpuCores", 0, 255], ["cpuFamilyId", 0, 255], ["cpuModelInfo", 0, 0xffffffff],
  ]) copy[key] = integer(value[key], min, max, "invalid browser machine information");
  for (const [key, max] of [
    ["gpuModel", 40], ["gpuDriverVersion", 40], ["directxVersion", 10], ["directxLetter", 10],
    ["cpuType", 64], ["cpuData", 128],
  ]) copy[key] = text(value[key], 0, max, "invalid browser machine information");
  if (copy.cpuType === "darkan-headless-client") fail("unsupported browser machine policy marker");
  copy.cpuFeatureFlags = words(value.cpuFeatureFlags, 3, 0xffffffff, "invalid browser machine information");
  return copy;
}

export function deriveBrowserMachineInfo({ platform = "", architecture = "", bitness = "", hardwareConcurrency, jsHeapSizeLimit } = {}) {
  if (typeof platform !== "string" || platform.length > 128 || typeof architecture !== "string" || architecture.length > 32 || typeof bitness !== "string" || bitness.length > 8) fail("invalid browser platform metadata");
  if (!Number.isSafeInteger(jsHeapSizeLimit) || jsHeapSizeLimit <= 0) fail("browser runtime memory limit unavailable");
  const maxMemory = integer(Math.floor(jsHeapSizeLimit / MEBIBYTE) + 1, 1, 65535, "browser runtime memory limit unsupported");
  const processors = integer(hardwareConcurrency, 1, 255, "browser processor metadata unavailable");
  const name = platform.toLowerCase();
  const operatingSystem = name.startsWith("win") ? 1 : name.startsWith("mac") ? 2 : name.startsWith("linux") ? 3 : 4;
  return {
    operatingSystem,
    x64Os: architecture === "x86" && bitness === "64" || /^(linux )?(x86_64|amd64)$/i.test(platform),
    osVendor: 0,
    javaVendor: 4,
    javaBuild: 0,
    javaSubBuild: 0,
    javaUpdate: 0,
    unknownFlag: false,
    maxMemory,
    processors,
    ram: 0,
    cpuClock: 0,
    gpuModel: "",
    gpuDriverVersion: "",
    directxVersion: "",
    directxLetter: "",
    dxDriverMonth: 0,
    dxDriverYear: 0,
    cpuType: "",
    cpuData: "",
    cpuCores: 0,
    cpuFamilyId: 0,
    cpuFeatureFlags: [0, 0, 0],
    cpuModelInfo: 0,
  };
}

export function defaultLoginPreferences({ maxMemory, processors, isArm } = {}) {
  integer(maxMemory, 1, 65535, "browser runtime memory limit unavailable");
  integer(processors, 1, 255, "browser processor metadata unavailable");
  if (typeof isArm !== "boolean") fail("browser preference architecture unavailable");
  return [
    28, 0, 0, 3, 0, 0, 1, 1, 1, 1, 1, 1, 2, 0, maxMemory < 245 ? 0 : 2,
    2, 0, 1, 1, 1, 3, 0, 1, isArm ? 3 : 2, 1, 0, processors > 1 ? 4 : 2,
    0, 0, 127, 127, 127, 127, 127, 1,
  ];
}

export function browserLoginProvenance() {
  return Object.freeze({
    profile: "browser-runtime-v1",
    loaderDefaults: "public-native-loader-727.1",
    memory: "observed-browser-runtime-heap-limit-not-physical-ram-or-jvm",
    processors: "browser-reported-logical-concurrency-not-native-cpuid",
    java: "not-a-jvm-other-vendor-zero-version",
    optionalHardware: "native-unavailable-zero-and-empty-fields",
    machineUid: "ephemeral-browser-instance-not-hardware-attestation",
    randomDat: "native-unavailable-24-byte-sentinel",
    indexCrcs: "current-js5-master-native-resource-order",
    authenticated: false,
  });
}

function crcs(report, fontIndex) {
  record(report, ["revision", "subrevision", "prefetchCount", "indices", "signatureBytes", "authenticated"], "invalid hosted JS5 master report");
  if (report.revision !== 727 || report.subrevision !== 1 || report.prefetchCount !== 27 || report.authenticated !== false) fail("unsupported hosted JS5 master report");
  integer(report.signatureBytes, 0, 65535, "invalid hosted JS5 master report");
  if (!Array.isArray(report.indices) || report.indices.length < 37 || report.indices.length > 255) fail("incomplete hosted JS5 master report");
  const entries = [];
  for (let index = 0; index < report.indices.length; index++) {
    const entry = record(report.indices[index], ["index", "crc", "revision"], "invalid hosted JS5 master index");
    if (entry.index !== index) fail("invalid hosted JS5 master index order");
    integer(entry.revision, -0x80000000, 0x7fffffff, "invalid hosted JS5 master revision");
    entries.push(integer(entry.crc, -0x80000000, 0x7fffffff, "invalid hosted JS5 master CRC") >>> 0);
  }
  return [...entries.slice(0, 32), entries[fontIndex], entries[33], entries[35], entries[36]];
}

function cancelled(signal) {
  if (signal?.aborted) fail("browser login configuration cancelled");
}

async function master(probe, relay, signal) {
  let abort;
  let timer;
  try {
    cancelled(signal);
    if (typeof relay !== "string" || relay.length > 512) fail("hosted JS5 master unavailable");
    const url = new URL(relay);
    if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== "/js5" || url.search || url.hash || url.username || url.password) fail("hosted JS5 master unavailable");
    return await Promise.race([
      Promise.resolve().then(() => {
        cancelled(signal);
        return probe(url.href, PUBLIC_LOGIN_DEFAULTS.js5AuthToken, { signal });
      }),
      new Promise((_, reject) => {
        abort = () => reject(new Error("browser login configuration cancelled"));
        signal?.addEventListener("abort", abort, { once: true });
        timer = setTimeout(() => reject(new Error("hosted JS5 master unavailable")), 30000);
      }),
    ]);
  } catch {
    cancelled(signal);
    fail("hosted JS5 master unavailable");
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", abort);
  }
}

export async function fetchLoginCrcs({ js5Relay = "ws://127.0.0.1:8110/js5", fontIndex, signal, probe = protocolProbe } = {}) {
  try {
    cancelled(signal);
    if (![32, 34].includes(fontIndex)) fail("browser font index selection unavailable");
    const indexCrcs = crcs(await master(probe, js5Relay, signal), fontIndex);
    cancelled(signal);
    return indexCrcs;
  } catch (error) {
    if (error instanceof ConfigurationError) throw error;
    fail("invalid hosted JS5 master report");
  }
}

export async function buildLoginConfig(privateInput, {
  js5Relay = "ws://127.0.0.1:8110/js5", viewport, machineInfo, machineUid, preferences, fontIndex, worldId,
  signal, probe = protocolProbe, crypto = globalThis.crypto,
} = {}) {
  let account;
  let complete = false;
  const temporary = [];
  const derivedSecrets = [];
  try {
    cancelled(signal);
    account = privateAccount(privateInput);
    record(viewport, ["width", "height"], "browser viewport unavailable");
    const screenWidth = integer(viewport.width, 1, 65535, "invalid browser viewport");
    const screenHeight = integer(viewport.height, 1, 65535, "invalid browser viewport");
    const observedMachine = machine(machineInfo);
    const preferenceBytes = words(preferences, 35, 255, "browser preferences unavailable");
    if (preferenceBytes[0] !== 28) fail("unsupported browser preference version");
    if (![32, 34].includes(fontIndex)) fail("browser font index selection unavailable");
    integer(worldId, 1, 0x7fffffff, "browser world selection unavailable");
    if (typeof crypto?.getRandomValues !== "function") fail("browser cryptographic randomness unavailable");
    const suppliedUid = machineUid === undefined ? undefined : words(machineUid, 2, 0xffffffff, "invalid browser instance identifier");
    const indexCrcs = await fetchLoginCrcs({ js5Relay, fontIndex, signal, probe });
    cancelled(signal);
    const randomWords = (length) => {
      const bytes = new Uint32Array(length);
      temporary.push(bytes);
      try { crypto.getRandomValues(bytes); }
      catch { fail("browser cryptographic randomness unavailable"); }
      const words = Array.from(bytes);
      derivedSecrets.push(words);
      return words;
    };
    const uid = suppliedUid ?? randomWords(2);
    if (suppliedUid) derivedSecrets.push(suppliedUid);
    const clientRandomSeed = randomWords(2);
    const shared = {
      identity: account.identity,
      socialNonce: [0, 0],
      clientRandomSeed,
      random24: Array(24).fill(255),
      affiliateP: PUBLIC_LOGIN_DEFAULTS.affiliateP,
      preferences: preferenceBytes,
      loginToken: PUBLIC_LOGIN_DEFAULTS.loginToken,
      affiliate: PUBLIC_LOGIN_DEFAULTS.affiliate,
      sessionToken: PUBLIC_LOGIN_DEFAULTS.sessionToken,
      js5AuthToken: PUBLIC_LOGIN_DEFAULTS.js5AuthToken,
      indexCrcs,
    };
    const lobby = {
      ...shared,
      identity: { ...account.identity },
      seed: randomWords(4),
      password: account.password,
      gameId: PUBLIC_LOGIN_DEFAULTS.gameId,
      language: PUBLIC_LOGIN_DEFAULTS.language,
    };
    const world = {
      ...shared,
      identity: { ...account.identity },
      socialNonce: [...shared.socialNonce],
      clientRandomSeed: [...shared.clientRandomSeed],
      random24: [...shared.random24],
      preferences: [...shared.preferences],
      indexCrcs: [...shared.indexCrcs],
      seed: randomWords(4),
      reconnecting: false,
      displayMode: 2,
      screenWidth,
      screenHeight,
      antiAliasing: preferenceBytes[1],
      machineInfo: observedMachine,
      varVerifyId: 0,
      machineUid: uid,
      supplementaryLoginData: null,
      jagtheoraLoaded: false,
      isApplet: false,
      hasSignedTermsAgreement: false,
      loginType: PUBLIC_LOGIN_DEFAULTS.loginType,
      differentWorld: true,
      worldId,
    };
    derivedSecrets.push(world.clientRandomSeed);
    for (const config of [lobby, world]) {
      const encoded = new TextEncoder().encode(JSON.stringify(config));
      try {
        if (encoded.byteLength > MAX_CONFIG_BYTES) fail("browser login configuration byte limit");
      } finally { encoded.fill(0); }
    }
    cancelled(signal);
    complete = true;
    return { lobby, world };
  } catch (error) {
    if (error instanceof ConfigurationError) throw error;
    fail("invalid browser login configuration");
  } finally {
    for (const bytes of temporary) bytes.fill(0);
    if (!complete) for (const words of derivedSecrets) words.fill(0);
    if (account) { account.password = null; account.identity = null; }
  }
}
