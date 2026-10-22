import { deriveBrowserMachineInfo, defaultLoginPreferences } from "./login-config.js";

export async function observeBrowserLoginOptions({
  navigator = globalThis.navigator,
  performance = globalThis.performance,
  viewport = { width: globalThis.innerWidth, height: globalThis.innerHeight },
  fontIndex,
  worldId,
  signal,
} = {}) {
  const cancelled = () => {
    if (signal?.aborted) throw new Error("browser profile cancelled");
  };
  cancelled();
  let platform = navigator?.platform ?? "";
  let architecture = "";
  let bitness = "";
  if (typeof navigator?.userAgentData?.getHighEntropyValues === "function") {
    let timer;
    let abort;
    try {
      const data = await Promise.race([
        navigator.userAgentData.getHighEntropyValues(["platform", "architecture", "bitness"]),
        new Promise((_, reject) => {
          timer = setTimeout(() => reject(new Error("browser profile unavailable")), 3000);
          abort = () => reject(new Error("browser profile cancelled"));
          signal?.addEventListener("abort", abort, { once: true });
        }),
      ]);
      platform = data.platform ?? platform;
      architecture = data.architecture ?? "";
      bitness = data.bitness ?? "";
    } catch {
      cancelled();
    } finally {
      clearTimeout(timer);
      signal?.removeEventListener("abort", abort);
    }
  }
  cancelled();
  if (typeof platform !== "string" || typeof architecture !== "string" || typeof bitness !== "string") throw new Error("browser architecture unavailable");
  let isArm;
  if (architecture === "arm" || /\b(aarch64|arm64|armv[5-9]\w*)\b/i.test(platform)) isArm = true;
  else if (architecture === "x86" || /\b(x86_64|amd64|i[3-6]86|win32|win64)\b/i.test(platform)) isArm = false;
  else throw new Error("browser architecture unavailable");
  const machineInfo = deriveBrowserMachineInfo({
    platform,
    architecture,
    bitness,
    hardwareConcurrency: navigator?.hardwareConcurrency,
    jsHeapSizeLimit: performance?.memory?.jsHeapSizeLimit,
  });
  return {
    viewport,
    machineInfo,
    preferences: defaultLoginPreferences({ maxMemory: machineInfo.maxMemory, processors: machineInfo.processors, isArm }),
    fontIndex,
    worldId,
  };
}
