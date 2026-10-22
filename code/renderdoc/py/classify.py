from collections import Counter

FAMILY = {
    (2, 2, 4, 1, 4): "sprite",
    (2, 2, 2): "surfaceMask",
    (3, 4, 3, 3): "groundTextured",
    (3, 3, 4): "ground",
    (3, 2, 4): "particle",
    (3, 3, 2, 4): "model",
    (3, 3, 2, 4, 4, 4, 4, 4, 4): "instanced",
}
OWNER = {
    "sprite": "sprite/UI/minimap",
    "surfaceMask": "sprite/UI/minimap",
    "groundTextured": "ground",
    "ground": "ground",
    "particle": "particles",
    "model": "model",
    "instanced": "model",
    "shadow": "shadow",
    "materialCubeSpec": "textures/materials",
}
LABEL = {"offscreen": "replay skips offscreen render targets"}


def family(draw, summary):
    if draw["topology"].startswith("PatchList"):
        return "hdWater"
    vs = summary["shaders"].get(draw.get("shaders", {}).get("vs"), {})
    return FAMILY.get(tuple(int(i.rsplit("x", 1)[1]) for i in vs.get("inputs", [])), "unknown")


def owner(fam):
    return "water" if fam.lower().startswith(("hdwater", "water")) else OWNER.get(fam, "frame wide")


def classify(c, w, only_c, only_w, frame):
    drawn, skips = frame if frame else (Counter(), Counter())
    budget = Counter(skips)
    cf, wf = Counter(family(d, c) for d in c["draws"]), Counter(family(d, w) for d in w["draws"])
    out = []
    left = {"client": [], "web": []}
    dumped = sum(drawn.values()) + sum(skips.values())
    partial = bool(frame) and dumped < len(c["draws"]) / 2
    for side, draws, summary in (("client", only_c, c), ("web", only_w, w)):
        for d in draws:
            fam = family(d, summary)
            row = {"side": side, "eid": d["eid"], "family": fam, "owner": owner(fam), "known": True}
            skip = next((k for k in budget if side == "client" and k.startswith(fam + "@") and budget[k] > 0), None)
            if partial:
                out.append({**row, "known": False, "reason": f"frame dump incomplete, {dumped} draws dumped for {len(c['draws'])} captured"})
                continue
            tess = side == "client" and d["topology"].startswith("PatchList")
            if skip:
                budget[skip] -= 1
                row["reason"] = ("tessellation replaced, " if tess else "") + LABEL.get(skip.split("@", 1)[1], "replay skips " + skip.split("@", 1)[1]) + f" ({skip})"
            elif tess:
                row["reason"] = "tessellation replaced"
            elif frame and drawn.get(fam, 0) and wf[fam] == drawn[fam] and cf[fam] != wf[fam]:
                row["reason"] = f"frame drift, capture has {cf[fam]} {fam} draws and the dump {drawn.get(fam, 0)}"
            elif frame and fam != "unknown" and wf[fam] == 0 and drawn.get(fam, 0) == 0 and side == "client":
                row["reason"] = "shader/pass not ported"
            else:
                row["known"] = False
                left[side].append(row)
            out.append(row)
    for fam in {r["family"] for r in left["client"]} & {r["family"] for r in left["web"]}:
        pairs = zip(*([r for r in left[side] if r["family"] == fam] for side in ("client", "web")))
        for x, y in pairs:
            x["reason"] = y["reason"] = f"same pass, different geometry (client eid {x['eid']}, web eid {y['eid']})"
    for r in left["client"] + left["web"]:
        r.setdefault("reason", "genuinely missing on the web" if r["side"] == "client" else "genuinely extra on the web")
    return out
