use super::*;
use whirlpool::{Digest, Whirlpool};

fn raw(data: &[u8]) -> Vec<u8> {
    [
        vec![0],
        (data.len() as u32).to_be_bytes().to_vec(),
        data.to_vec(),
    ]
    .concat()
}

fn reply(index: u8, id: u32, container: &[u8]) -> Vec<u8> {
    let mut result = vec![index];
    result.extend_from_slice(&id.to_be_bytes());
    result.push(container[0]);
    result.extend_from_slice(&container[1..5]);
    let mut at = 10;
    for &byte in &container[5..] {
        if at == 512 {
            result.push(255);
            at = 1;
        }
        result.push(byte);
        at += 1;
    }
    result
}

fn smart(out: &mut Vec<u8>, value: u32) {
    if value >= 32768 {
        out.extend_from_slice(&(value | 0x80000000).to_be_bytes());
    } else {
        out.extend_from_slice(&(value as u16).to_be_bytes());
    }
}

fn script(ops: &[(Opcode, i32)]) -> Vec<u8> {
    let mut out = vec![0];
    for (op, value) in ops {
        out.extend_from_slice(&op.wire().to_be_bytes());
        if op.metadata().has_int_constant {
            out.extend_from_slice(&value.to_be_bytes());
        } else {
            out.push(*value as u8);
        }
    }
    out.extend_from_slice(&(ops.len() as u32).to_be_bytes());
    out.extend_from_slice(&[0; 12]);
    out.extend_from_slice(&[0, 0, 1]);
    out
}

struct Chain {
    version: Vec<u8>,
    reference: Vec<u8>,
    archives: BTreeMap<u32, Vec<u8>>,
}
impl Chain {
    fn new(scripts: &[(u32, Vec<u8>)]) -> Self {
        let archives: BTreeMap<_, _> = scripts
            .iter()
            .map(|(id, bytes)| (*id, raw(bytes)))
            .collect();
        Self::with_archives(
            archives
                .into_iter()
                .map(|(id, bytes)| (id, (bytes, 1)))
                .collect(),
        )
    }
    fn with_archives(entries: BTreeMap<u32, (Vec<u8>, u32)>) -> Self {
        let archives: BTreeMap<_, _> = entries
            .iter()
            .map(|(&id, (bytes, _))| (id, bytes.clone()))
            .collect();
        let mut table = vec![7];
        table.extend_from_slice(&7i32.to_be_bytes());
        table.push(2);
        smart(&mut table, archives.len() as u32);
        let mut prior = 0;
        for &id in archives.keys() {
            smart(&mut table, id - prior);
            prior = id;
        }
        for bytes in archives.values() {
            table.extend_from_slice(&crc32fast::hash(bytes).to_be_bytes());
        }
        for bytes in archives.values() {
            table.extend_from_slice(&Whirlpool::digest(bytes));
        }
        for _ in archives.keys() {
            table.extend_from_slice(&44i32.to_be_bytes());
        }
        for (_, count) in entries.values() {
            smart(&mut table, *count);
        }
        for (_, count) in entries.values() {
            for id in 0..*count {
                smart(&mut table, u32::from(id != 0));
            }
        }
        let reference = raw(&table);
        let mut versions = vec![13];
        versions.extend_from_slice(&[0; 12 * 72]);
        versions.extend_from_slice(&crc32fast::hash(&reference).to_be_bytes());
        versions.extend_from_slice(&9001i32.to_be_bytes());
        versions.extend_from_slice(&Whirlpool::digest(&reference));
        versions.extend_from_slice(&[0x5a; 128]);
        Self {
            version: raw(&versions),
            reference,
            archives,
        }
    }
    fn begin(&self, root: u32) -> ScriptGraphProbe {
        let mut probe = ScriptGraphProbe::new(b"test", root).unwrap();
        probe.take_outgoing();
        probe.feed(&[0; 109]).unwrap();
        assert_eq!(probe.take_outgoing().len(), 18);
        probe.feed(&reply(255, 255, &self.version)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(255, 12, js5::Priority::High)
        );
        probe.feed(&reply(255, 12, &self.reference)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(12, root, js5::Priority::High)
        );
        probe
    }
}

