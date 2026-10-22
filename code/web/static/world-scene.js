export const KIND_LOC = 0;
export const KIND_GROUND = 1;
export const KIND_PLAYER = 2;
export const KIND_NPC = 3;
export const KIND_HINT_ARROW = 6;
export const KIND_HINT_TRAIL = 7;
export const DEF_LOC = 0;
export const DEF_ITEM = 1;
export const DEF_NPC = 2;

const NO_HEIGHT = -2147483648;
const META_STRIDE = 6;
const NPC_STRIDE = 5;
const RETRIES = 1;

export function heightGrid(sample, tiles, planes) {
  const stride = tiles + 1;
  const grid = new Int32Array(planes * stride * stride);
  for (let plane = 0; plane < planes; plane += 1) {
    for (let x = 0; x < stride; x += 1) {
      for (let y = 0; y < stride; y += 1) {
        let value = null;
        try { value = sample(plane, x, y); } catch { value = null; }
        grid[(plane * stride + x) * stride + y] = Number.isFinite(value) ? Math.trunc(value) : NO_HEIGHT;
      }
    }
  }
  return grid;
}

export function partitionDraw(meta, data) {
  const main = { positions: [], colors: [], alphas: [], materials: [] };
  const overlay = { positions: [], colors: [], alphas: [], materials: [] };
  let triangle = 0;
  for (const mesh of meta) {
    const target = mesh.kind === KIND_HINT_ARROW || mesh.kind === KIND_HINT_TRAIL ? overlay : main;
    const end = triangle + mesh.triangles;
    target.positions.push(data.positions.subarray(triangle * 9, end * 9));
    target.colors.push(data.colors.subarray(triangle, end));
    target.alphas.push(data.alphas.subarray(triangle, end));
    target.materials.push(data.materials.subarray(triangle, end));
    triangle = end;
  }
  const join = (parts, ctor) => {
    const out = new ctor(parts.reduce((n, part) => n + part.length, 0));
    let at = 0;
    for (const part of parts) { out.set(part, at); at += part.length; }
    return out;
  };
  const build = (group) => ({
    positions: join(group.positions, Int32Array),
    colors: join(group.colors, Int16Array),
    alphas: join(group.alphas, Uint8Array),
    materials: join(group.materials, Int16Array),
  });
  return { main: build(main), overlay: build(overlay) };
}

export function parseMeta(flat) {
  const meshes = [];
  for (let at = 0; at + META_STRIDE <= flat.length; at += META_STRIDE) {
    meshes.push({ kind: flat[at], id: flat[at + 1], plane: flat[at + 2], x: flat[at + 3], y: flat[at + 4], triangles: flat[at + 5] });
  }
  return meshes;
}

export function parseOverlay(report, lines) {
  const overlay = JSON.parse(report);
  overlay.lines = JSON.parse(lines);
  return overlay;
}

export function parseNpcs(flat) {
  const npcs = new Map();
  for (let at = 0; at + NPC_STRIDE <= flat.length; at += NPC_STRIDE) {
    npcs.set(flat[at], { index: flat[at], typeId: flat[at + 1], x: flat[at + 2], y: flat[at + 3], plane: flat[at + 4] });
  }
  return npcs;
}

export function localTile(meta, ownIndex, base) {
  for (const mesh of meta) if (mesh.kind === KIND_PLAYER && mesh.id === ownIndex) return { x: mesh.x - base.x, y: mesh.y - base.y, plane: mesh.plane };
  return null;
}

export function sceneMaterials(materials) {
  const found = new Set();
  for (const material of materials) if (material >= 0) found.add(material);
  return found;
}

export function locHash(x, y, id) {
  return (x & 0x7f) + (y & 0x7f) * 0x80 + 2 * 0x20000000 + id * 0x4000;
}

