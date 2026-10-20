import json
import sys
from pathlib import Path

s = json.loads((Path(sys.argv[1]) / "summary.json").read_text())
m = s["meta"]
print(f"{m['api']} draws={m['draws']} clears={m['clears']} targets={len(s['targets'])} shaders={len(s['shaders'])} capture={m['capture']}")
for t in s["targets"]:
    print(f"target {t['id']} {t['size'][0]}x{t['size'][1]} {t['format']}{' final' if t['final'] else ''} {t['png']}")
limit = int(sys.argv[2]) if len(sys.argv) > 2 else 100
for i, d in enumerate(s["draws"][:limit]):
    st = d.get("state", {})
    blend = "blend" if st.get("blend") else "opaque"
    depth = "depth" if st.get("depth") else "nodepth"
    print(f"{i:4} eid={d['eid']} {d['topology']} n={d['count']}{'i' if d['indexed'] else ''} inst={d['instances']} {d.get('program')} {blend} {depth} cull={st.get('cull')} tex={len(d['textures'])} ubo={d.get('uniform_hash')} -> {','.join(d['outputs'])}")
if len(s["draws"]) > limit:
    print(f"... {len(s['draws']) - limit} more draws in summary.json")
