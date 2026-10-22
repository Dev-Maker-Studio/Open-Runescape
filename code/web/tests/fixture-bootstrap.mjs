import { writeFile } from "node:fs/promises";

const lobby = {
  identity: { kind: "username", username: "offline_Œ_727" },
  password: "OFFLINE_ONLY_€",
  seed: [1, 4294967294, 0x12345678, 0x80000000],
  socialNonce: [0xfedcba98, 0x76543210],
  clientRandomSeed: [0x01234567, 0x89abcdef],
  gameId: 0,
  language: 2,
  random24: Array.from({ length: 24 }, (_, i) => (i * 7 + 3) & 255),
  affiliateP: "FAKE_AFFILIATE_™",
  preferences: Array.from({ length: 35 }, (_, i) => (i * 11 + 27) & 255),
  loginToken: "FAKE_LOGIN_€",
  affiliate: 0x89abcdef | 0,
  sessionToken: 0x76543210,
  js5AuthToken: "FAKE_JS5_ÿ",
  indexCrcs: Array.from({ length: 36 }, (_, i) => (i * 0x1234567 + 0x80000000) >>> 0),
};
const world = {
  ...lobby,
  reconnecting: false,
  displayMode: 2,
  screenWidth: 1280,
  screenHeight: 720,
  antiAliasing: 4,
  machineInfo: {
    operatingSystem: 3, x64Os: true, osVendor: 9, javaVendor: 4,
    javaBuild: 17, javaSubBuild: 2, javaUpdate: 3, unknownFlag: false,
    maxMemory: 4096, processors: 8, ram: 0xabcdef, cpuClock: 4200,
    gpuModel: "FAKE_GPU_€", gpuDriverVersion: "FAKE_DRIVER_™",
    directxVersion: "12.1", directxLetter: "A", dxDriverMonth: 10,
    dxDriverYear: 2026, cpuType: "FAKE_CPU", cpuData: "OFFLINE_CPU_Œ",
    cpuCores: 16, cpuFamilyId: 254,
    cpuFeatureFlags: [0x89abcdef, 0x12345678, 0x80000000], cpuModelInfo: 0xfedcba98,
  },
  varVerifyId: 0xfedcba98 | 0,
  machineUid: [0x88776655, 0x44332211],
  loginToken: "FAKE_CLIENT_LOGIN_€",
  supplementaryLoginData: "FAKE_EXTRA_ÿ",
  jagtheoraLoaded: true,
  isApplet: false,
  hasSignedTermsAgreement: true,
  loginType: 7,
  js5AuthToken: "FAKE_WORLD_JS5_™",
  differentWorld: true,
  worldId: 42,
};
delete world.password;
delete world.gameId;
delete world.language;

if (process.argv.length !== 3) throw new Error("one new fixture output path required");
await writeFile(process.argv[2], JSON.stringify({ lobby, world }), { flag: "wx", mode: 0o600 });
console.log(JSON.stringify({ fixtureOnly: true, privateBootstrapCreated: true }));
