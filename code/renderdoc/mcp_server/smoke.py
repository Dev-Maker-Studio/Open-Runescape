"""Smoke test: start the MCP server over stdio and call every tool against a capture.
usage: python smoke.py <capture.rdc>"""
import asyncio, sys, json
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

MESH, UI = 0, 0
CALLS = [
    ("rdc_overview", {"action": "info"}), ("rdc_overview", {"action": "passes"}), ("rdc_overview", {"action": "stats"}),
    ("rdc_overview", {"action": "count", "flags": "draws"}),
    ("rdc_draws", {}), ("rdc_draws", {"eid": "MESH"}), ("rdc_events", {"flags": "--limit 3"}),
    ("rdc_pipeline", {"eid": "MESH", "section": "rasterizer"}), ("rdc_pipeline", {"eid": "MESH", "section": "depth-stencil"}),
    ("rdc_pipeline", {"eid": "MESH", "action": "bindings"}),
    ("rdc_shader", {}), ("rdc_shader", {"eid": "MESH", "stage": "ps"}), ("rdc_resources", {}),
    ("rdc_export", {"action": "rt", "eid": "UI", "output": "/tmp/mcp_rt.png"}),
    ("rdc_export", {"action": "mesh", "eid": "MESH", "output": "/tmp/mcp_mesh.obj"}),
    ("rdc_pixel", {"action": "pixel", "eid": "UI", "args": "160 120"}),
    ("rdc_vfs", {"action": "ls", "path": "/"}), ("rdc_command", {"command": "counters", "flags": "--eid 43"}),
]

async def main(cap):
    p = StdioServerParameters(command=sys.executable, args=[__file__.replace("smoke.py", "server.py")])
    async with stdio_client(p) as (r, w), ClientSession(r, w) as s:
        await s.initialize()
        tools = [t.name for t in (await s.list_tools()).tools]
        print("tools:", tools)
        print("open:", (await s.call_tool("rdc_session", {"action": "open", "capture": cap})).content[0].text[:100])
        global MESH, UI
        draws = (await s.call_tool("rdc_draws", {})).content[0].text
        for row in draws.splitlines():
            c = row.split("\t")
            if len(c) > 5 and c[1] == "DrawIndexed" and not MESH:
                MESH = int(c[0])
            if len(c) > 5 and c[4] == "ui sprites":
                UI = int(c[0])
        print("mesh draw", MESH, "ui draw", UI)
        calls = [(n, {k: (MESH if v == "MESH" else UI if v == "UI" else v) for k, v in a.items()}) for n, a in CALLS]
        bad = 0
        for name, args in calls:
            try:
                res = await s.call_tool(name, args)
                txt = "".join(getattr(c, "text", "[image]") for c in res.content)
                ok = not res.isError and "error" not in txt[:40].lower()
            except Exception as e:
                txt, ok = str(e), False
            bad += not ok
            print("OK  " if ok else "FAIL", name, args.get("action", args.get("command", "")), "|", txt[:90].replace("\n", " "))
        await s.call_tool("rdc_session", {"action": "close"})
    return bad
sys.exit(1 if asyncio.run(main(sys.argv[1])) else 0)