#[test]
fn verified_native_root_and_child_execute_with_shared_stacks_and_arrays() {
    let root = include_bytes!("../../../cs2/tests/fixtures/vm-root.bin").to_vec();
    let child = include_bytes!("../../../cs2/tests/fixtures/vm-child.bin").to_vec();
    let chain = Chain::new(&[(900001, root.clone()), (900002, child.clone())]);
    let wire = [
        vec![0; 109],
        reply(255, 255, &chain.version),
        reply(255, 12, &chain.reference),
        reply(12, 900001, &chain.archives[&900001]),
        reply(12, 900002, &chain.archives[&900002]),
    ]
    .concat();
    for size in [1, 2, 7, 109, 511, 512, 513, wire.len()] {
        let mut probe = ScriptGraphProbe::new(b"test", 900001).unwrap();
        probe.take_outgoing();
        for bytes in wire.chunks(size) {
            probe.feed(bytes).unwrap();
        }
        let report = probe.report().unwrap();
        assert!(!report.authenticated);
        assert_eq!(report.scripts.len(), 2);
        assert_eq!(report.scripts[0].calls, [900002]);
        assert_eq!(report.total_bytes, root.len() + child.len());
        assert_eq!(
            report.total_expanded_bytes,
            chain.reference.len() - 5 + root.len() + child.len()
        );
        assert_eq!(probe.file(900001).unwrap(), root);
        assert_eq!(probe.file(900002).unwrap(), child);
        assert!(probe.file(1).is_err());
        let result = probe.run(Arguments::default()).unwrap();
        assert_eq!(result.instructions_executed, 37);
        assert_eq!(result.ints, [70, 11, 22, 81, -1, 71]);
        assert_eq!(
            result.strings,
            [
                Some("lower".into()),
                Some("firstsecond".into()),
                Some("caller".into())
            ]
        );
        assert_eq!(result.longs, ["73", "74", "72"]);
        assert_eq!(result.arrays[4], [-1]);
        assert_eq!(result.local_ints, [71]);
        probe.finish().unwrap();
    }
}

#[test]
fn native_generated_gzip_chain_is_compatible() {
    let mut probe = ScriptGraphProbe::new(b"test", 2163).unwrap();
    probe
        .feed(include_bytes!("../../tests/fixtures/asset-chain.wire"))
        .unwrap();
    assert_eq!(probe.report().unwrap().scripts.len(), 1);
    assert_eq!(
        probe
            .run(Arguments::default())
            .unwrap()
            .instructions_executed,
        158
    );
}

#[test]
fn cyclic_dependencies_fetch_each_script_once_but_execution_stays_bounded() {
    let a = script(&[(Opcode::GOSUB_WITH_PARAMS, 2), (Opcode::RETURN, 0)]);
    let b = script(&[(Opcode::GOSUB_WITH_PARAMS, 1), (Opcode::RETURN, 0)]);
    let chain = Chain::new(&[(1, a), (2, b)]);
    let mut probe = chain.begin(1);
    probe.feed(&reply(12, 1, &chain.archives[&1])).unwrap();
    assert_eq!(
        probe.take_outgoing(),
        js5::encode_request(12, 2, js5::Priority::High)
    );
    probe.feed(&reply(12, 2, &chain.archives[&2])).unwrap();
    assert!(probe.complete());
    assert!(probe.take_outgoing().is_empty());
    assert!(probe.run(Arguments::default()).is_err());
    assert!(probe.complete());
}

