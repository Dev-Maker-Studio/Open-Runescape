import argparse
import hashlib
import json
import sys
import time
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diff

PASS_SSIM = 0.99


def digest(path):
    p = Path(path)
    if not p.is_file():
        return {"file": str(p), "bytes": None, "sha256": None}
    data = p.read_bytes()
    return {"file": str(p), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}


def first_difference(a, b):
    x, y = Path(a).read_bytes(), Path(b).read_bytes()
    for i, (p, q) in enumerate(zip(x, y)):
        if p != q:
            return i
    return None if len(x) == len(y) else min(len(x), len(y))


def serialized(client, web):
    c, w = digest(client), digest(web)
    same = c["sha256"] is not None and c["sha256"] == w["sha256"]
    at = None if same or c["sha256"] is None or w["sha256"] is None else first_difference(client, web)
    return {"client": c, "web": w, "identical": same, "first_difference": at}


def regions(a, b, count):
    d = np.abs(np.asarray(a, np.float64) - np.asarray(b, np.float64)).max(2)
    h, w = d.shape
    tile = max(16, -(-max(h, w) // 16))
    out = []
    for y in range(0, h, tile):
        for x in range(0, w, tile):
            t = d[y : y + tile, x : x + tile]
            off = float((t > 8).mean())
            if off > 0:
                out.append({"box": [x, y, min(x + tile, w) - 1, min(y + tile, h) - 1], "mean_diff": round(float(t.mean()), 2), "differing_pct": round(100 * off, 1)})
    out.sort(key=lambda r: (-r["mean_diff"], -r["differing_pct"]))
    return out[:count]


def font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


def compose(client_png, web_png, diff_png, out_png, title, lines):
    a = Image.open(client_png).convert("RGB")
    b = Image.open(web_png).convert("RGB")
    if b.size != a.size:
        b = b.resize(a.size, Image.BILINEAR)
    d = Image.open(diff_png).convert("RGB")
    w, h = a.size
    gap, head, foot = 8, 26, 18 * len(lines) + 10
    sheet = Image.new("RGB", (3 * w + 4 * gap, head + h + foot + 2 * gap), (24, 24, 28))
    draw = ImageDraw.Draw(sheet)
    f = font(14)
    for i, (img, label) in enumerate(((a, "OpenGL client"), (b, "WebGPU browser"), (d, "difference x4"))):
        x = gap + i * (w + gap)
        draw.text((x, 6), label, fill=(230, 230, 230), font=f)
        sheet.paste(img, (x, head))
    draw.text((gap, head + h + gap), title, fill=(255, 220, 120), font=f)
    for i, line in enumerate(lines):
        draw.text((gap, head + h + gap + 18 * (i + 1)), line, fill=(200, 200, 200), font=f)
    sheet.save(out_png)
    return sheet.size


def read_json(path):
    p = Path(path)
    if not p.is_file():
        return None
    try:
        return json.loads(p.read_text())
    except ValueError:
        return {"text": p.read_text()[:2000]}


def read_text(path):
    p = Path(path)
    return p.read_text().strip() if p.is_file() else None


def run(out, scene, renderdoc):
    out = Path(out)
    client, web = out / "client", out / "web"
    cpng, wpng = client / "shot.png", web / "shot.png"
    px = diff.pixels(cpng, wpng, out / "diff.png")
    a, b = Image.open(cpng).convert("RGB"), Image.open(wpng).convert("RGB")
    if b.size != a.size:
        b = b.resize(a.size, Image.BILINEAR)
    orf = serialized(client / "frame.orf", web / "frame.orf")
    states = {"client": read_json(client / "state.json"), "web": read_json(web / "state.json")}
    desc = {"client": read_text(client / "describe.txt"), "web": read_text(web / "describe.txt")}
    rd = read_json(renderdoc) if renderdoc else None
    verdict = "PASS" if px["ssim"] >= PASS_SSIM and orf["identical"] else "FAIL"
    report = {
        "scene": scene,
        "verdict": verdict,
        "thresholds": {"ssim": PASS_SSIM, "serialized_frames_identical": True},
        "pixels": px,
        "regions": regions(a, b, 5),
        "serialized": orf,
        "state": states,
        "describe": desc,
        "renderdoc": None if rd is None else {k: rd.get(k) for k in ("verdict", "score", "previous_score", "checks", "draws")},
        "side_by_side": str(out / "twin.png"),
    }
    lines = [
        f"verdict {verdict}   ssim {px['ssim']}   psnr {px['psnr_db']} dB   differing {px['differing_pixels_pct']}%   serialized frames identical {orf['identical']}",
        f"client {states['client'].get('gameState') if states['client'] else '?'} tick {states['client'].get('tick') if states['client'] else '?'}   web {states['web'].get('backend') if states['web'] else '?'} frame {states['web'].get('frames') if states['web'] else '?'}",
    ]
    if rd:
        lines.append(f"renderdoc equiv {rd.get('verdict')} score {rd.get('score')}")
    compose(cpng, wpng, out / "diff.png", out / "twin.png", f"twin {scene}", lines)
    (out / "twin.json").write_text(json.dumps(report, indent=1))
    return report


def text(r):
    p, s = r["pixels"], r["serialized"]
    L = [
        f"TWIN scene={r['scene']} verdict={r['verdict']}",
        f"pixels: ssim={p['ssim']} psnr={p['psnr_db']}dB mse={p['mse']} max={p['max_diff']} differing={p['differing_pixels_pct']}% bbox={p['bbox']}",
        f"serialized frames: client {s['client']['bytes']} bytes, web {s['web']['bytes']} bytes, identical={s['identical']}" + ("" if s["identical"] or s["first_difference"] is None else f", first difference at byte {s['first_difference']}"),
    ]
    for side in ("client", "web"):
        if r["describe"][side]:
            L.append(f"{side}: " + r["describe"][side].splitlines()[0][:200])
    if r["renderdoc"]:
        rd = r["renderdoc"]
        L.append(f"renderdoc equiv: verdict={rd['verdict']} score={rd['score']} previous={rd['previous_score']}")
    if r["regions"]:
        L.append("worst regions (x0,y0,x1,y1):")
        L += [f"  {g['box']} mean_diff={g['mean_diff']} differing={g['differing_pct']}%" for g in r["regions"]]
    L.append(f"side by side: {r['side_by_side']}")
    L.append(f"report: {Path(r['side_by_side']).with_name('twin.json')}")
    return "\n".join(L)


def trend(path, r):
    p = Path(path)
    old = [json.loads(l) for l in p.read_text().splitlines() if l.strip()] if p.is_file() else []
    last = next((o for o in reversed(old) if o["scene"] == r["scene"]), None)
    rec = {"time": int(time.time()), "scene": r["scene"], "verdict": r["verdict"], "ssim": r["pixels"]["ssim"], "psnr": r["pixels"]["psnr_db"], "identical": r["serialized"]["identical"]}
    p.parent.mkdir(parents=True, exist_ok=True)
    with p.open("a") as f:
        f.write(json.dumps(rec) + "\n")
    return None if last is None else round(rec["ssim"] - last["ssim"], 4)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--scene", default="live")
    ap.add_argument("--renderdoc")
    ap.add_argument("--history")
    a = ap.parse_args()
    r = run(a.out, a.scene, a.renderdoc)
    print(text(r))
    if a.history:
        d = trend(a.history, r)
        print("trend: first run for this scene" if d is None else f"trend: ssim {'+' if d >= 0 else ''}{d} against the previous run")
    sys.exit(0 if r["verdict"] == "PASS" else 1)


if __name__ == "__main__":
    main()
