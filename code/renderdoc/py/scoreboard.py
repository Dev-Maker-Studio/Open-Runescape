import json, pathlib, re, sys, time

ROLES = ["model", "ground", "sprite/UI/minimap", "shadow", "particles", "water", "textures/materials", "camera/lighting/bloom", "equivalence-diff"]


def runs(path):
    return [json.loads(l) for l in path.read_text().splitlines() if l.strip()] if path.is_file() else []


def skipped(replay):
    m = re.search(r"skipped (\d+)(?: \[([^\]]*)\])?", replay or "")
    return f"{m.group(1)} {m.group(2) or ''}".strip() if m else "?"


def trend(rs):
    return " ".join(f"{r['pixels']['ssim']:.3f}" for r in rs[-8:])


def main(root):
    root = pathlib.Path(root)
    rows = ["| Slot | Role | SSIM | PSNR dB | Differing % | Skipped draws | First SSIM | Delta vs previous | Best | Runs | Last 8 SSIM |", "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"]
    for n in range(1, 10):
        rs = runs(root / f"history-s{n}.jsonl")
        if not rs:
            rows.append(f"| {n} | {ROLES[n - 1]} | none | | | | | | | 0 | |")
            continue
        p, last = rs[-1]["pixels"], rs[-1]
        prev = rs[-2]["pixels"]["ssim"] if len(rs) > 1 else p["ssim"]
        age = int(time.time() - last.get("time", 0)) // 60
        rows.append(f"| {n} | {ROLES[n - 1]} | {p['ssim']:.4f} | {p['psnr_db']} | {p['differing_pixels_pct']} | {skipped(last.get('replay'))} | {rs[0]['pixels']['ssim']:.4f} | {p['ssim'] - prev:+.4f} | {max(r['pixels']['ssim'] for r in rs):.4f} | {len(rs)}, last {age} min ago | {trend(rs)} |")
    print("\n".join(rows))


if __name__ == "__main__":
    main(sys.argv[1])
