import hashlib
import json
from pathlib import Path
import subprocess
import sys

assets = Path(__file__).resolve().parents[1]
jar = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else assets.parent / "client/build/libs/darkan-client-shaded-2.0.0-all.jar"
classes = assets / "target/native-oracle"
fixtures = assets / "tests/fixtures"
classes.mkdir(parents=True, exist_ok=True)
subprocess.run(["javac", "-cp", str(jar), "-d", str(classes), str(assets / "oracle/AssetsOracle.java")], check=True)
subprocess.run(["java", "-Xmx256m", "-cp", f"{jar}:{classes}", "AssetsOracle", str(fixtures)], check=True)
provenance = {
    "source": "Synthetic bytes encoded by AssetsOracle.java; no cache, server, account or client launch",
    "native_jar": jar.name,
    "native_jar_sha256": hashlib.file_digest(jar.open("rb"), "sha256").hexdigest(),
    "container_encoding": "Java DataOutputStream and GZIPOutputStream; bzip2 -1c with BZh1 removed",
    "native_decoding": ["Js5Compression.decompress", "Js5MasterIndex", "Js5ResourceProvider.getFile", "Whirlpool.calculateDigest"],
    "fixture_sha256": {file.name: hashlib.sha256(file.read_bytes()).hexdigest() for file in sorted(fixtures.iterdir()) if file.name != "provenance.json" and file.is_file()},
}
(fixtures / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
