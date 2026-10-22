import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import struct
import subprocess


JAR_SHA256 = '78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048'
FIXTURES = (
    'inverse-power-boundaries.native.bin',
    'inverse-power.native-cases.tsv',
    'vm-inverse-power.bin',
    'vm-inverse-power.decode.tsv',
    'vm-inverse-power.vm.tsv',
    'vm-inverse-power-child.bin',
    'vm-inverse-power-child.decode.tsv',
)
TABLE = 'inverse_power_round_down.rs'
OUTPUTS = (*FIXTURES, TABLE)


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def inspect(output):
    payload = (output / FIXTURES[0]).read_bytes()
    magic, count = struct.unpack('>ii', payload[:8])
    if magic != 0x727c5215 or len(payload) != 8 + count * 24:
        raise SystemExit('Invalid inverse-power boundary encoding')
    rows = list(struct.iter_unpack('>iiiiii', payload[8:]))
    expected = []
    for root in range(2, 31):
        value = 2
        while value ** root <= 2147483647:
            expected.append((root, value, value ** root))
            value += 1
    if [row[:3] for row in rows] != expected:
        raise SystemExit('Native boundary inputs do not exhaust every positive int perfect-power threshold')
    corrections = []
    for root, result, base, lower, exact, upper in rows:
        if lower != result - 1 or upper != result or exact not in (result, result - 1):
            raise SystemExit('Unexpected native inverse-power threshold outcome')
        if exact == result - 1:
            corrections.append((root, base))
    table = '&[\n' + ''.join(f'    ({root}, {base}),\n' for root, base in corrections) + ']\n'
    if (output / TABLE).read_text() != table:
        raise SystemExit('Generated runtime correction table does not exactly match actual native outputs')
    cases = Counter()
    bytes_seen = set()
    names = set()
    for line in (output / FIXTURES[1]).read_text().splitlines():
        columns = line.split('\t')
        if len(columns) != 6:
            raise SystemExit('Inverse-power cases must have six TSV columns')
        name, operand, initial, result, pointer, final = columns
        if name in names or not 0 <= int(operand) <= 255:
            raise SystemExit('Duplicate case or invalid byte operand')
        names.add(name)
        values = [] if initial == '-' else initial.split(',')
        last = [] if final == '-' else final.split(',')
        if len(values) < 2:
            if int(pointer) != len(values) - 2 or final != '-' or result != f'error:ArrayIndexOutOfBoundsException:Index {pointer} out of bounds for length 1000':
                raise SystemExit('Native inverse-power underflow result differs')
            cases['underflow'] += 1
        else:
            if result != 'ok' or int(pointer) != len(values) - 1 or last[:-1] != values[:-2]:
                raise SystemExit('Native inverse-power stack replacement differs')
        if name.startswith('byte-'):
            bytes_seen.add(int(operand))
        if name.startswith('full-1000-'):
            if len(values) != 1000:
                raise SystemExit('Full-stack inverse-power case is not full')
            cases['full_1000'] += 1
        for prefix in ('random-wide-', 'random-positive-', 'edge-', 'bit-'):
            if name.startswith(prefix):
                cases[prefix.rstrip('-').replace('-', '_')] += 1
    if bytes_seen != set(range(256)) or cases['underflow'] != 4 or cases['full_1000'] != 32:
        raise SystemExit('Incomplete operand, underflow or capacity coverage')
    if cases['random_wide'] != 4096 or cases['random_positive'] != 4096:
        raise SystemExit('Incomplete deterministic random corpus')
    decoded = {}
    for stem in ('vm-inverse-power', 'vm-inverse-power-child'):
        records = [line.split('\t') for line in (output / (stem + '.decode.tsv')).read_text().splitlines()]
        operations = [row for row in records if len(row) == 4 and row[1] == 'INVPOW']
        decoded[stem] = {
            'instructions': sum(row[0].isdigit() for row in records),
            'counts': [int(value) for value in records[1][1:]],
            'inverse_power_instructions': len(operations),
            'byte_operands': sorted({int(row[3]) for row in operations}),
        }
    if decoded['vm-inverse-power']['byte_operands'] != list(range(256)):
        raise SystemExit('Decoded executor fixture misses byte operands')
    execution = [line.split('\t') for line in (output / 'vm-inverse-power.vm.tsv').read_text().splitlines()]
    if execution[0][0] != 'instructions':
        raise SystemExit('Native executor result missing instruction count')
    return {
        'perfect_power_boundaries': count,
        'boundaries_per_root': {str(key): value for key, value in sorted(Counter(row[0] for row in rows).items())},
        'native_boundary_dispatch_cases': count * 3,
        'round_down_corrections': len(corrections),
        'corrections_per_root': {str(key): value for key, value in sorted(Counter(row[0] for row in corrections).items())},
        'native_direct_cases': len(names),
        'direct_case_coverage': dict(cases),
        'native_dispatch_cases_total': count * 3 + len(names),
        'decoded_scripts': decoded,
        'native_vm_instructions': int(execution[0][1]),
        'native_vm_records': dict(Counter(row[0] for row in execution[1:])),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--work-dir', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    crate = root / 'code/cs2'
    jar = root / 'code/scene/target/client-build/build/libs/darkan-client-shaded-2.0.0-all.jar'
    allowed = (crate / 'target', root / 'code/harness/target/loop/iteration15-validation/cs2')
    work = (args.work_dir or crate / 'target/native-inverse-power').resolve()
    if not any(work.is_relative_to(directory) for directory in allowed):
        raise SystemExit('--work-dir must resolve inside code/cs2/target or iteration15-validation/cs2')
    sources = [
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ClientScriptOpCode.kt',
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Script.kt',
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Executor.kt',
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/CS2CallFrame.kt',
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CS2Dispatch.kt',
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/CoreVmOps.kt',
        'code/client/src/main/kotlin/com/jagex/game/runetek5/config/rsinterface/cs2/ops/MathStringOps.kt',
        'code/cs2/tests/native/NativeFixtures.java',
        'code/cs2/tests/native/InversePowerFixtures.java',
        'code/cs2/tests/native/generate_inverse_power.py',
    ]
    source_hashes = {name: digest(root / name) for name in sources}
    original_before = digest(jar)
    if original_before != JAR_SHA256:
        raise SystemExit('Pre-existing native JAR does not match pinned SHA-256')
    work.mkdir(parents=True, exist_ok=True)
    copied = work / 'native-client.jar'
    classes = work / 'classes'
    modes = {'cold': ([], []), 'warm': ([], ['warm']), 'interpreted': (['-Xint'], []), 'compiled': (['-Xcomp'], ['warm'])}
    for path in (copied, classes, *(work / mode for mode in modes)):
        if not any(path.resolve().is_relative_to(directory) for directory in allowed):
            raise SystemExit('Oracle output resolves outside allowed work directories')
    classes.mkdir(exist_ok=True)
    shutil.copyfile(jar, copied)
    copied_before = digest(copied)
    if copied_before != JAR_SHA256:
        raise SystemExit('Isolated JAR copy differs')
    options = ['-Djava.awt.headless=true', '-XX:-OmitStackTraceInFastThrow']
    mode_hashes = {}
    try:
        subprocess.run(['javac', '-cp', str(copied), '-d', str(classes), *(str(root / name) for name in sources if name.endswith('.java'))], cwd=work, check=True)
        for mode, (vm_options, driver_options) in modes.items():
            output = work / mode
            result = subprocess.run(['java', *options, *vm_options, '-cp', os.pathsep.join([str(classes), str(copied)]), 'InversePowerFixtures', str(output), *driver_options], cwd=work, capture_output=True, text=True, check=True)
            (work / (mode + '.log')).write_text(result.stdout + result.stderr)
            print(mode + ': ' + result.stdout.strip().replace('\n', '; '))
            if {path.name for path in output.iterdir()} != set(OUTPUTS):
                raise SystemExit('Unexpected oracle output files')
            mode_hashes[mode] = {name: digest(output / name) for name in sorted(OUTPUTS)}
    finally:
        original_after = digest(jar)
        copied_after = digest(copied)
        if original_after != original_before or copied_after != copied_before:
            raise SystemExit('Native JAR changed during inverse-power generation')
    if any(hashes != mode_hashes['cold'] for hashes in mode_hashes.values()):
        raise SystemExit('Native INVPOW differs between cold, warmed JIT, interpreted and forced-compiled execution')
    if {name: digest(root / name) for name in sources} != source_hashes:
        raise SystemExit('Native oracle sources changed during generation')
    output = work / 'cold'
    statistics = inspect(output)
    java = subprocess.run(['java', '-version'], cwd=work, capture_output=True, text=True, check=True)
    manifest = {
        'oracle': 'Actual MathStringOps.dispatch(INVPOW), native CS2Script.decodeScript and CS2Executor.executeScript via unchanged NativeFixtures helpers from an isolated copy of the pinned JAR. No Java/Python floating power expression is substituted for the native opcode.',
        'java': java.stderr.splitlines()[0],
        'architecture': platform.machine(),
        'java_options': options,
        'execution_modes': {'cold': [], 'warm': ['200000 native dispatch warmup calls'], 'interpreted': ['-Xint'], 'compiled': ['-Xcomp', '200000 native dispatch warmup calls']},
        'all_execution_modes_byte_identical': True,
        'mode_fixture_sha256': mode_hashes,
        'jar': str(jar.relative_to(root)),
        'jar_sha256': JAR_SHA256,
        'jar_sha256_before': original_before,
        'jar_sha256_after': original_after,
        'copied_jar_sha256_before': copied_before,
        'copied_jar_sha256_after': copied_after,
        'source_sha256': source_hashes,
        'source_provenance': 'Current relevant source hashes identify provenance only; the executable oracle is the pinned pre-existing JAR, not a source rebuild.',
        'fixture_sha256': {name: digest(output / name) for name in sorted(FIXTURES)},
        'generated_table': {'path': 'code/cs2/src/' + TABLE, 'sha256': digest(output / TABLE)},
        'opcode': {'name': 'INVPOW', 'ordinal': 547, 'wire': 776, 'family': 10, 'has_int_operand': False},
        **statistics,
        'random_seed': '0x727c5215 (java.util.Random)',
        'boundary_encoding': 'Big-endian signed 32-bit magic 0x727c5215 and row count, then six i32 per row: root, exact integer root k, base k^root, native(base-1), native(base), native(base+1). All k>=2 and roots 2..30 with k^root<=i32::MAX are present, ordered by root then k.',
        'direct_encoding': 'Six TSV columns without header: case name, unsigned byte operand, initial int stack, native result (ok or error:exception-simple-class:exact-message), native signed stack pointer, final visible int stack. Comma-separated signed decimals; dash is empty or negative-pointer stack.',
        'table_encoding': 'Generated Rust array expression of (root, base) pairs whose actual native perfect-power result was k-1, rather than k. Every table pair is independently present in the boundary oracle. No handwritten exceptions.',
        'domain_argument': [
            'Native checks base==0 before root==0. Root 1 is exact identity, including negative inputs. Base 1 with nonzero root is 1. Negative base with reciprocal exponent non-integral gives NaN then integer 0; root -1 gives -1 only for base -1, otherwise truncates to 0.',
            'Positive base>=2 and negative root gives a value strictly between 0 and 1, at least approximately ln(2)/2^31 from 1. Positive root>=31 gives a value strictly between 1 and 2; the closest upper case is i32::MAX with root31, about 3e-11 below 2. These margins exceed reciprocal and Math.pow rounding error.',
            'For positive roots 2..30, exact integer binary search determines floor root. Non-perfect integer bases differ by at least one from an integer power; their root-relative distance from an integer threshold exceeds 1/(30*2^31) > 1.5e-11. The reciprocal exponent relative error is <=2^-53; resulting relative error is bounded by ln(base)*2^-53/root < 1.2e-15, plus Math.pow one ulp <2.3e-16. Thus only exact perfect powers can cross an integer truncation boundary.',
            'All possible exact perfect powers are exhaustively enumerated as integer inputs and executed by the native opcode, including roots 2,4,8,16. Their observed results are k or k-1; the generated correction table preserves every k-1 result. There is no runtime floating point, host libm or approximate root.',
        ],
        'coverage': [
            'All perfect-power thresholds and their immediate neighbors for roots2..30 across the complete positive int range; all native corrections generated from those outcomes.',
            'Signed extrema, 65 selected bases crossed with 54 selected roots, one-hot/complement/neighbor values at every bit crossed with those roots, 4096 full-width and 4096 positive-root random vectors.',
            'All byte operands0..255 in direct cases and a native decoded root script, all short-stack shapes with bytes0/255, 32 full1000-input stacks, non-int stacks/locals/operand/instruction sentinels.',
            'Decoded parent and parameterized child900776 preserve caller locals, lower typed stacks, return ordering and exact native instruction count through eleven mixed-domain calls.',
        ],
        'limitations': [
            'Math.pow is not StrictMath. This table freezes the pinned native JAR on the recorded JDK/architecture; a different JVM or CPU may legally choose another last bit. Cold, warmed-JIT, interpreter and forced-compilation outputs are required to agree for this pinned oracle.',
            'The boundary corpus exhausts every possible positive integer truncation threshold for roots2..30, not all 2^64 input pairs. Interior input equivalence uses the recorded error-bound argument and deterministic supplementary vectors.',
            'Rust underflow retains available operands and reports typed StackUnderflow(Int); the native negative stack pointer and exception are captured as evidence, not adopted as an unsafe state contract.',
            'No client/server/lobby services, accounts, network, cache, scene, rendering or runtime/browser integration is exercised. Only isolated fixture subprocesses run. Original JAR, enum, executor and pre-existing fixtures are unchanged.',
        ],
        'reproduction': 'python3 code/cs2/tests/native/generate_inverse_power.py --check',
        'generation': 'python3 code/cs2/tests/native/generate_inverse_power.py',
    }
    destinations = {name: crate / 'tests/fixtures' / name for name in FIXTURES}
    destinations[TABLE] = crate / 'src' / TABLE
    manifest_path = crate / 'tests/fixtures/inverse-power-oracle.json'
    if args.check:
        for name, destination in destinations.items():
            if not destination.is_file() or destination.read_bytes() != (output / name).read_bytes():
                raise SystemExit('Native inverse-power output differs: ' + name)
        if not manifest_path.is_file() or json.loads(manifest_path.read_text()) != manifest:
            raise SystemExit('Native inverse-power manifest differs')
        print('Verified every fixture and generated table against all four native modes')
    else:
        for name, destination in destinations.items():
            shutil.copyfile(output / name, destination)
        manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
        print('Installed native inverse-power fixtures, generated table and provenance manifest')
    print(json.dumps(statistics, sort_keys=True))


if __name__ == '__main__':
    main()