#[test]
fn missing_or_corrupt_dependencies_invalidate_all_partial_state() {
    let root = script(&[(Opcode::GOSUB_WITH_PARAMS, 2), (Opcode::RETURN, 0)]);
    for corrupt in [false, true] {
        let child = script(&[(Opcode::RETURN, 0)]);
        let chain = if corrupt {
            Chain::new(&[(1, root.clone()), (2, child)])
        } else {
            Chain::new(&[(1, root.clone())])
        };
        let mut probe = chain.begin(1);
        let first = probe.feed(&reply(12, 1, &chain.archives[&1]));
        if corrupt {
            first.unwrap();
            let mut damaged = chain.archives[&2].clone();
            *damaged.last_mut().unwrap() ^= 1;
            assert!(
                probe
                    .feed(&reply(12, 2, &damaged))
                    .unwrap_err()
                    .contains("CRC")
            );
        } else {
            assert!(first.unwrap_err().contains("absent"));
        }
        assert!(!probe.complete());
        assert!(probe.report().is_none());
        assert!(probe.file(1).is_err());
        assert!(probe.run(Arguments::default()).is_err());
        assert!(probe.take_outgoing().is_empty());
        assert!(probe.feed(&[]).is_err());
    }
}

#[test]
fn eof_trailing_bytes_wrong_identity_and_wire_limit_are_terminal() {
    let chain = Chain::new(&[(1, script(&[(Opcode::RETURN, 0)]))]);
    for kind in 0..4 {
        let mut probe = chain.begin(1);
        match kind {
            0 => assert!(probe.finish().is_err()),
            1 => {
                probe.feed(&reply(12, 1, &chain.archives[&1])).unwrap();
                assert!(probe.feed(&[0]).is_err());
            }
            2 => assert!(probe.feed(&reply(12, 2, &chain.archives[&1])).is_err()),
            _ => assert!(probe.feed(&vec![0; MAX_RECEIVED_BYTES + 1]).is_err()),
        }
        assert!(!probe.complete());
        assert!(probe.report().is_none());
        assert!(probe.file(1).is_err());
    }
}

#[test]
fn invalid_call_ids_and_repository_budgets_are_rejected_transactionally() {
    let mut repo = Repository::new();
    assert!(
        repo.insert(1, script(&[(Opcode::GOSUB_WITH_PARAMS, -1)]), true)
            .is_err()
    );
    assert!(repo.scripts.is_empty());
    let leaf = script(&[(Opcode::RETURN, 0)]);
    repo.insert(1, leaf.clone(), true).unwrap();
    assert!(repo.insert(1, leaf.clone(), true).is_err());
    assert!(repo.insert(MAX_SCRIPT_ID + 1, leaf.clone(), true).is_err());
    assert!(repo.insert(2, vec![0; MAX_SCRIPT_BYTES + 1], true).is_err());
    let n = repo.scripts.len();
    repo.total_bytes = MAX_TOTAL_BYTES;
    assert!(repo.insert(2, leaf.clone(), true).is_err());
    repo.total_bytes = leaf.len();
    repo.total_instructions = MAX_TOTAL_INSTRUCTIONS;
    assert!(repo.insert(2, leaf.clone(), true).is_err());
    assert_eq!(repo.scripts.len(), n);
    repo.total_instructions = 1;
    for id in 2..=MAX_SCRIPTS as u32 {
        repo.insert(id, leaf.clone(), true).unwrap();
    }
    assert!(repo.insert(65, leaf, true).is_err());
    assert!(ScriptGraphProbe::new(b"test", MAX_SCRIPT_ID + 1).is_err());
}

#[test]
fn host_effects_and_wrong_arguments_are_not_fabricated_or_skipped() {
    let host = script(&[(Opcode::PUSH_VAR, 0), (Opcode::RETURN, 0)]);
    let chain = Chain::new(&[(1, host)]);
    let mut probe = chain.begin(1);
    probe.feed(&reply(12, 1, &chain.archives[&1])).unwrap();
    assert!(
        probe
            .run(Arguments::default())
            .unwrap_err()
            .contains("UnsupportedOpcode")
    );
    assert!(
        probe
            .run(Arguments {
                ints: &[1],
                ..Default::default()
            })
            .unwrap_err()
            .contains("ArgumentCount")
    );
    assert!(probe.complete());
}

