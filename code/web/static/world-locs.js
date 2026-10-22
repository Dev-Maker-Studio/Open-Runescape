import { worldLocProbe } from "./js5-probe.js";

const UNAVAILABLE = "original world locs unavailable";

export async function openWorldLocs({ js5Relay, lend, probe = worldLocProbe, signal } = {}) {
  let snapshot;
  try { snapshot = await probe(js5Relay, lend, { signal }); }
  catch { throw new Error(UNAVAILABLE); }
  let { report, regions, types, models, typeModels } = snapshot;
  let closed = false;
  const live = () => { if (closed) throw new Error(UNAVAILABLE); };
  return Object.freeze({
    state: () => { live(); return JSON.parse(JSON.stringify({ scope: "original-world-locs", authenticated: report.authenticated, regions: report.regions.length, placements: report.placements, typeIds: report.typeIds, typesFound: report.typesFound, typesDecoded: report.typesDecoded, typesUndecodable: report.typesUndecodable, modelIds: report.modelIds, modelsFound: report.modelsFound, regionReports: report.regions.map((r) => ({ regionId: r.regionId, present: r.present, keyed: r.keyed, decrypted: r.decrypted, expandedBytes: r.expandedBytes, placements: r.placements })) })); },
    locs: (regionId) => { live(); return regions.get(regionId); },
    locType: (id) => { live(); return types.get(id); },
    locModels: (typeId) => {
      live();
      const found = [];
      for (const id of typeModels.get(typeId) ?? []) {
        const data = models.get(id);
        if (data) found.push({ id, data });
      }
      return found;
    },
    locModel: (id) => { live(); return models.get(id); },
    close: () => {
      if (closed) return;
      closed = true;
      regions.clear();
      types.clear();
      models.clear();
      typeModels.clear();
      report = regions = types = models = typeModels = snapshot = null;
    },
  });
}
