import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


JAR_SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048"
FIXTURES = (
    "varc.native.tsv",
    "vm-varcs.bin",
    "vm-varcs.decode.tsv",
    "vm-varcs.vm.tsv",
    "vm-varcs-child.bin",
    "vm-varcs-child.decode.tsv",
    "vm-varcs.state.tsv",
)
OPERATIONS = {
    "PUSH_VARC_INT": {"ordinal": 26, "wire": 311, "family": 0, "has_int_operand": True},
    "POP_VARC_INT": {"ordinal": 27, "wire": 510, "family": 0, "has_int_operand": True},
    "PUSH_VARC_STRING": {"ordinal": 31, "wire": 700, "family": 0, "has_int_operand": True},
    "POP_VARC_STRING": {"ordinal": 32, "wire": 21, "family": 0, "has_int_operand": True},
}
METADATA = ("opcodes.tsv", "all-opcodes.bin", "all-opcodes.decode.tsv", "native-oracle.json")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def values(encoded):
    return [] if encoded == "-" else encoded.split(",")


def events(encoded):
    result = [tuple(map(int, value.split(":"))) for value in values(encoded)]
    if any(len(value) != 4 or value[0] < 1 or value[1] not in (1, 2) or value[2] < 0 or value[3] != 500 for value in result):
        raise SystemExit("Invalid normalized native event record")
    if len(set(result)) != len(result) or len({value[0] for value in result}) != len(result):
        raise SystemExit("Duplicate event identity in native iteration")
    return result


