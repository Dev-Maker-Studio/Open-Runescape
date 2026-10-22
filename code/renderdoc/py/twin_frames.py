import argparse
import json
import sys
import time
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diff
import twin

MEAN_SSIM = twin.PASS_SSIM
MIN_SSIM = 0.98
DYNAMIC_SHARE = 0.9
DYNAMIC_MAX_AREA = 0.25
SHEET_FRAMES = 12
OUTLIER_DROP = 0.1


def name(i):
    return f"frame-{i:04d}"


def load(path, size=None):
    img = Image.open(path).convert("RGB")
    if size and img.size != size:
        img = img.resize(size, Image.BILINEAR)
    return np.asarray(img, np.uint8)


def dilate(mask, k=3):
    return diff.box(np.pad(mask, k).astype(np.float64), 2 * k + 1) > 0


def motion(current, neighbours):
    mask = np.zeros(current[0].shape[:2], bool)
    for neighbour in neighbours:
        if neighbour is not None:
            for a, b in zip(current, neighbour):
                mask |= np.abs(a.astype(np.int16) - b.astype(np.int16)).max(2) > 8
    return dilate(mask)


def trend(ssims):
    n = len(ssims)
    slope = float(np.polyfit(np.arange(n), ssims, 1)[0]) if n > 1 else 0.0
    half = max(1, n // 2)
    total = slope * (n - 1)
    return {
        "slope_per_frame": round(slope, 6),
        "first_half_mean": round(float(np.mean(ssims[:half])), 4),
        "second_half_mean": round(float(np.mean(ssims[half:] or ssims[:half])), 4),
        "direction": "flat" if abs(total) < 0.005 else ("improving" if total > 0 else "degrading"),
    }


def outliers(ssims):
    mid = float(np.median(ssims))
    return [i for i, v in enumerate(ssims) if v < mid - OUTLIER_DROP]


def verdict(records):
    mean = float(np.mean([r["ssim"] for r in records]))
    low = min(r["ssim"] for r in records)
    same = all(r["identical"] for r in records)
    return ("PASS" if same and mean >= MEAN_SSIM and low >= MIN_SSIM else "FAIL"), mean, low, same


def sheet_indices(n):
    return sorted({int(round(i)) for i in np.linspace(0, n - 1, min(n, SHEET_FRAMES))})


def thumb(img, width):
    return img.resize((width, max(1, round(img.height * width / img.width))), Image.BILINEAR)


def panel(c, w, d, width, caption):
    gap, head = 4, 20
    a, b, e = (thumb(x, width) for x in (c, w, d))
    out = Image.new("RGB", (3 * width + 4 * gap, head + a.height + gap), (24, 24, 28))
    ImageDraw.Draw(out).text((gap, 4), caption, fill=(255, 220, 120), font=twin.font(12))
    for i, x in enumerate((a, b, e)):
        out.paste(x, (gap + i * (width + gap), head))
    return out


def contact_sheet(frames_dir, records, picked, out_png, cols=2, width=240):
    cells = []
    for i in picked:
        r = records[i]
        d = frames_dir / f"{i:04d}"
        cells.append(panel(Image.open(d / "client.png"), Image.open(d / "web.png"), Image.open(d / "diff.png"), width, f"#{i}  render frame {r['frame']}  ssim {r['ssim']}  differing {r['differing_pct']}%"))
    cw, ch = cells[0].size
    rows = -(-len(cells) // cols)
    sheet = Image.new("RGB", (cols * cw, rows * ch), (24, 24, 28))
    for k, cell in enumerate(cells):
        sheet.paste(cell, ((k % cols) * cw, (k // cols) * ch))
    sheet.save(out_png)
    return sheet.size


def animation(frames_dir, records, out_gif, width=320, ms=120):
    imgs = []
    for i, r in enumerate(records):
        d = frames_dir / f"{i:04d}"
        p = panel(Image.open(d / "client.png"), Image.open(d / "web.png"), Image.open(d / "diff.png"), width, f"client | web | diff x4   #{i}  ssim {r['ssim']}")
        imgs.append(p.convert("P", palette=Image.ADAPTIVE, colors=128))
    imgs[0].save(out_gif, save_all=True, append_images=imgs[1:], duration=ms, loop=0, optimize=False)


def history(path, rec):
    p = Path(path)
    old = [json.loads(line) for line in p.read_text().splitlines() if line.strip()] if p.is_file() else []
    last = next((o for o in reversed(old) if o["scene"] == rec["scene"] and o.get("frames", 1) == rec["frames"] and o.get("every", 1) == rec.get("every", 1)), None)
    p.parent.mkdir(parents=True, exist_ok=True)
    with p.open("a") as f:
        f.write(json.dumps(rec) + "\n")
    return None if last is None else round(rec["ssim"] - last["ssim"], 4)


def stats(values):
    finite = [v for v in values if v is not None]
    return {k: round(float(f(finite)), 4) if finite else None for k, f in (("min", min), ("mean", np.mean), ("max", max))}


def run(out, scene, every, renderdoc=None, expected=None):
    out = Path(out)
    client, web = out / "client", out / "web"
    meta = json.loads((client / "frames.json").read_text())
    entries = meta["frames"]
    n = len(entries)
    if not n or meta.get("error") or (expected is not None and n != expected):
        raise ValueError(f"incomplete capture: {n}/{expected}, {meta.get('error')}")
    def pair(i):
        c = load(client / f"{name(i)}.png")
        size = (c.shape[1], c.shape[0])
        return c, load(web / f"{name(i)}.png", size)

    previous = None
    current = pair(0)
    frames_dir = out / "frames"
    records = []
    for i, e in enumerate(entries):
        d = frames_dir / f"{i:04d}"
        d.mkdir(parents=True, exist_ok=True)
        following = pair(i + 1) if i + 1 < n else None
        c, w = current
        mask = motion(current, (previous, following))
        Image.fromarray(c).save(d / "client.png")
        Image.fromarray(w).save(d / "web.png")
        px = diff.pixels(d / "client.png", d / "web.png", d / "diff.png")
        orf = twin.serialized(client / f"{name(i)}.orf", web / f"{name(i)}.orf")
        off = np.abs(c.astype(np.int16) - w.astype(np.int16)).max(2) > 8
        count = int(off.sum())
        share = None if count == 0 else round(float((off & mask).sum()) / count, 4)
        rec = {
            "index": i,
            "frame": e["frame"],
            "tick": e["tick"],
            "nanos": e["nanos"],
            "gameState": e.get("gameState"),
            "ssim": px["ssim"],
            "psnr_db": px["psnr_db"],
            "mse": px["mse"],
            "max_diff": px["max_diff"],
            "differing_pct": px["differing_pixels_pct"],
            "identical": orf["identical"],
            "first_difference": orf["first_difference"],
            "dynamic_area_pct": round(100 * float(mask.mean()), 2),
            "dynamic_share": share,
            "dynamic_only": share is not None and share >= DYNAMIC_SHARE and float(mask.mean()) <= DYNAMIC_MAX_AREA,
            "regions": twin.regions(c, w, 3),
            "region_ssim": twin.region_ssim(c, w),
            "serialized": orf,
            "dir": str(d),
        }
        records.append(rec)
        twin.compose(d / "client.png", d / "web.png", d / "diff.png", d / "twin.png", f"twin {scene} #{i} render frame {e['frame']} tick {e['tick']}", [f"ssim {rec['ssim']}   psnr {rec['psnr_db']} dB   differing {rec['differing_pct']}%   serialized identical {rec['identical']}   dynamic only {rec['dynamic_only']}"])
        previous, current = current, following
    ssims = [r["ssim"] for r in records]
    psnrs = [r["psnr_db"] for r in records if r["psnr_db"] is not None]
    result, mean, low, same = verdict(records)
    worst = min(records, key=lambda r: r["ssim"])
    picked = sheet_indices(n)
    contact_sheet(frames_dir, records, picked, out / "twin-frames.png")
    animation(frames_dir, records, out / "twin.gif")
    rd = twin.read_json(renderdoc) if renderdoc else None
    report = {
        "scene": scene,
        "mode": "frames",
        "verdict": result,
        "frames": n,
        "every": every,
        "thresholds": {"mean_ssim": MEAN_SSIM, "min_ssim": MIN_SSIM, "serialized_frames_identical": True, "dynamic_share": DYNAMIC_SHARE, "dynamic_max_area": DYNAMIC_MAX_AREA},
        "summary": {
            "ssim": stats(ssims),
            "psnr_db": stats([r["psnr_db"] for r in records]),
            "differing_pct": stats([r["differing_pct"] for r in records]),
            "psnr_db_mean": round(float(np.mean(psnrs)), 2) if psnrs else None,
            "differing_pct_mean": round(float(np.mean([r["differing_pct"] for r in records])), 3),
            "worst": {"index": worst["index"], "frame": worst["frame"], "tick": worst["tick"], "ssim": worst["ssim"]},
            "trend": trend(ssims),
            "serialized_identical": sum(r["identical"] for r in records),
            "all_serialized_identical": same,
            "dynamic_only_frames": [r["index"] for r in records if r["dynamic_only"]],
            "outlier_frames": outliers(ssims),
        },
        "frame_records": records,
        "state": {"client": twin.read_json(client / "state.json"), "web": twin.read_json(web / "state.json")},
        "describe": {"client": twin.read_text(client / "describe.txt"), "web": twin.read_text(web / "describe.txt")},
        "renderdoc": None if rd is None else {k: rd.get(k) for k in ("verdict", "score", "previous_score", "checks", "draws")},
        "contact_sheet": str(out / "twin-frames.png"),
        "contact_sheet_frames": picked,
        "animation": str(out / "twin.gif"),
    }
    (out / "twin.json").write_text(json.dumps(report, indent=1))
    return report


def text(r):
    s = r["summary"]
    q = s["ssim"]
    t = s["trend"]
    w = s["worst"]
    lines = [
        f"TWIN scene={r['scene']} frames={r['frames']} every={r['every']} verdict={r['verdict']}",
        f"ssim: mean={q['mean']} min={q['min']} max={q['max']}  psnr mean={s['psnr_db_mean']}dB  differing mean={s['differing_pct_mean']}%",
        f"worst frame: #{w['index']} (render frame {w['frame']}, tick {w['tick']}) ssim={w['ssim']}",
        f"trend: {t['direction']}, slope {t['slope_per_frame']}/frame, first half {t['first_half_mean']}, second half {t['second_half_mean']}",
        f"serialized frames identical: {s['serialized_identical']}/{r['frames']}",
        f"frames where only time dependent regions differ: {len(s['dynamic_only_frames'])} {s['dynamic_only_frames'][:20]}",
        f"outlier frames (ssim more than {OUTLIER_DROP} under the median): {len(s['outlier_frames'])} {s['outlier_frames'][:20]}",
        f"thresholds: mean ssim >= {r['thresholds']['mean_ssim']}, min ssim >= {r['thresholds']['min_ssim']}, all serialized identical",
    ]
    worst = r["frame_records"][w["index"]]
    if worst["regions"]:
        lines.append("worst frame regions (x0,y0,x1,y1):")
        lines += [f"  {g['box']} mean_diff={g['mean_diff']} differing={g['differing_pct']}%" for g in worst["regions"]]
    if r["renderdoc"]:
        rd = r["renderdoc"]
        lines.append(f"renderdoc equiv: verdict={rd['verdict']} score={rd['score']} previous={rd['previous_score']}")
    lines.append(f"contact sheet: {r['contact_sheet']}")
    lines.append(f"animation: {r['animation']}")
    lines.append(f"report: {Path(r['contact_sheet']).with_name('twin.json')}")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--scene", default="live")
    ap.add_argument("--frames", type=int, required=True)
    ap.add_argument("--every", type=int, default=1)
    ap.add_argument("--renderdoc")
    ap.add_argument("--history")
    a = ap.parse_args()
    r = run(a.out, a.scene, a.every, a.renderdoc, a.frames)
    print(text(r))
    if a.history:
        q = r["summary"]["ssim"]
        d = history(a.history, {"time": int(time.time()), "scene": r["scene"], "verdict": r["verdict"], "frames": r["frames"], "every": r["every"], "ssim": q["mean"], "min_ssim": q["min"], "identical": r["summary"]["all_serialized_identical"]})
        print("trend: first run for this scene and frame count" if d is None else f"trend: mean ssim {'+' if d >= 0 else ''}{d} against the previous run")
    sys.exit(0 if r["verdict"] == "PASS" else 1)


if __name__ == "__main__":
    main()