export function hoverTargets({ tile, base, plane, meta, npcs, ownIndex, definition, entityHits = null }) {
  const onTile = (mesh) => (entityHits ? entityHits.has(`${mesh.kind}:${mesh.id}`) : mesh.x - base.x === tile.x && mesh.y - base.y === tile.y);
  const targets = [{ kind: "tile", x: tile.x, y: tile.y }];
  const pending = [];
  const lookup = (kind, id) => {
    const found = definition(kind, id);
    if (found === undefined) pending.push([kind, id]);
    return found ?? null;
  };
  const stack = [];
  for (const mesh of meta) {
    if (mesh.kind === KIND_PLAYER) {
      if (!onTile(mesh)) continue;
      targets.push({ kind: "player", index: mesh.id, name: "", differentPlane: mesh.plane !== plane });
    } else if (mesh.kind === KIND_NPC) {
      const npc = npcs.get(mesh.id);
      if (!npc || !onTile(mesh)) continue;
      const found = lookup(DEF_NPC, npc.typeId);
      if (!found) continue;
      targets.push({
        kind: "npc", index: npc.index, name: found.name ?? "", combatLevel: found.combatLevel ?? 0, visible: found.visible ?? true,
        gameType: found.gameType ?? 0, actions: found.actions ?? [], primaryCursorIndex: found.primaryCursorIndex ?? -1,
        primaryCursor: found.primaryCursor ?? -1, secondaryCursorIndex: found.secondaryCursorIndex ?? -1,
        secondaryCursor: found.secondaryCursor ?? -1, attackCursor: -1, differentPlane: mesh.plane !== plane, useOnAllowed: true,
      });
    } else if (mesh.kind === KIND_GROUND) {
      if (mesh.x - base.x !== tile.x || mesh.y - base.y !== tile.y) continue;
      const found = lookup(DEF_ITEM, mesh.id);
      if (!found) continue;
      stack.push({
        id: mesh.id, name: found.name ?? "", color: 0, actions: found.actions ?? [], primaryCursorIndex: found.primaryCursorIndex ?? -1,
        primaryCursor: found.primaryCursor ?? -1, secondaryCursorIndex: found.secondaryCursorIndex ?? -1,
        secondaryCursor: found.secondaryCursor ?? -1, useOnAllowed: true, samePlane: mesh.plane === plane,
      });
    } else if (mesh.kind === KIND_LOC) {
      if (mesh.x - base.x !== tile.x || mesh.y - base.y !== tile.y) continue;
      const found = lookup(DEF_LOC, mesh.id);
      if (!found) continue;
      targets.push({
        kind: "loc", uid: mesh.id, startX: tile.x, startZ: tile.y, hash: locHash(tile.x, tile.y, mesh.id), name: found.name ?? "",
        actions: found.actions ?? [], primaryCursorIndex: found.primaryCursorIndex ?? -1, primaryCursor: found.primaryCursor ?? -1,
        secondaryCursorIndex: found.secondaryCursorIndex ?? -1, secondaryCursor: found.secondaryCursor ?? -1,
        samePlane: mesh.plane === plane, useOnAllowed: true,
      });
    }
  }
  if (stack.length) {
    targets.push({ kind: "ground", startX: tile.x, startZ: tile.y, samePlane: stack.every((item) => item.samePlane), items: stack.map(({ samePlane, ...item }) => item) });
  }
  return { targets, pending };
}

export function menuState(ownIndex) {
  return { me: { index: ownIndex, combatLevel: 0, cbLevelRelated: 0, teamId: 0 }, reduceAttackPriority: false, defaultAttackCursor: -1, activeCursor: -1, uptext: "" };
}

export function createDefinitionCache(lend) {
  const ready = new Map();
  return {
    get(kind, id) {
      const key = `${kind}:${id}`;
      if (ready.has(key)) return ready.get(key);
      const text = lend((world) => world.sceneDefinition(kind, id));
      if (typeof text !== "string" || text === "") return undefined;
      const value = JSON.parse(text);
      ready.set(key, value);
      return value;
    },
    size: () => ready.size,
  };
}

export function createSceneService({ lend, fetchArchive, relay, token, parallel = 6, onProvided = () => {}, onStatus = () => {} }) {
  const stats = { rounds: 0, archives: 0, files: 0, absent: 0, failures: 0, retried: 0 };
  let busy = false;
  let closed = false;
  let lastError = null;

  const readNeeds = () => {
    const flat = lend((world) => Array.from(world.sceneNeeds()));
    const needs = [];
    for (let at = 0; at + 1 < flat.length; at += 2) needs.push([flat[at], flat[at + 1]]);
    return needs;
  };

  const load = async (index, archive) => {
    let failure;
    for (let attempt = 0; attempt <= RETRIES; attempt += 1) {
      try { return await fetchArchive(relay, token, index, archive); }
      catch (error) { failure = error; }
    }
    stats.failures += 1;
    lastError = String(failure?.message ?? failure);
    return null;
  };

  const provide = (index, archive, loaded) => {
    if (closed) return;
    const retried = lend((world) => {
      if (!loaded || loaded.files.size === 0) {
        stats.absent += 1;
        return JSON.parse(world.sceneAbsent(index, archive)).retried;
      }
      for (const [file, bytes] of loaded.files) {
        world.sceneAddFile(index, archive, file, bytes);
        stats.files += 1;
      }
      return JSON.parse(world.sceneFinishGroup(index, archive)).retried;
    });
    stats.archives += 1;
    stats.retried += retried;
  };

  const pump = async () => {
    if (busy || closed) return 0;
    busy = true;
    let provided = 0;
    try {
      for (let guard = 0; guard < 64 && !closed; guard += 1) {
        const needs = readNeeds();
        if (!needs.length) break;
        stats.rounds += 1;
        for (let at = 0; at < needs.length && !closed; at += parallel) {
          const slice = needs.slice(at, at + parallel);
          const results = await Promise.all(slice.map(([index, archive]) => load(index, archive)));
          slice.forEach(([index, archive], i) => provide(index, archive, results[i]));
          provided += slice.length;
        }
        onProvided(provided);
      }
    } catch (error) {
      lastError = String(error?.message ?? error);
      onStatus(`scene service: ${lastError}`);
    } finally { busy = false; }
    return provided;
  };

  return {
    pump,
    busy: () => busy,
    close: () => { closed = true; },
    stats: () => ({ ...stats, error: lastError }),
  };
}