fn multi_file_archive(script: &[u8], extra_bytes: usize) -> Vec<u8> {
    let mut data = script.to_vec();
    data.resize(data.len() + extra_bytes, 0);
    data.extend_from_slice(&(script.len() as i32).to_be_bytes());
    data.extend_from_slice(&(extra_bytes as i32 - script.len() as i32).to_be_bytes());
    data.push(1);
    raw(&data)
}

#[test]
fn cumulative_expansion_counts_discarded_files_and_reference_at_exact_limit() {
    let scripts = [
        script(&[(Opcode::GOSUB_WITH_PARAMS, 2), (Opcode::RETURN, 0)]),
        script(&[(Opcode::GOSUB_WITH_PARAMS, 3), (Opcode::RETURN, 0)]),
        script(&[(Opcode::RETURN, 0)]),
    ];
    let build = |padding: [usize; 3]| {
        Chain::with_archives(
            scripts
                .iter()
                .enumerate()
                .map(|(i, script)| (i as u32 + 1, (multi_file_archive(script, padding[i]), 2)))
                .collect(),
        )
    };
    let empty = build([0; 3]);
    let retained = scripts.iter().map(Vec::len).sum::<usize>();
    let overhead = empty.reference.len() - 5 + retained + 3 * 9;
    let chunk = 2 * 1024 * 1024;
    for over in [0, 1] {
        let chain = build([
            chunk,
            chunk,
            MAX_TOTAL_EXPANDED_BYTES - overhead - 2 * chunk + over,
        ]);
        let mut probe = chain.begin(1);
        for id in [1, 2] {
            probe.feed(&reply(12, id, &chain.archives[&id])).unwrap();
            probe.take_outgoing();
        }
        let result = probe.feed(&reply(12, 3, &chain.archives[&3]));
        if over == 0 {
            result.unwrap();
            let report = probe.report().unwrap();
            assert_eq!(report.total_expanded_bytes, MAX_TOTAL_EXPANDED_BYTES);
            assert_eq!(report.total_bytes, retained);
            assert_eq!(report.scripts.len(), 3);
            assert_eq!(probe.file(3).unwrap(), scripts[2]);
            probe.run(Arguments::default()).unwrap();
            probe.finish().unwrap();
        } else {
            assert!(
                result
                    .unwrap_err()
                    .contains("cumulative expanded byte limit")
            );
            assert!(!probe.complete());
            assert!(probe.report().is_none());
            assert!(probe.repository.scripts.is_empty());
            assert!(probe.pending.is_empty());
            assert_eq!(probe.expanded, 0);
            assert!(probe.file(1).is_err());
            assert!(probe.run(Arguments::default()).is_err());
            assert!(probe.take_outgoing().is_empty());
            assert!(probe.feed(&[]).is_err());
        }
    }
}

#[test]
fn expansion_budget_is_checked_before_compressed_payload_decoding() {
    for mode in [1, 2] {
        let container = [
            vec![mode],
            0u32.to_be_bytes().to_vec(),
            1024u32.to_be_bytes().to_vec(),
        ]
        .concat();
        assert_eq!(
            assets::decode_with_budget(&container, Integrity::default(), 1023).unwrap_err(),
            "asset cumulative expanded byte limit"
        );
        assert_ne!(
            assets::decode_with_budget(&container, Integrity::default(), 1024).unwrap_err(),
            "asset cumulative expanded byte limit"
        );
    }
}

#[test]
fn discovery_count_budget_includes_pending_unique_callees() {
    let ops: Vec<_> = (2..=65).map(|id| (Opcode::GOSUB_WITH_PARAMS, id)).collect();
    let chain = Chain::new(&[(1, script(&ops))]);
    let mut probe = chain.begin(1);
    assert!(
        probe
            .feed(&reply(12, 1, &chain.archives[&1]))
            .unwrap_err()
            .contains("count limit")
    );
    assert!(probe.repository.scripts.is_empty());
    assert!(probe.pending.is_empty());
}
