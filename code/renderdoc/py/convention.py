import re

CLIP = re.compile(r"(?i)^(?!.*inv).*(projection|shadow|mvp)")
RELATIVE = 1e-3


def is_clip_matrix(key, values):
    return len(values) == 16 and bool(CLIP.match(key.split(".")[-1]))


def depth_range(m):
    return m[:8] + [(z + w) / 2 for z, w in zip(m[8:12], m[12:16])] + m[12:]


def to_web(summary):
    if summary["meta"].get("api") != "OpenGL":
        return summary
    for d in summary["draws"]:
        u = d.get("uniforms", {})
        for k, v in u.items():
            if is_clip_matrix(k, v):
                u[k] = depth_range(v)
    return summary


def same(x, y):
    return abs(x - y) <= RELATIVE * max(1.0, abs(x), abs(y))


def alpha_only(cb, wb):
    if len(cb) != len(wb):
        return False
    return all(a != b and {**a, "asrc": 0} == {**b, "asrc": 0} and a["asrc"] == "SrcAlpha" and b["asrc"] == "One" for a, b in zip(cb, wb))


def placeholder(t):
    return t["size"][:2] == [1, 1]


def textures(ct, wt):
    a = {t["hash"]: t for t in ct if not placeholder(t)}
    b = {t["hash"]: t for t in wt if not placeholder(t)}
    out = []
    for h in sorted(a.keys() - b.keys()):
        t = a[h]
        m = next((x for x in sorted(b.keys() - a.keys()) if x == t.get("flip_hash") or b[x].get("flip_hash") == h), None)
        if m:
            b.pop(m)
            a[h] = None
            out.append(f"hash client={h} web={m} {t['size']} {t['format']} vertically flipped")
    for h in sorted(a.keys() & b.keys()):
        r = a[h].get("range") if a[h] else None
        if r and (r[0] < 0 or r[1] > 1) and "FLOAT" not in b[h]["format"]:
            out.append(f"hash {h} {b[h]['size']} client {a[h]['format']} holds {r}, web {b[h]['format']} clamps it to 0..1")
    left_a = sorted(h for h in a.keys() - b.keys() if a[h])
    left_b = sorted(b.keys() - a.keys())
    out += [f"hash only client={h} {a[h]['size']} {a[h]['format']}" for h in left_a]
    out += [f"hash only web={h} {b[h]['size']} {b[h]['format']}" for h in left_b]
    return out, []