def inspect(output):
    counts = Counter()
    error_counts = Counter()
    cases = set()
    names = set()
    identities = {}
    sequences = Counter()
    for line in (output / "varc.native.tsv").read_text().splitlines():
        row = line.split("\t")
        if len(row) != 25:
            raise SystemExit("VARC native fixture does not have 25 columns")
        name, index, opcode, operand = row[:4]
        if opcode not in OPERATIONS or (name, index) in names or int(index) != sequences[name]:
            raise SystemExit("Invalid VARC operation or case sequence")
        names.add((name, index))
        cases.add(name)
        sequences[name] += 1
        counts[opcode] += 1
        int(operand)
        if row[10] not in ("0", "1") or row[22] not in ("0", "1") or any(value not in ("0", "1") for value in values(row[9])):
            raise SystemExit("Invalid native persistence flags or dirty record")
        for pointer, stack in ((14, 15), (16, 17), (18, 19)):
            if len(values(row[stack])) != max(0, min(1000, int(row[pointer]))):
                raise SystemExit("Native pointer and clipped backing-stack snapshot differ")
        if row[13] != "ok":
            if not row[13].startswith("error:ArrayIndexOutOfBoundsException:"):
                raise SystemExit("Unexpected native VARC exception")
            error_counts[opcode] += 1
        if name.startswith("underflow-") and (int(row[14]) if opcode == "POP_VARC_INT" else int(row[16])) != -1:
            raise SystemExit("Native underflow pointer was not captured before slicing")
        for cache_column, low_column in ((11, 12), (23, 24)):
            cache, low = events(row[cache_column]), events(row[low_column])
            if set(cache) != set(low):
                raise SystemExit("Native event cache and low-priority membership differ")
            for identity, kind, event_id, delay in low:
                key = (name, identity)
                if key in identities and identities[key] != (kind, event_id):
                    raise SystemExit("Native event identity changed type or id")
                identities[key] = (kind, event_id)
    if any(counts[opcode] == 0 or error_counts[opcode] == 0 for opcode in OPERATIONS):
        raise SystemExit("VARC operation success/failure coverage is incomplete")
    if not all(name in cases for name in ("coalesce-dirty-false", "wide-id-257", "independent-lengths", "short-flags-dirty-false")):
        raise SystemExit("VARC sequence coverage is incomplete")
    decoded = {}
    for stem in ("vm-varcs", "vm-varcs-child"):
        rows = [line.split("\t") for line in (output / (stem + ".decode.tsv")).read_text().splitlines()]
        varcs = [row for row in rows if len(row) == 4 and row[1] in OPERATIONS]
        if any(row[2] != "int" for row in varcs):
            raise SystemExit("Native VARC decoder lost the wide integer operand")
        decoded[stem] = {
            "instructions": sum(row[0].isdigit() for row in rows),
            "counts": list(map(int, rows[1][1:])),
            "varc_instructions": dict(Counter(row[1] for row in varcs)),
            "varc_ids": sorted({int(row[3]) for row in varcs}),
        }
    if 257 not in decoded["vm-varcs"]["varc_ids"]:
        raise SystemExit("Native decoded root does not contain a wide id")
    vm = [line.split("\t") for line in (output / "vm-varcs.vm.tsv").read_text().splitlines()]
    state = [line.split("\t") for line in (output / "vm-varcs.state.tsv").read_text().splitlines()]
    expected_keys = {"ints", "strings", "persistent", "dirty", "cache", "low-priority"}
    for phase in ("before", "after"):
        records = {row[1]: row[2] for row in state if len(row) == 3 and row[0] == phase}
        if set(records) != expected_keys:
            raise SystemExit("Incomplete native VM VARC state records")
        if set(events(records["cache"])) != set(events(records["low-priority"])):
            raise SystemExit("Native VM event membership differs")
    if len(state) != 12 or vm[0][0] != "instructions":
        raise SystemExit("Invalid native VM execution evidence")
    return {
        "native_cases": len(cases),
        "native_steps": sum(counts.values()),
        "steps_per_operation": dict(counts),
        "native_errors_per_operation": dict(error_counts),
        "multi_step_cases": {name: count for name, count in sequences.items() if count > 1},
        "decoded_scripts": decoded,
        "native_vm_instructions": int(vm[0][1]),
        "native_vm_records": dict(Counter(row[0] for row in vm[1:])),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / "code/cs2"
    destination = crate / "tests/fixtures"
    jar = root / "code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar"
    permitted = (root / "code/harness/target/loop/iteration16-validation/varcs/native").resolve()
    work = (args.work_dir or permitted).resolve()
    if work != permitted:
        raise SystemExit("--work-dir must be code/harness/target/loop/iteration16-validation/varcs/native")
    source_paths = [
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Script.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2CallFrame.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CS2Dispatch.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CoreVmOps.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/config/varc/VarcTypeList.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/Client.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/periodic/PulseEvent.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/periodic/PulseEventQueue.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/periodic/PulseEventHandler.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/client/periodic/PulseEventType.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/cache/node/IterableNodeMap.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/cache/node/HashTableIterator.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/cache/node/Queue.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/cache/node/QueueIterator.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/cache/node/lru/CacheableNode.kt",
        "code/client/src/main/kotlin/com/jagex/game/runetek5/utils/Utils.kt",
        "code/cs2/tests/native/NativeFixtures.java",
        "code/cs2/tests/native/VarcFixtures.java",
        "code/cs2/tests/native/generate_varcs.py",
    ]
    source_hashes = {name: digest(root / name) for name in source_paths}
    protected = {path.name: digest(path) for path in destination.iterdir() if path.is_file() and path.name not in (*FIXTURES, "varc-oracle.json")}
    metadata_hashes = {name: digest(destination / name) for name in METADATA}
    original_before = digest(jar)
    if original_before != JAR_SHA256:
        raise SystemExit("Pre-existing native jar does not match the pinned SHA-256; refusing to repin")
    work.mkdir(parents=True, exist_ok=True)
    copied = work / "native-client.jar"
    classes = work / "classes"
    output = work / "fixtures"
    for path in (copied, classes, output):
        if not path.resolve().is_relative_to(permitted):
            raise SystemExit("Native VARC work paths must not resolve outside the private evidence directory")
    classes.mkdir(exist_ok=True)
    output.mkdir(exist_ok=True)
    shutil.copyfile(jar, copied)
    copied_before = digest(copied)
    if copied_before != JAR_SHA256:
        raise SystemExit("Private native jar copy differs from the pinned SHA-256")
    try:
        subprocess.run([
            "javac", "-cp", str(copied), "-d", str(classes),
            str(crate / "tests/native/NativeFixtures.java"),
            str(crate / "tests/native/VarcFixtures.java"),
        ], cwd=work, check=True)
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow",
            "-cp", os.pathsep.join([str(classes), str(copied)]), "VarcFixtures", str(output),
        ], cwd=work, check=True)
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit("Native original or private copied jar changed during VARC generation")
        if {name: digest(root / name) for name in source_paths} != source_hashes:
            raise SystemExit("Native VARC oracle sources changed during generation")
        if any(not (destination / name).is_file() or digest(destination / name) != value for name, value in protected.items()):
            raise SystemExit("Pre-existing fixtures or metadata changed during VARC generation")
    if {path.name for path in output.iterdir()} != set(FIXTURES):
        raise SystemExit("Unexpected native VARC output files")
    statistics = inspect(output)
    java = subprocess.run(["java", "-version"], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        "oracle": "Actual CoreVmOps.dispatch for four VARC operations, CS2Script.decodeScript and CS2Executor.executeScript through unchanged NativeFixtures.Program/fixture/execution. Executed only against a private copy of the pre-existing pinned JAR; no VARC expressions or pulse scheduling are reimplemented as an oracle.",
        "java": java.stderr.splitlines()[0],
        "java_options": ["-Djava.awt.headless=true", "-XX:-OmitStackTraceInFastThrow"],
        "jar": str(jar.relative_to(root)),
        "jar_sha256": JAR_SHA256,
        "jar_sha256_before": original_before,
        "jar_sha256_after": original_after,
        "copied_jar_sha256_before": copied_before,
        "copied_jar_sha256_after": copied_after,
        "source_sha256": source_hashes,
        "existing_metadata_sha256": metadata_hashes,
        "source_provenance": "Current source hashes identify the relevant Kotlin sources and unchanged shared Java helper; the executable oracle is the pre-existing pinned JAR, not a source rebuild. Sources and all pre-existing fixture files are hashed before/after compilation and execution.",
        "generator_sha256": digest(Path(__file__).resolve()),
        "driver_sha256": digest(crate / "tests/native/VarcFixtures.java"),
        "fixture_sha256": {name: digest(output / name) for name in sorted(FIXTURES)},
        "opcodes": OPERATIONS,
        **statistics,
        "random_seed": "0x727c5210 (java.util.Random); 64 cases per operation",
        "native_tsv_columns": [
            "case", "step", "opcode", "id", "before_int_stack", "before_string_stack", "before_long_stack",
            "before_varc_ints", "before_varc_strings", "persistent_flags", "before_dirty", "before_cache", "before_low_priority",
            "native_result", "native_int_pointer", "after_int_stack", "native_string_pointer", "after_string_stack",
            "native_long_pointer", "after_long_stack", "after_varc_ints", "after_varc_strings", "after_dirty", "after_cache", "after_low_priority",
        ],
        "native_tsv_encoding": "No header. Comma-separated arrays/stacks, dash for empty; signed decimal integers/longs; strings null or u followed by four lowercase hex digits per UTF-16 code unit (empty string u); flags/dirty 0 or 1. Each step resets input stacks but retains state and pulse identities within a case. Native result ok or error:exception-simple-class:exact-message (dash for null message). Stack snapshots clip native pointers to 0..1000 but retain exact signed error pointers in separate columns.",
        "event_encoding": "Comma-separated identity:type:id:500, dash for empty. Identity is assigned from actual LOW_PRIORITY native iteration on first observation, stable per case, and shared by cache/queue records. Cache and LOW_PRIORITY preserve their actual native iterator order; HIGH_PRIORITY is asserted empty. Types 1/2 are CLIENT_SETVARC/CLIENT_SETVARCSTR. Event payload fields remain native zero/null. No wallclock timestamps are written.",
        "deadline_assertion": "Each direct dispatch is bracketed by Utils.time(); a new/requeued store event must have executeAfter in [before+500,after+500]. Other existing event deadlines must remain exactly unchanged. Whole decoded root/child execution is bracketed similarly and every final event must lie in that 500ms scheduling interval. The normalized 500 is target delay at the most recent store, not remaining delay at snapshot time.",
        "oracle_exit": "Client static initialization creates idle non-daemon connection pingers even in a headless test. The private Java oracle exits with status 0/1 on completion/failure; it never calls Client.shutdown, writes preferences or controls any other process.",
        "decode_tsv_encoding": "Unchanged NativeFixtures.fixture output: name and six local/argument counts, then instruction index, opcode, operand kind and decoded value. .bin uses the native four-byte integer operand for all four VARC operations, including id 257.",
        "vm_tsv_encoding": "Unchanged NativeFixtures.execution output: instruction count, typed final stacks/root locals and five global-array records. Strings here use UTF-8 hex: or null, unlike direct TSV UTF-16 strings.",
        "vm_state_tsv_encoding": "Twelve records phase<TAB>key<TAB>value with phases before/after and keys ints, strings, persistent, dirty, cache, low-priority; values use the direct TSV encodings. Root/child script names vm-varcs and vm-varcs-child; child script pointer 900026. Arrays contain 258 entries; int persistent flags true at ids 1 and 257, all others false.",
        "coverage": [
            "All eight ordinary ids with integer -1 and nullable-string defaults; signed integer extrema and neighboring/zero values; null, empty, non-BMP, embedded NUL/tab/newline, markup and non-ASCII strings.",
            "Reads, changed stores and same-value stores crossed with first/interior/last ids and both persistence flags; every case starts clean with empty queues, while mixed sequences verify sticky dirty state after persistent then nonpersistent stores. Includes 64 deterministic random cases per operation.",
            "Mixed-type/mixed-id sequences capture native cache iteration, low-priority requeue order, stable node identity and same-value coalescing; int/string id collisions remain distinct events.",
            "Negative/extreme/out-of-range ids, underflow and full 1000-stack overflow retain native exact exception messages and pointer mutations; independent int/string lengths and short persistence arrays capture partial native mutations.",
            "Wide id 257 direct and decoded operations; integer/string/long sentinels and locals remain unchanged on unrelated stack paths. Root with parameterized child calls stores global state, returns typed values, and stores null from uninitialized string locals.",
        ],
        "limitations": [
            "Native error records are factual observations, not a demand that bounded Rust reproduce unsafe partial writes, negative/overflow pointers or late flag failures. Rust rejection should be transactional.",
            "No pulse consumer, preference persistence, hook execution, game/server/lobby entry point, supplied cache, account, credentials, network or display is invoked. Deferred events describe scheduling intent, not synthetic future overwrites.",
            "Only the four named family-0 operations are covered; existing opcode metadata, all-opcode fixtures, native helper and every pre-existing fixture remain unchanged.",
            "The executable JAR and full Java exception messages are pinned. No Gradle, rebuild, JAR repin or live JAR modification occurs.",
        ],
        "reproduction": "python3 code/cs2/tests/native/generate_varcs.py --check",
        "generation": "python3 code/cs2/tests/native/generate_varcs.py",
        "work_directory": str(permitted.relative_to(root)),
    }
    manifest_path = destination / "varc-oracle.json"
    if args.check:
        for name in FIXTURES:
            if not (destination / name).is_file() or (output / name).read_bytes() != (destination / name).read_bytes():
                raise SystemExit(f"Native VARC fixture differs: {name}")
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit("Native VARC manifest differs")
        print(f"Verified {len(FIXTURES)} native VARC fixtures, {statistics['native_cases']} cases, {statistics['native_steps']} steps, unchanged source/metadata and original/private JAR hashes")
    else:
        for name in FIXTURES:
            shutil.copyfile(output / name, destination / name)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Installed {len(FIXTURES)} native VARC fixtures and provenance manifest")
    print(json.dumps(statistics, sort_keys=True))


if __name__ == "__main__":
    main()
