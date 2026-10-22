import argparse
import json
import sys
from pathlib import Path


def run(out, count, scene):
    out = Path(out)
    records = []
    for i in range(count):
        path = out / "frames" / f"{i:04d}" / "equiv.json"
        r = json.loads(path.read_text())
        records.append({"index": i, "verdict": r["verdict"], "score": r["score"], "ssim": r["pixels"]["ssim"] if r.get("pixels") else None, "report": str(path)})
    result = "PASS" if all(r["verdict"] == "PASS" for r in records) else "FAIL"
    ssims = [r["ssim"] for r in records if r["ssim"] is not None]
    report = {"scene": scene, "frames": count, "verdict": result, "score": round(sum(r["score"] for r in records) / count, 2), "mean_ssim": round(sum(ssims) / count, 4) if len(ssims) == count else None, "frame_records": records}
    (out / "equiv.json").write_text(json.dumps(report, indent=1))
    lines = [f"EQUIV scene={scene} frames={count} verdict={result} mean score={report['score']}/100 mean ssim={report['mean_ssim']}"]
    lines += [f"frame #{r['index']}: verdict={r['verdict']} score={r['score']}/100 ssim={r['ssim']}" for r in records]
    text = "\n".join(lines)
    (out / "report.txt").write_text(text + "\n")
    print(text)
    return report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--frames", type=int, required=True, choices=range(1, 6))
    ap.add_argument("--scene", required=True)
    a = ap.parse_args()
    sys.exit(0 if run(a.out, a.frames, a.scene)["verdict"] == "PASS" else 1)


if __name__ == "__main__":
    main()
