import json
import os
import shutil
import sys
import time
from pathlib import Path

sys.path.insert(0, os.environ["RENDERDOC_PYTHON_PATH"])
import renderdoc as rd


def ancestors(pid):
    out = []
    while pid > 1:
        out.append(pid)
        try:
            pid = int(next(l for l in Path(f"/proc/{pid}/status").read_text().splitlines() if l.startswith("PPid:")).split()[1])
        except (OSError, StopIteration):
            break
    return out


def find(root, wait):
    deadline = time.monotonic() + wait
    while time.monotonic() < deadline:
        ident = rd.EnumerateRemoteTargets("localhost", 0)
        while ident:
            tc = rd.CreateTargetControl("localhost", ident, "openrs", True)
            if tc and tc.Connected():
                if root in ancestors(tc.GetPID()):
                    return ident, tc
                tc.Shutdown()
            ident = rd.EnumerateRemoteTargets("localhost", ident)
        time.sleep(0.25)
    raise SystemExit(f"no RenderDoc target under pid {root} after {wait}s")


def main():
    root, out, wait, timeout = int(sys.argv[1]), sys.argv[2], float(sys.argv[3]), float(sys.argv[4])
    ident, tc = find(root, wait)
    pid = tc.GetPID()
    print(f"ready {ident} {pid}", flush=True)
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline and tc.Connected():
        m = tc.ReceiveMessage(None)
        if m.type == rd.TargetControlMessageType.NewCapture:
            nc = m.newCapture
            shutil.move(nc.path, out) if nc.local else tc.CopyCapture(nc.captureId, out)
            print(json.dumps({"success": True, "ident": ident, "pid": pid, "frame": nc.frameNumber, "path": out}), flush=True)
            tc.Shutdown()
            return
        time.sleep(0.01)
    tc.Shutdown()
    raise SystemExit(f"no capture from ident {ident} pid {pid} within {timeout}s")


if __name__ == "__main__":
    main()
