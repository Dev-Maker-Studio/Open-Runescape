import json
import re
import sys
from bisect import bisect_left
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path

import numpy as np
from PIL import Image

import convention

TOL = 1e-3
TIME = re.compile(r"(?i)time|pointlightcolor")
IGNORED = {"drift", "web extra"}
GROUP = "openrs"


def tag(recs):
    inside = [GROUP in r["marker"].split("/") for r in recs]
    if any(inside):
        for r, i in zip(recs, inside):
            r["origin"] = "renderer" if i else "browser"
    return recs


def renderer_only(s):
    if not any(x.get("origin") for x in s["draws"]):
        return s
    for k in ("draws", "clears"):
        s[k] = [x for x in s[k] if x.get("origin") != "browser"]
        for x in s[k]:
            x["marker"] = "/".join(p for p in x["marker"].split("/") if p != GROUP)
    outs = {o for x in s["draws"] for o in x["outputs"]}
    s["targets"] = [t for t in s["targets"] if t["id"] in outs]
    return s


def load(d):
    return convention.to_web(renderer_only(json.loads((Path(d) / "summary.json").read_text())))


def key(d):
    return (d["topology"], d["count"], d["instances"])


def align(a, b):
    sm = SequenceMatcher(None, [key(x) for x in a], [key(x) for x in b], autojunk=False)
    pairs, only_a, only_b = [], [], []
    for op, i1, i2, j1, j2 in sm.get_opcodes():
        if op == "equal":
            pairs += [(a[i1 + k], b[j1 + k]) for k in range(i2 - i1)]
        else:
            n = min(i2 - i1, j2 - j1)
            pairs += [(a[i1 + k], b[j1 + k]) for k in range(n)]
            only_a += a[i1 + n : i2]
            only_b += b[j1 + n : j2]
    moved = set()
    for x in list(only_a):
        y = next((y for y in only_b if key(y) == key(x)), None)
        if y:
            only_a.remove(x)
            only_b.remove(y)
            pairs.append((x, y))
            moved.add(x["eid"])
    return pairs, only_a, only_b, moved


def close(x, y):
    return len(x) == len(y) and all(abs(p - q) <= TOL * max(1.0, abs(p), abs(q)) for p, q in zip(x, y))


def split(u):
    named, anon = {}, []
    for k, v in u.items():
        n = k.split(".")[-1]
        (anon.append(tuple(v)) if n.startswith("_child") else named.setdefault(n, v))
    return named, anon


def scalars(u):
    return sorted({x for v in u.values() for x in v if abs(x) > TOL})


def missing(a, b):
    out = []
    for x in a:
        i = bisect_left(b, x)
        if not any(0 <= j < len(b) and convention.same(x, b[j]) for j in (i - 1, i)):
            out.append(x)
    return out


def names_of(u, values):
    hit = {k.split(".")[-1].split("[")[0] for k, v in u.items() if any(not missing([x], values) for x in v if abs(x) > TOL)}
    return ",".join(sorted(hit))


def time_moved(cu, wu):
    if not any(k.endswith("._words") for k in wu):
        return False
    return any(TIME.search(n) and "light" not in n.lower() for n in names_of(cu, missing(scalars(cu), scalars(wu))).split(","))


def drifted(cu, x, y, moved=False):
    owners = {}
    for k, vals in cu.items():
        for v in vals:
            owners.setdefault(v, set()).add(k.split(".")[-1].split("[")[0])
    if not (moved or any(n for v in x for n in owners.get(v, ()) if TIME.search(n) and "light" not in n.lower())):
        return None
    explained = [v for v in x if owners.get(v) and all(TIME.search(n) for n in owners[v])]
    rest = [v for v in x if v not in explained]
    return (rest, y) if rest or len(y) > len(explained) + 2 else ([], [])


def uniform_diffs(cu, wu, moved=False):
    if any(k.endswith("._words") for k in (*cu, *wu)):
        a, b = scalars(cu), scalars(wu)
        x, y = missing(a, b), missing(b, a)
        if not (x or y):
            return []
        if not x:
            return [("web extra", f"web block holds {len(y)} values with no client counterpart {y[:8]}")]
        d = drifted(cu, x, y, moved)
        if d == ([], []):
            return [("drift", f"time driven uniforms differ between the two frames, client values {x[:8]} | client uniforms {names_of(cu, x)}")]
        x, y = d if d else (x, y)
        return [("uniform", f"values only client={x[:8]} web={y[:8]} | client uniforms {names_of(cu, x)}")]
    (cn, ca), (wn, wa) = split(cu), split(wu)
    out = []
    for k in sorted(set(cn) & set(wn)):
        if not close(cn[k], wn[k]):
            out.append(("uniform", f"{k} client={cn[k]} web={wn[k]}"))
    ca += [tuple(v) for k, v in cn.items() if k not in wn]
    wa += [tuple(v) for k, v in wn.items() if k not in cn]
    rest = list(wa)
    left = []
    for v in ca:
        m = next((i for i, x in enumerate(rest) if close(v, x)), None)
        left.append(v) if m is None else rest.pop(m)
    if left or rest:
        out.append(("uniform", f"unmatched values client={[list(v) for v in left]} web={[list(v) for v in rest]}"))
    return out


