import { assetProbe, loadRuntime } from "./js5-probe.js";
import { PUBLIC_LOGIN_DEFAULTS } from "./login-config.js";

const MATERIAL_INDEX = 26;
const GRAPH_INDEX = 9;
const SPRITE_INDEX = 8;
const MAX_ROUNDS = 32;
const MAX_MATERIALS = 64;

function hex(bytes) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export function bgraFromRgba(rgba) {
  if (!(rgba instanceof Uint8Array) || rgba.length % 4 !== 0) throw new Error("RGBA pixels required");
  const out = new Uint8Array(rgba.length);
  for (let i = 0; i < rgba.length; i += 4) {
    out[i] = rgba[i + 2];
    out[i + 1] = rgba[i + 1];
    out[i + 2] = rgba[i];
    out[i + 3] = rgba[i + 3];
  }
  return out;
}

export async function oracleDigest(rgba, subtle = globalThis.crypto.subtle) {
  return hex(new Uint8Array(await subtle.digest("SHA-256", bgraFromRgba(rgba))));
}

async function fetchFile(relay, token, index, archive, probe) {
  const result = await probe(relay, token, index, archive);
  const bytes = result.files.get(0);
  if (!bytes) throw new Error(`JS5 ${index}/${archive} file 0 unavailable`);
  return bytes;
}

export async function openTextureSet(relay, { token = PUBLIC_LOGIN_DEFAULTS.js5AuthToken, probe = assetProbe, load = async () => (await loadRuntime()).Js5TextureSet } = {}) {
  const TextureSet = await load();
  const set = new TextureSet();
  const table = await fetchFile(relay, token, MATERIAL_INDEX, 0, probe);
  const slots = set.setTable(table);
  const fetched = { graphs: 0, sprites: 0 };
  async function ensure(material) {
    for (let round = 0; round < MAX_ROUNDS; round += 1) {
      const needs = JSON.parse(set.needs(material));
      if (!needs.graphs.length && !needs.sprites.length) return;
      for (const id of needs.graphs) {
        set.addGraph(id, await fetchFile(relay, token, GRAPH_INDEX, id, probe));
        fetched.graphs += 1;
      }
      for (const id of needs.sprites) {
        set.addSprite(id, await fetchFile(relay, token, SPRITE_INDEX, id, probe));
        fetched.sprites += 1;
      }
    }
    throw new Error(`material ${material} inputs did not converge`);
  }
  return {
    slots,
    fetched,
    ensure,
    size: (material) => set.size(material),
    async render(material) {
      await ensure(material);
      return set.render(material);
    },
    async digest(material, subtle) {
      const rgba = await this.render(material);
      return { material, size: set.size(material), bytes: rgba.length, sha256: await oracleDigest(rgba, subtle) };
    },
    async digests(materials, subtle) {
      if (!Array.isArray(materials) || materials.length > MAX_MATERIALS) throw new Error("bounded material list required");
      const out = [];
      for (const material of materials) out.push(await this.digest(material, subtle));
      return out;
    },
    report: () => JSON.parse(set.report()),
    close: () => set.free(),
  };
}
