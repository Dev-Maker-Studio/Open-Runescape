import hashlib
import json
import multiprocessing
import os
import sys
import time
from concurrent.futures import ProcessPoolExecutor
from concurrent.futures.process import BrokenProcessPool
from pathlib import Path

import numpy as np

sys.path.insert(0, os.environ["RENDERDOC_PYTHON_PATH"])
import renderdoc as rd

import diff

VERSION = 6
TINT = 23
SCREEN_Y = {"aClip"}
LIMIT = 4 << 20
STAGES = {"vs": rd.ShaderStage.Vertex, "fs": rd.ShaderStage.Pixel, "cs": rd.ShaderStage.Compute}
NULL = rd.ResourceId.Null()
DTYPE = {
    (rd.CompType.Float, 8): "<f8", (rd.CompType.Float, 4): "<f4", (rd.CompType.Float, 2): "<f2",
    (rd.CompType.UInt, 1): "u1", (rd.CompType.UInt, 2): "<u2", (rd.CompType.UInt, 4): "<u4",
    (rd.CompType.SInt, 1): "i1", (rd.CompType.SInt, 2): "<i2", (rd.CompType.SInt, 4): "<i4",
    (rd.CompType.UNorm, 1): "u1", (rd.CompType.UNorm, 2): "<u2",
    (rd.CompType.SNorm, 1): "i1", (rd.CompType.SNorm, 2): "<i2",
}
NORM = {rd.CompType.UNorm: lambda w: 2.0 ** (8 * w) - 1, rd.CompType.SNorm: lambda w: 2.0 ** (8 * w - 1) - 1}
VARS = {rd.VarType.Float: "f32v", rd.VarType.Double: "f64v", rd.VarType.Half: "f16v", rd.VarType.SInt: "s32v", rd.VarType.UInt: "u32v"}


def h(b):
    return hashlib.blake2b(b, digest_size=6).hexdigest()


def nm(x):
    return getattr(x, "name", str(x))


def rid(x):
    return int(x) if x != NULL else 0


def open_capture(path):
    rd.InitialiseReplay(rd.GlobalEnvironment(), [])
    cap = rd.OpenCaptureFile()
    r = cap.OpenFile(path, "", None)
    if not r.OK():
        raise SystemExit(f"cannot open {path}: {r}")
    r, ctl = cap.OpenCapture(rd.ReplayOptions(), None)
    if not r.OK():
        raise SystemExit(f"cannot replay {path}: {r}")
    return cap, ctl


def actions(ctl):
    sf = ctl.GetStructuredFile()
    out = []

    def walk(a, path):
        f = a.flags
        kind = "draw" if f & rd.ActionFlags.Drawcall else "dispatch" if f & rd.ActionFlags.Dispatch else "clear" if f & rd.ActionFlags.Clear else None
        if kind:
            out.append((a.eventId, kind, "/".join(path), a.GetName(sf)))
        sub = path + [a.GetName(sf)] if f & rd.ActionFlags.PushMarker else path
        for c in a.children:
            walk(c, sub)

    for a in ctl.GetRootActions():
        walk(a, [])
    return out


def tint(refl):
    raw = bytes(refl.rawBytes)
    return len(raw) > 12 and int.from_bytes(raw[8:12], "little") >> 16 == TINT


