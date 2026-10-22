import argparse
import contextlib
import io
import json
import sys
import time
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np
from PIL import Image

import classify
import convention
import diff
import orf

FORMULA = 2

BARS = "▁▂▃▄▅▆▇█"


def draw_index(summary):
    return {d["eid"]: d for d in summary["draws"]}


def pass_name(draw, summary):
    if draw.get("marker"):
        return draw["marker"]
    vs = summary["shaders"].get(draw.get("shaders", {}).get("vs"), {})
    sig = ",".join(i.split(":", 1)[1].split(":")[0] for i in vs.get("inputs", []))
    return f"vs[{sig}] {draw['topology']}"


def regions(cdir, wdir, c, w, count):
    fc = next((t for t in c["targets"] if t["final"]), None)
    fw = next((t for t in w["targets"] if t["final"]), None)
    if not (fc and fw and fc.get("png") and fw.get("png")):
        return [], None
    a = Image.open(Path(cdir) / fc["png"]).convert("RGB")
    b = Image.open(Path(wdir) / fw["png"]).convert("RGB")
    if a.size != b.size:
        b = b.resize(a.size, Image.BILINEAR)
    d = np.abs(np.asarray(a, np.float64) - np.asarray(b, np.float64)).max(2)
    h, wd = d.shape
    tile = max(16, -(-max(h, wd) // 16))
    out = []
    for y in range(0, h, tile):
        for x in range(0, wd, tile):
            t = d[y : y + tile, x : x + tile]
            off = float((t > 8).mean())
            if off > 0:
                out.append({"box": [x, y, min(x + tile, wd) - 1, min(y + tile, h) - 1], "mean_diff": round(float(t.mean()), 2), "differing_pct": round(100 * off, 1)})
    out.sort(key=lambda r: (-r["mean_diff"], -r["differing_pct"]))
    return out[:count], {"client": list(a.size), "web": [fw["size"][0], fw["size"][1]]}


def texture_sets(c, w):
    def hashes(s):
        return {t["hash"] for d in s["draws"] for t in d.get("textures", []) if not convention.placeholder(t)}

    hc, hw = hashes(c), hashes(w)
    return {"client_unique": len(hc), "web_unique": len(hw), "shared": len(hc & hw), "client_only": sorted(hc - hw)[:8], "web_only": sorted(hw - hc)[:8]}


def previous(history, scene):
    rows = [r for r in history if r["scene"] == scene]
    return rows[-1] if rows else None


def load_history(path):
    p = Path(path)
    return [json.loads(l) for l in p.read_text().splitlines() if l.strip()] if p.exists() else []


def grouped(flat, kind, key, top):
    g = defaultdict(lambda: {"draws": 0, "first": None, "example": ""})
    for i, k, m in flat:
        if k == kind:
            e = g[key(m)]
            e["draws"] += 1
            e["first"] = i if e["first"] is None else e["first"]
            e["example"] = e["example"] or m[:200]
    return [{"what": k, **v} for k, v in sorted(g.items(), key=lambda kv: -kv[1]["draws"])[: top * 2]]


def report(a):
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        diff.run(a.client, a.web, out / "diff", 1000)
    rep = json.loads((out / "diff" / "diff.json").read_text())
    c, w = diff.load(a.client), diff.load(a.web)
    ci, wi = draw_index(c), draw_index(w)
    bad = [p for p in rep["pairs"] if p["diffs"]]
    groups = defaultdict(lambda: {"draws": 0, "kinds": Counter(), "examples": []})
    for p in bad:
        d = ci[p["client_eid"]]
        fam = classify.family(d, c)
        g = groups[pass_name(d, c) if fam == "unknown" else f"{fam} ({classify.owner(fam)})"]
        g["draws"] += 1
        for k, m in p["diffs"]:
            g["kinds"][k] += 1
            if len(g["examples"]) < a.examples:
                g["examples"].append(f"draw {p['index']} (client eid {p['client_eid']}, web eid {p['web_eid']}) {k}: {m[:260]}")
    frame_path = Path(a.orf) if a.orf else next((q for q in (out / "frame.orf", Path(a.web) / "frame.orf") if q.exists()), None)
    rows = classify.classify(c, w, [ci[e] for e in rep["only_client"]], [wi[e] for e in rep["only_web"]], orf.counts(frame_path) if frame_path else None)
    real = [r for r in rows if not r["known"]]
    unmatched = len(real)
    known = Counter(r["side"] for r in rows if r["known"])
    only = defaultdict(lambda: {"eids": [], "known": False})
    for r in rows:
        o = only[(r["side"], r["family"], r["owner"], r["reason"].split(" (")[0])]
        o["eids"].append(r["eid"])
        o["known"] = r["known"]
    conventions = Counter(k for p in rep["pairs"] for k, _ in p["notes"])
    converted = sum(any(convention.is_clip_matrix(k, v) for k, v in d.get("uniforms", {}).items()) for d in c["draws"]) if c["meta"]["api"] == "OpenGL" else 0
    final = next((t for t in rep["targets"] if t["final"] and "pixels" in t), None)
    px = final["pixels"] if final else None
    ssim = max(0.0, px["ssim"]) if px else 0.0
    matched = rep["matched"]
    comparable = max(len(c["draws"]) - known["client"], len(w["draws"]) - known["web"], 1)
    score = round(100 * (0.5 * ssim + 0.25 * min(matched / comparable, 1) + 0.25 * (1 - len(bad) / max(matched, 1))), 1)
    checks = [
        {"name": "final ssim", "need": f">= {a.min_ssim}", "got": px["ssim"] if px else None, "pass": bool(px) and px["ssim"] >= a.min_ssim},
        {"name": "unmatched draws", "need": "== 0", "got": unmatched, "pass": unmatched == 0},
        {"name": "mismatched draws", "need": f"<= {a.max_mismatch}", "got": len(bad), "pass": len(bad) <= a.max_mismatch},
    ]
    verdict = "PASS" if all(k["pass"] for k in checks) else "FAIL"
    top, sizes = regions(a.client, a.web, c, w, a.top)
    hist = load_history(a.history)
    prev = previous(hist, a.scene)
    flat = [(p["index"], k, m) for p in bad for k, m in p["diffs"]]
    res = {
        "scene": a.scene,
        "mode": a.mode,
        "verdict": verdict,
        "score": score,
        "previous_score": prev["score"] if prev else None,
        "previous_formula": prev.get("formula", 1) if prev else None,
        "formula": FORMULA,
        "checks": checks,
        "pixels": px,
        "sizes": sizes,
        "draws": {"client": len(c["draws"]), "web": len(w["draws"]), "matched": matched, "mismatched": len(bad), "only_client": rep["only_client"][:20], "only_web": rep["only_web"][:20], "reordered": sum(p["reordered"] for p in rep["pairs"]), "only_client_total": len(rep["only_client"]), "only_web_total": len(rep["only_web"]), "real_unmatched": unmatched, "known_unmatched": sum(known.values()),
                  "unmatched": [{"side": k[0], "family": k[1], "owner": k[2], "reason": k[3], "known": v["known"], "count": len(v["eids"]), "eids": v["eids"][:8]} for k, v in sorted(only.items(), key=lambda kv: (kv[1]["known"], -len(kv[1]["eids"])))]},
        "conventions": {"clip matrix draws": converted, **conventions},
        "groups": {k: {"draws": g["draws"], "kinds": dict(g["kinds"]), "examples": g["examples"]} for k, g in sorted(groups.items(), key=lambda kv: -kv[1]["draws"])},
        "uniform_diffs": grouped(flat, "uniform", lambda m: m.split(" | ")[-1] if " | " in m else m[:80], a.top),
        "texture_diffs": grouped(flat, "texture", lambda m: m.split("=")[0] + (" flipped" if "flipped" in m else ""), a.top),
        "texture_sets": texture_sets(c, w),
        "regions": top,
        "files": {"client": str(a.client), "web": str(a.web), "diff_report": str(out / "diff" / "report.txt"), "diff_json": str(out / "diff" / "diff.json"), "diff_png": px["diff_png"] if px else None},
        "timings": json.loads(a.timings) if a.timings else {},
        "time": int(time.time()),
    }
    (out / "equiv.json").write_text(json.dumps(res, indent=1))
    text = render(res)
    (out / "report.txt").write_text(text + "\n")
    if not a.no_history:
        Path(a.history).parent.mkdir(parents=True, exist_ok=True)
        row = {"time": res["time"], "scene": a.scene, "mode": a.mode, "verdict": verdict, "score": score, "ssim": px["ssim"] if px else None, "psnr": px["psnr_db"] if px else None, "mismatched": len(bad), "unmatched": unmatched, "known": len(rows) - unmatched, "formula": FORMULA, "draws": [len(c["draws"]), len(w["draws"])]}
        with open(a.history, "a") as f:
            f.write(json.dumps(row) + "\n")
    print(text)
    return 0 if verdict == "PASS" else 1


def render(r):
    px, d = r["pixels"], r["draws"]
    prev = r["previous_score"]
    trend = "" if prev is None else f" (previous {prev}, {r['score'] - prev:+.1f}{', score formula changed' if r['formula'] != r['previous_formula'] else ''})"
    L = [f"EQUIV scene={r['scene']} mode={r['mode']} verdict={r['verdict']} score={r['score']}/100{trend}"]
    L.append("thresholds: " + " | ".join(f"{k['name']} {k['need']} got {k['got']} {'ok' if k['pass'] else 'FAIL'}" for k in r["checks"]))
    if px:
        L.append(f"pixels: psnr={px['psnr_db']}dB ssim={px['ssim']} mse={px['mse']} max={px['max_diff']} differing={px['differing_pixels_pct']}% bbox={px['bbox']}" + (f" note={px['note']}" if px["note"] else ""))
        if r["sizes"]:
            L.append(f"sizes: client={r['sizes']['client']} web={r['sizes']['web']}")
    else:
        L.append("pixels: no paired final target")
    L.append(f"draws: client={d['client']} web={d['web']} matched={d['matched']} (reordered {d['reordered']}) mismatched={d['mismatched']} unmatched real={d['real_unmatched']} known={d['known_unmatched']}")
    L.append("conventions normalized: " + ", ".join(f"{k}={v}" for k, v in r["conventions"].items()))
    if d["unmatched"]:
        L.append("unmatched draws by pass (owner), reason:")
        for u in d["unmatched"]:
            L.append(f"  {'known' if u['known'] else 'REAL '} {u['side']} {u['family']} ({u['owner']}) x{u['count']} eids {u['eids']}: {u['reason']}")
    if r["groups"]:
        L.append("mismatches by shader/pass:")
        for k, g in r["groups"].items():
            L.append(f"  {k}: draws={g['draws']} " + " ".join(f"{a}={b}" for a, b in sorted(g["kinds"].items())))
            L += [f"    {e}" for e in g["examples"]]
    if r["texture_diffs"]:
        L.append("texture content differences by hash (draws, first draw, example):")
        L += [f"  {t['what']}: draws={t['draws']} first={t['first']} {t['example']}" for t in r["texture_diffs"]]
    if r["uniform_diffs"]:
        L.append("uniform differences by client uniform (draws, first draw, example):")
        L += [f"  {u['what']}: draws={u['draws']} first={u['first']} {u['example']}" for u in r["uniform_diffs"]]
    t = r["texture_sets"]
    L.append(f"textures: client unique={t['client_unique']} web unique={t['web_unique']} shared={t['shared']} client only={t['client_only']} web only={t['web_only']}")
    if r["regions"]:
        L.append("worst regions (x0,y0,x1,y1 in client pixels):")
        L += [f"  {g['box']} mean_diff={g['mean_diff']} differing={g['differing_pct']}%" for g in r["regions"]]
    f = r["files"]
    L.append(f"files: diff_png={f['diff_png']} report={f['diff_report']} json={f['diff_json']} client={f['client']} web={f['web']}")
    if r["timings"]:
        L.append("timings: " + " ".join(f"{k}={v}s" for k, v in r["timings"].items()))
    return "\n".join(L)


def history(a):
    rows = [r for r in load_history(a.history) if not a.scene or r["scene"] == a.scene][-a.count :]
    if not rows:
        print("no equiv history yet")
        return 0
    print(f"{'time':16} {'scene':12} {'mode':8} {'verdict':7} {'score':>6} {'delta':>6} {'ssim':>7} {'psnr':>6} {'bad':>4} {'unm':>4}")
    last = {}
    for r in rows:
        p = last.get(r["scene"])
        dl = "" if p is None else f"{r['score'] - p:+.1f}"
        last[r["scene"]] = r["score"]
        print(f"{time.strftime('%m-%d %H:%M:%S', time.localtime(r['time'])):16} {r['scene']:12} {r['mode']:8} {r['verdict']:7} {r['score']:6} {dl:>6} {str(r['ssim']):>7} {str(r['psnr']):>6} {r['mismatched']:4} {r['unmatched']:4}")
    for s in sorted({r["scene"] for r in rows}):
        v = [r["score"] for r in rows if r["scene"] == s]
        lo, hi = min(v), max(v)
        print(f"{s} trend {''.join(BARS[0 if hi == lo else int((x - lo) / (hi - lo) * 7)] for x in v)} first={v[0]} last={v[-1]} best={hi}")
    return 0


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    r = sub.add_parser("report")
    r.add_argument("client")
    r.add_argument("web")
    r.add_argument("out")
    r.add_argument("--scene", default="adhoc")
    r.add_argument("--mode", default="builtin")
    r.add_argument("--top", type=int, default=5)
    r.add_argument("--examples", type=int, default=2)
    r.add_argument("--min-ssim", type=float, default=0.99)
    r.add_argument("--max-mismatch", type=int, default=0)
    r.add_argument("--timings", default="")
    r.add_argument("--orf", default="")
    r.add_argument("--history", required=True)
    r.add_argument("--no-history", action="store_true")
    h = sub.add_parser("history")
    h.add_argument("--history", required=True)
    h.add_argument("--scene", default="")
    h.add_argument("--count", type=int, default=20)
    a = p.parse_args()
    return report(a) if a.cmd == "report" else history(a)


if __name__ == "__main__":
    sys.exit(main())
