import json, os, subprocess, sys, tempfile, warnings
warnings.filterwarnings("ignore")
from PIL import Image

def rdc(*a):
    return subprocess.run(["rdc", *a], capture_output=True, text=True).stdout

def table(text):
    l = text.splitlines()
    h = [c.lower() for c in l[0].split("\t")] if l else []
    return [dict(zip(h, r.split("\t"))) for r in l[1:] if r]

cap = sys.argv[1]
rdc("close"); rdc("open", cap)
draws = table(rdc("draws"))
res = table(rdc("resources"))
log = table(rdc("log"))
out, fails = [], 0

def check(name, ok, detail=""):
    global fails
    fails += not ok
    out.append(f"{'PASS' if ok else 'FAIL'}  {name}  {detail}")

bad = [m for m in log if m.get("level", "").lower() in ("high", "medium")]
check("no high or medium API debug messages", not bad, f"{len(bad)} of {len(log)}")
for m in bad[:10]:
    out.append("      " + " ".join(m.values())[:200])
check("draw calls present", bool(draws), str(len(draws)))
check("draw calls under budget (<=20000)", len(draws) <= 20000, str(len(draws)))
tex = [r for r in res if r.get("type") == "Texture"]
out.append(f"INFO  textures {len(tex)}")
if draws:
    eid = draws[-1].get("eid", "")
    tmp = tempfile.mkdtemp()
    f = f"{tmp}/final.png"
    rdc("rt", eid, "-o", f)
    if os.path.exists(f):
        im = Image.open(f).convert("RGB")
        colours = len(set(im.resize((128, 128)).getdata()))
        check("final render target not blank", colours > 8, f"{im.size[0]}x{im.size[1]} {colours} colours at event {eid}")
    else:
        check("final render target readable", False, f"event {eid}")
    pipe = rdc("pipeline", eid, "--json")
    check("pipeline state readable for last draw", bool(pipe.strip()), f"event {eid}")
rdc("close")
print("\n".join(out)); sys.exit(1 if fails else 0)