def words(raw):
    u = np.frombuffer(raw[: len(raw) // 4 * 4], "<u4")
    f = u.view("<f4")
    with np.errstate(invalid="ignore"):
        return np.round(np.where((u < 1 << 23) | np.isnan(f), u.view("<i4"), f).astype(np.float64), 4).tolist()


def flat(v, prefix=""):
    if v.members:
        return {k: x for m in v.members for k, x in flat(m, f"{prefix}{v.name}.").items()}
    n = max(v.rows, 1) * max(v.columns, 1)
    vals = np.asarray(list(getattr(v.value, VARS.get(v.type, "f32v")))[:n], dtype=np.float64)
    return {prefix + v.name: np.round(vals, 4).tolist()}


def top_left_y(y, height, target_height):
    return target_height - y - height


def relative_indices(idx):
    return idx - idx.min() if len(idx) else idx


def rgba8(f, a, t):
    c = f.compCount
    if f.type != rd.ResourceFormatType.Regular or c not in (1, 2, 3, 4) or not a.size:
        return None
    if f.compByteWidth == 1 and c == 4 and a.size % max(t.height, 1) == 0:
        px = a.reshape(-1, 4)
        return px[:, [2, 1, 0, 3]] if f.Name().startswith("B8G8R8A8") else px
    if f.compByteWidth == 1 and a.size == t.width * t.height * c:
        return expand_rgba(a.reshape(-1, c))
    if f.compType == rd.CompType.Float and f.compByteWidth == 2 and a.size == t.width * t.height * c * 2:
        return expand_rgba(np.rint(np.clip(a.view("<f2").astype(np.float32), 0, 1) * 255).astype(np.uint8).reshape(-1, c))
    return None


def value_range(f, a):
    if f.compType != rd.CompType.Float or f.compByteWidth not in (2, 4) or a.size % f.compByteWidth:
        return None
    v = a.view("<f2" if f.compByteWidth == 2 else "<f4")
    v = v[np.isfinite(v)]
    return [round(float(v.min()), 3), round(float(v.max()), 3)] if len(v) else None


def expand_rgba(px):
    out = np.zeros((len(px), 4), np.uint8)
    out[:, 3] = 255
    out[:, : px.shape[1]] = px
    return out


def top_left_clip(d, target_height):
    d = d.copy()
    d[:, [1, 3]] = target_height - d[:, [3, 1]]
    return d


def trim_zero_components(d):
    used = np.flatnonzero(d.any(0))
    return d[:, : used[-1] + 1 if len(used) else 1]


def decode(raw, off, stride, idx, f):
    dt = DTYPE.get((f.compType, f.compByteWidth))
    if f.type != rd.ResourceFormatType.Regular or dt is None:
        return None
    size = f.compCount * f.compByteWidth
    pos = off + idx.astype(np.int64) * stride
    ok = (pos >= 0) & (pos + size <= len(raw))
    cols = pos[ok][:, None] + np.arange(size)[None, :]
    arr = np.ascontiguousarray(raw[cols]).view(dt).reshape(-1, f.compCount).astype(np.float64)
    if f.compType in NORM:
        arr /= NORM[f.compType](f.compByteWidth)
    return arr


class Replay:
    def __init__(self, path):
        self.cap, self.ctl = open_capture(path)
        self.tex = {int(t.resourceId): t for t in self.ctl.GetTextures()}
        self.thash = {}
        self.acts = {}
        stack = list(self.ctl.GetRootActions())
        while stack:
            a = stack.pop()
            self.acts[a.eventId] = a
            stack.extend(a.children)

    def buf(self, r, off=0, size=0):
        return self.ctl.GetBufferData(r, off, min(size, LIMIT) if size else 0)

    def texture(self, r):
        t = self.tex.get(int(r))
        if t is None:
            return None
        key = int(r)
        if key in self.thash:
            return self.thash[key]
        raw = self.ctl.GetTextureData(r, rd.Subresource(0, 0, 0))
        a = np.frombuffer(raw, np.uint8)
        f = t.format
        px = rgba8(f, a, t)
        if px is not None:
            rows = np.ascontiguousarray(px).reshape(t.height, -1)
            res = (h(rows.tobytes()), h(rows[::-1].tobytes()), value_range(f, a))
        else:
            res = (h(raw), None, value_range(f, a))
        if not t.creationFlags & (rd.TextureCategory.ColorTarget | rd.TextureCategory.DepthTarget):
            self.thash[key] = res
        return res

    def height(self, p):
        return next((self.tex[int(o.resource)].height for o in p.GetOutputTargets() if o.resource != NULL and int(o.resource) in self.tex), None)

    def state(self, p):
        gl = self.ctl.GetGLPipelineState() if p.IsCaptureGL() else None
        vk = self.ctl.GetVulkanPipelineState() if p.IsCaptureVK() else None
        if gl:
            ras, dep, sten = gl.rasterizer.state, gl.depthState, gl.stencilState.stencilEnable
            dtest, dwrite = dep.depthEnable, dep.depthWrites
        elif vk:
            ras, dep, sten = vk.rasterizer, vk.depthStencil, vk.depthStencil.stencilTestEnable
            dtest, dwrite = dep.depthTestEnable, dep.depthWriteEnable
        else:
            return {}
        vp, sc = p.GetViewport(0), p.GetScissor(0)
        vx, vy, vw, vh = vp.x, vp.y + min(vp.height, 0), vp.width, abs(vp.height)
        full = not sc.enabled or (sc.x <= 0 and sc.y <= 0 and sc.width >= vw and sc.height >= vh)
        sy = sc.y
        th = self.height(p) if gl else None
        if th is not None:
            sy = top_left_y(sc.y, sc.height, th)
            vy = top_left_y(vy, vh, th)
        blends = [
            {"src": nm(b.colorBlend.source), "dst": nm(b.colorBlend.destination), "op": nm(b.colorBlend.operation),
             "asrc": nm(b.alphaBlend.source), "adst": nm(b.alphaBlend.destination), "aop": nm(b.alphaBlend.operation), "mask": b.writeMask}
            for b in p.GetColorBlends()[: max(sum(o.resource != NULL for o in p.GetOutputTargets()), 1)] if b.enabled
        ]
        return {
            "blend": blends,
            "cull": nm(ras.cullMode), "front_ccw": bool(ras.frontCCW), "fill": nm(ras.fillMode),
            "depth": {"write": bool(dwrite), "func": nm(dep.depthFunction)} if dtest else None,
            "stencil": bool(sten),
            "viewport": [round(vx), round(vy), round(vw), round(vh)],
            "scissor": None if full else [sc.x, sy, sc.width, sc.height],
        }

    def vertices(self, p, a, refl):
        vbs, ins = p.GetVBuffers(), p.GetVertexInputs()
        n = max(a.numIndices, 0)
        stride_i = 0
        idx = np.arange(a.vertexOffset, a.vertexOffset + n)
        index = None
        if a.flags & rd.ActionFlags.Indexed:
            ib = p.GetIBuffer()
            stride_i = ib.byteStride or 2
            raw = np.frombuffer(self.buf(ib.resourceId, ib.byteOffset + a.indexOffset * stride_i, n * stride_i), np.uint8)
            idx = np.frombuffer(raw[: len(raw) // stride_i * stride_i].tobytes(), {1: "u1", 2: "<u2", 4: "<u4"}[stride_i]).astype(np.int64) + a.baseVertex
            rel = relative_indices(idx)
            index = {"format": f"u{stride_i * 8}", "hash": h(rel.tobytes()), "max": int(rel.max()) if len(rel) else 0}
        inst = np.arange(a.instanceOffset, a.instanceOffset + max(a.numInstances, 1))
        attrs, raws = [], {}
        th = self.height(p) if p.IsCaptureGL() else None
        for s in refl.inputSignature if refl else []:
            at = next((a for a in ins if a.name and a.name == s.varName), ins[s.regIndex] if s.regIndex < len(ins) else None)
            if s.systemValue != rd.ShaderBuiltin.Undefined or at is None:
                continue
            if at.vertexBuffer >= len(vbs) or vbs[at.vertexBuffer].resourceId == NULL:
                continue
            vb = vbs[at.vertexBuffer]
            sel = inst // max(at.instanceRate, 1) if at.perInstance else idx
            if not len(sel):
                continue
            lo, hi = int(sel.min()), int(sel.max())
            key = (int(vb.resourceId), vb.byteOffset, lo, hi)
            if key not in raws:
                raws[key] = np.frombuffer(self.ctl.GetBufferData(vb.resourceId, vb.byteOffset + lo * vb.byteStride, (hi - lo + 1) * vb.byteStride + 64), np.uint8)
            d = decode(raws[key], at.byteOffset, vb.byteStride, sel - lo, at.format)
            rec = {"loc": s.regIndex, "name": s.varName, "format": at.format.Name(), "per_instance": bool(at.perInstance)}
            if d is not None:
                if th is not None and s.varName in SCREEN_Y and d.shape[1] == 4:
                    d = top_left_clip(d, th)
                d = np.round(trim_zero_components(d), 4)
                rec |= {"count": len(d), "hash": h(d.astype(np.float32).tobytes()), "head": d[:2].ravel().tolist()[:6]}
            attrs.append(rec)
        return attrs, index

    def uniforms(self, p, stage, refl):
        out = {}
        for i, cb in enumerate(refl.constantBlocks):
            d = p.GetConstantBlock(stage, i, 0).descriptor
            if tint(refl):
                out[f"{cb.name or 'block'}{i}._words"] = words(bytes(self.buf(d.resource, d.byteOffset, d.byteSize)))
                continue
            pipe = p.GetComputePipelineObject() if stage == rd.ShaderStage.Compute else p.GetGraphicsPipelineObject()
            vs = self.ctl.GetCBufferVariableContents(pipe, p.GetShader(stage), stage, p.GetShaderEntryPoint(stage), i, d.resource, d.byteOffset, d.byteSize)
            for v in vs:
                out |= flat(v, f"{cb.name}." if cb.name and cb.name != "$Globals" else "")
        return out

    def draw(self, item):
        eid, kind, marker, name = item
        self.ctl.SetFrameEvent(eid, False)
        a = self.ctl.GetPipelineState()
        act = self.acts[eid]
        rec = {"eid": eid, "kind": kind, "name": name, "marker": marker, "outputs": [rid(o) for o in act.outputs if o != NULL], "depth": rid(act.depthOut)}
        if kind == "clear":
            return rec
        rec |= {"topology": nm(a.GetPrimitiveTopology()), "indexed": bool(act.flags & rd.ActionFlags.Indexed), "count": act.numIndices, "instances": max(act.numInstances, 1), }
        if kind == "dispatch":
            rec["dispatch"] = list(act.dispatchDimension)
        rec["shader_ids"], rec["uniforms"], rec["textures"], rec["buffers"] = {}, {}, [], []
        for key, stage in STAGES.items():
            sh = a.GetShader(stage)
            if sh == NULL:
                continue
            refl = a.GetShaderReflection(stage)
            rec["shader_ids"][key] = int(sh)
            rec["uniforms"] |= {f"{key}.{k}": v for k, v in self.uniforms(a, stage, refl).items()}
            if key == "vs":
                rec["vertex"], rec["index"] = self.vertices(a, act, refl)
            for u in a.GetReadOnlyResources(stage):
                if u.access.staticallyUnused or u.descriptor.resource == NULL:
                    continue
                r = u.descriptor.resource
                if int(r) in self.tex:
                    t = self.tex[int(r)]
                    th = self.texture(r)
                    rec["textures"].append({"stage": key, "slot": u.access.index, "rid": int(r), "size": [t.width, t.height, t.depth], "mips": t.mips, "format": t.format.Name(), "hash": th[0], "flip_hash": th[1], **({"range": th[2]} if th[2] else {})})
                else:
                    rec["buffers"].append({"stage": key, "slot": u.access.index, "hash": h(bytes(self.buf(r)))})
        rec["state"] = self.state(a)
        rec["uniform_hash"] = h(json.dumps(rec["uniforms"], sort_keys=True).encode())
        return rec


_worker = None


def _init(path):
    global _worker
    _worker = Replay(path)


def _run(chunk):
    return [_worker.draw(i) for i in chunk]


def ordinals(prefix):
    seen = {}
    return lambda k: seen.setdefault(k, f"{prefix}{len(seen)}")


def export_targets(rp, recs, out, tex_ord):
    last = {}
    for r in recs:
        for o in r["outputs"]:
            last[o] = r["eid"]
    swap = [int(t.resourceId) for t in rp.tex.values() if t.creationFlags & rd.TextureCategory.SwapBuffer]
    final = next((s for s in swap if s in last), next((o for r in reversed(recs) if r.get("origin") != "browser" for o in r["outputs"][:1]), None))
    (out / "targets").mkdir(parents=True, exist_ok=True)
    targets = []
    for r, eid in last.items():
        t = rp.tex.get(r)
        if t is None:
            continue
        rp.ctl.SetFrameEvent(eid, True)
        ts = rd.TextureSave()
        ts.resourceId, ts.destType, ts.mip, ts.slice.sliceIndex = t.resourceId, rd.FileType.PNG, 0, 0
        ts.alpha = rd.AlphaMapping.Discard if r == final else rd.AlphaMapping.Preserve
        png = out / "targets" / f"{tex_ord(r)}.png"
        rp.ctl.SaveTexture(ts, str(png))
        targets.append({"id": tex_ord(r), "size": [t.width, t.height], "format": t.format.Name(), "samples": t.msSamp, "final": r == final, "swapchain": bool(t.creationFlags & rd.TextureCategory.SwapBuffer), "last_eid": eid, "png": f"targets/{tex_ord(r)}.png" if png.exists() else None})
    return targets


def export_shaders(rp, recs, out, sh_ord):
    first = {}
    for r in recs:
        for k, v in r.get("shader_ids", {}).items():
            first.setdefault(v, (r["eid"], k))
    shaders, ids = {}, {}
    (out / "shaders").mkdir(parents=True, exist_ok=True)
    for v, (eid, key) in first.items():
        rp.ctl.SetFrameEvent(eid, False)
        p = rp.ctl.GetPipelineState()
        refl = p.GetShaderReflection(STAGES[key])
        raw = bytes(refl.rawBytes)
        sid = ids[v] = sh_ord((key, h(raw)))
        if sid in shaders:
            continue
        text = raw.decode("utf-8", "replace")
        if p.IsCaptureVK():
            text = rp.ctl.DisassembleShader(p.GetGraphicsPipelineObject(), refl, rp.ctl.GetDisassemblyTargets(True)[0])
        (out / "shaders" / f"{sid}_{key}.txt").write_text(text)
        shaders[sid] = {
            "stage": key, "entry": refl.entryPoint, "hash": h(raw),
            "inputs": [f"{s.regIndex}:{s.varName}:{nm(s.varType)}x{s.compCount}" for s in refl.inputSignature if s.systemValue == rd.ShaderBuiltin.Undefined],
            "outputs": len(refl.outputSignature), "blocks": [c.name for c in refl.constantBlocks], "file": f"shaders/{sid}_{key}.txt",
        }
    return shaders, ids


def dump(path, out, jobs):
    t0 = time.time()
    out.mkdir(parents=True, exist_ok=True)
    rp = Replay(path)
    evs = actions(rp.ctl)
    api = nm(rp.ctl.GetAPIProperties().pipelineType)
    if rp.ctl.GetAPIProperties().pipelineType == rd.GraphicsAPI.Vulkan and "OPENRS_JOBS" not in os.environ:
        jobs = 1
    jobs = max(1, min(jobs, len(evs) // int(os.environ.get("OPENRS_CHUNK", 150))))
    recs = None
    if jobs > 1:
        size = -(-len(evs) // jobs)
        chunks = [evs[i : i + size] for i in range(0, len(evs), size)]
        try:
            with ProcessPoolExecutor(jobs, mp_context=multiprocessing.get_context("spawn"), initializer=_init, initargs=(path,)) as ex:
                recs = [r for part in ex.map(_run, chunks) for r in part]
        except BrokenProcessPool:
            print(f"parallel replay failed with {jobs} workers, replaying serially", file=sys.stderr)
    if recs is None:
        recs = [rp.draw(i) for i in evs]
    diff.tag(recs)
    t1 = time.time()
    tex_ord, sh_ord, prog_ord, dep_ord = ordinals("T"), ordinals("S"), ordinals("P"), ordinals("D")
    targets = export_targets(rp, recs, out, tex_ord)
    shaders, sids = export_shaders(rp, recs, out, sh_ord)
    for r in recs:
        r["outputs"] = [tex_ord(o) for o in r["outputs"] if o in rp.tex]
        r["depth"] = dep_ord(r["depth"]) if r["depth"] else None
        if "shader_ids" in r:
            r["shaders"] = {k: sids[v] for k, v in r.pop("shader_ids").items()}
            r["program"] = prog_ord(tuple(sorted(r["shaders"].items())))
            for t in r["textures"]:
                t["id"] = tex_ord(t.pop("rid"))
    meta = {"schema": VERSION, "api": api, "draws": sum(r["kind"] != "clear" for r in recs), "clears": sum(r["kind"] == "clear" for r in recs), "browser": sum(r.get("origin") == "browser" for r in recs), "seconds": {"replay": round(t1 - t0, 2), "export": round(time.time() - t1, 2)}}
    return meta, [r for r in recs if r["kind"] != "clear"], [r for r in recs if r["kind"] == "clear"], shaders, targets


def stamp(path):
    st = Path(path).stat()
    return f"{VERSION}:{st.st_size}:{st.st_mtime_ns}"


def write(out, meta, draws, clears, shaders, targets):
    body = lambda xs: "[\n" + ",\n".join(json.dumps(x, separators=(",", ":")) for x in xs) + "\n]"
    text = '{"meta":' + json.dumps(meta) + ',\n"targets":' + body(targets) + ',\n"shaders":' + json.dumps(shaders, separators=(",", ":")) + ',\n"clears":' + body(clears) + ',\n"draws":' + body(draws) + "\n}\n"
    (out / "summary.json").write_text(text)


def main():
    path, out = sys.argv[1], Path(sys.argv[2])
    jobs = int(os.environ.get("OPENRS_JOBS", os.cpu_count() or 2))
    key = stamp(path)
    prev = out / "summary.json"
    if prev.exists() and not os.environ.get("OPENRS_FORCE"):
        try:
            if json.loads(prev.read_text())["meta"].get("stamp") == key:
                print(f"cached {prev}")
                return
        except Exception:
            pass
    meta, draws, clears, shaders, targets = dump(path, out, jobs)
    meta["stamp"], meta["capture"] = key, str(path)
    write(out, meta, draws, clears, shaders, targets)
    print(f"summary {out / 'summary.json'}: {meta['api']} draws={meta['draws']} clears={meta['clears']} targets={len(targets)} shaders={len(shaders)} replay={meta['seconds']['replay']}s export={meta['seconds']['export']}s")


if __name__ == "__main__":
    main()