def blend(s):
    return [f"{b['src']}*{b['dst']} {b['op']} / {b['asrc']}*{b['adst']} {b['aop']} mask {b['mask']}" for b in s.get("blend", [])]


def draw_diffs(c, w, moved=False):
    out, notes = [], []
    cs, ws = c.get("state", {}), w.get("state", {})
    for f in ("cull", "front_ccw", "depth", "stencil", "scissor"):
        if cs.get(f) != ws.get(f):
            out.append(("state", f"{f} client={cs.get(f)} web={ws.get(f)}"))
    if blend(cs) != blend(ws):
        out.append(("state", f"blend client={blend(cs)} web={blend(ws)}"))
    if cs.get("viewport") != ws.get("viewport"):
        out.append(("state", f"viewport client={cs.get('viewport')} web={ws.get('viewport')}"))
    if c.get("index", {}) and w.get("index"):
        if c["index"]["hash"] != w["index"]["hash"]:
            out.append(("index", f"hash client={c['index']['hash']} web={w['index']['hash']} max client={c['index']['max']} web={w['index']['max']}"))
    elif bool(c.get("index")) != bool(w.get("index")):
        out.append(("index", f"client={'indexed' if c.get('index') else 'none'} web={'indexed' if w.get('index') else 'none'}"))
    cv, wv = {a["loc"]: a for a in c.get("vertex", [])}, {a["loc"]: a for a in w.get("vertex", [])}
    for loc in sorted(set(cv) | set(wv)):
        a, b = cv.get(loc), wv.get(loc)
        if not a or not b:
            out.append(("vertex", f"loc {loc} only on {'client' if a else 'web'}"))
        elif a.get("hash") != b.get("hash"):
            out.append(("vertex", f"loc {loc} {a['name']}/{b['name']} hash client={a.get('hash')} web={b.get('hash')} head client={a.get('head')} web={b.get('head')}"))
    for d in uniform_diffs(c.get("uniforms", {}), w.get("uniforms", {}), moved):
        (notes if d[0] in IGNORED else out).append(d)
    found, extra = convention.textures(c.get("textures", []), w.get("textures", []))
    out += [("texture", m) for m in found]
    notes += [("client extra textures", m) for m in extra]
    return out, notes


def box(x, k):
    p = np.pad(x, ((1, 0), (1, 0))).cumsum(0).cumsum(1)
    return (p[k:, k:] - p[:-k, k:] - p[k:, :-k] + p[:-k, :-k]) / (k * k)


def ssim(a, b, k=7):
    a, b = a.mean(2), b.mean(2)
    if min(a.shape) < k:
        return 1.0 if np.array_equal(a, b) else 0.0
    ma, mb = box(a, k), box(b, k)
    va, vb, cov = box(a * a, k) - ma * ma, box(b * b, k) - mb * mb, box(a * b, k) - ma * mb
    c1, c2 = (0.01 * 255) ** 2, (0.03 * 255) ** 2
    return float((((2 * ma * mb + c1) * (2 * cov + c2)) / ((ma * ma + mb * mb + c1) * (va + vb + c2))).mean())


def pixels(c_png, w_png, out_png):
    a = Image.open(c_png).convert("RGB")
    b = Image.open(w_png).convert("RGB")
    note = None
    if a.size != b.size:
        note = f"size client={a.size} web={b.size}, web resized"
        b = b.resize(a.size, Image.BILINEAR)
    x, y = np.asarray(a, np.float64), np.asarray(b, np.float64)
    d = np.abs(x - y)
    mse = float((d**2).mean())
    m = d.max(2) > 8
    ys, xs = np.nonzero(m)
    Image.fromarray(np.clip(d * 4, 0, 255).astype(np.uint8)).save(out_png)
    return {
        "psnr_db": None if mse == 0 else round(10 * np.log10(255**2 / mse), 2),
        "ssim": round(ssim(x, y), 4),
        "mse": round(mse, 3),
        "max_diff": int(d.max()),
        "differing_pixels_pct": round(100 * float(m.mean()), 3),
        "bbox": [int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())] if len(xs) else None,
        "note": note,
        "diff_png": str(out_png),
    }


def pair_targets(c, w):
    ct, wt = c["targets"], w["targets"]
    fc, fw = next((t for t in ct if t["final"]), None), next((t for t in wt if t["final"]), None)
    pairs = [(fc, fw)] if fc and fw else []
    rc, rw = [t for t in ct if t is not fc], [t for t in wt if t is not fw]
    for t in rc:
        o = next((u for u in rw if u["size"] == t["size"]), None)
        pairs.append((t, o))
        rw = [u for u in rw if u is not o]
    return pairs + [(None, u) for u in rw]


def verdict(report):
    final = next((t for t in report["targets"] if t.get("final")), None)
    px = final["pixels"] if final and "pixels" in final else None
    issues = sum(len(p["diffs"]) for p in report["pairs"]) + len(report["only_client"]) + len(report["only_web"])
    if px and px["ssim"] >= 0.995 and issues == 0:
        return "EQUIVALENT"
    if px and px["ssim"] >= 0.95 and len(report["only_client"]) + len(report["only_web"]) == 0:
        return "CLOSE"
    return "DIVERGENT"


def run(cd, wd, out, limit):
    out.mkdir(parents=True, exist_ok=True)
    c, w = load(cd), load(wd)
    pairs, oc, ow, reordered = align(c["draws"], w["draws"])
    rep = {"client": str(cd), "web": str(wd), "client_api": c["meta"]["api"], "web_api": w["meta"]["api"], "draws": [len(c["draws"]), len(w["draws"])], "matched": len(pairs), "only_client": [d["eid"] for d in oc], "only_web": [d["eid"] for d in ow], "pairs": [], "targets": []}
    moved = any(time_moved(a["uniforms"], b["uniforms"]) for a, b in pairs[:: max(1, len(pairs) // 40)])
    for i, (a, b) in enumerate(pairs):
        diffs, notes = draw_diffs(a, b, moved)
        rep["pairs"].append({"index": i, "client_eid": a["eid"], "web_eid": b["eid"], "key": list(key(a)), "reordered": a["eid"] in reordered, "diffs": diffs, "notes": notes})
    for a, b in pair_targets(c, w):
        t = {"client": a and a["id"], "web": b and b["id"], "final": bool((a and a["final"]) or (b and b["final"]))}
        if a and b and a["png"] and b["png"]:
            t["pixels"] = pixels(Path(cd) / a["png"], Path(wd) / b["png"], out / f"diff_{a['id']}_{b['id']}.png")
        rep["targets"].append(t)
    rep["verdict"] = verdict(rep)
    (out / "diff.json").write_text(json.dumps(rep, indent=1))
    lines = [f"verdict: {rep['verdict']}", f"client {rep['client_api']} draws={len(c['draws'])} clears={len(c['clears'])} targets={len(c['targets'])} | web {rep['web_api']} draws={len(w['draws'])} clears={len(w['clears'])} targets={len(w['targets'])}", f"aligned draws: {len(pairs)} ({len(reordered)} reordered), only client: {len(oc)}, only web: {len(ow)}"]
    kinds = Counter(k for p in rep["pairs"] for k, _ in p["diffs"])
    lines.append("mismatches by kind: " + (", ".join(f"{k}={n}" for k, n in sorted(kinds.items())) or "none"))
    conv = Counter(k for p in rep["pairs"] for k, _ in p["notes"])
    lines.append("conventions ignored: " + (", ".join(f"{k}={n}" for k, n in sorted(conv.items())) or "none"))
    shown = 0
    for p in rep["pairs"]:
        for k, msg in p["diffs"]:
            if shown < limit:
                lines.append(f"  draw {p['index']} (client eid {p['client_eid']}, web eid {p['web_eid']}) {k}: {msg}")
            shown += 1
    if shown > limit:
        lines.append(f"  ... {shown - limit} more in diff.json")
    for d in oc[:limit]:
        lines.append(f"  only client eid {d['eid']}: {d['topology']} count={d['count']} instances={d['instances']} program={d.get('program')}")
    for d in ow[:limit]:
        lines.append(f"  only web eid {d['eid']}: {d['topology']} count={d['count']} instances={d['instances']} program={d.get('program')}")
    for t in rep["targets"]:
        px = t.get("pixels")
        tag = "final" if t["final"] else "target"
        lines.append(f"{tag} client={t['client']} web={t['web']}" + (f" psnr={px['psnr_db']}dB ssim={px['ssim']} mse={px['mse']} max={px['max_diff']} differing={px['differing_pixels_pct']}% bbox={px['bbox']}{' ' + px['note'] if px['note'] else ''} diff={px['diff_png']}" if px else " unpaired"))
    text = "\n".join(lines)
    (out / "report.txt").write_text(text + "\n")
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(run(sys.argv[1], sys.argv[2], Path(sys.argv[3]), int(sys.argv[4]) if len(sys.argv) > 4 else 40))
