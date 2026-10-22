use super::*;
use whirlpool::{Digest, Whirlpool};

fn raw(bytes: &[u8]) -> Vec<u8> {
    [
        vec![0],
        (bytes.len() as u32).to_be_bytes().to_vec(),
        bytes.to_vec(),
    ]
    .concat()
}

fn reply(index: u8, archive: u32, container: &[u8]) -> Vec<u8> {
    let mut wire = vec![index];
    wire.extend_from_slice(&archive.to_be_bytes());
    wire.push(container[0]);
    wire.extend_from_slice(&container[1..5]);
    let mut position = 10;
    for &byte in &container[5..] {
        if position == 512 {
            wire.push(255);
            position = 1;
        }
        wire.push(byte);
        position += 1;
    }
    wire
}

struct Chain {
    version: Vec<u8>,
    reference: Vec<u8>,
    archive: Vec<u8>,
    data: Vec<u8>,
}

impl Chain {
    fn new(digest: bool) -> Self {
        let data = include_bytes!("../../../cs2/tests/fixtures/vm-flow.bin").to_vec();
        let archive = raw(&data);
        let mut reference = vec![6];
        reference.extend_from_slice(&7i32.to_be_bytes());
        reference.push(if digest { 2 } else { 0 });
        reference.extend_from_slice(&1u16.to_be_bytes());
        reference.extend_from_slice(&2163u16.to_be_bytes());
        reference.extend_from_slice(&crc32fast::hash(&archive).to_be_bytes());
        if digest {
            reference.extend_from_slice(&Whirlpool::digest(&archive));
        }
        reference.extend_from_slice(&44i32.to_be_bytes());
        reference.extend_from_slice(&1u16.to_be_bytes());
        reference.extend_from_slice(&3u16.to_be_bytes());
        let reference = raw(&reference);
        let version = Self::version(&reference);
        Self {
            version,
            reference,
            archive,
            data,
        }
    }

    fn version(reference: &[u8]) -> Vec<u8> {
        let mut version = vec![13];
        version.extend_from_slice(&[0; 12 * 72]);
        version.extend_from_slice(&crc32fast::hash(reference).to_be_bytes());
        version.extend_from_slice(&9001i32.to_be_bytes());
        version.extend_from_slice(&Whirlpool::digest(reference));
        version.extend_from_slice(&[0x5a; 128]);
        raw(&version)
    }

    fn begin(&self) -> AssetProbe {
        let mut probe = AssetProbe::new(b"test", 12, 2163).unwrap();
        probe.take_outgoing();
        probe.feed(&[0; 109]).unwrap();
        assert_eq!(probe.take_outgoing().len(), 18);
        probe.feed(&reply(255, 255, &self.version)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(255, 12, js5::Priority::High)
        );
        probe
    }

    fn reference(&self, probe: &mut AssetProbe) {
        probe.feed(&reply(255, 12, &self.reference)).unwrap();
        assert_eq!(
            probe.take_outgoing(),
            js5::encode_request(12, 2163, js5::Priority::High)
        );
    }
}

#[test]
fn fetches_sequential_verified_assets_and_decodes_native_script() {
    let chain = Chain::new(true);
    let bytes = [
        vec![0; 109],
        reply(255, 255, &chain.version),
        reply(255, 12, &chain.reference),
        reply(12, 2163, &chain.archive),
    ]
    .concat();
    for chunk_size in [1, 2, 7, 109, 511, 512, 513, bytes.len()] {
        let mut probe = AssetProbe::new(b"test", 12, 2163).unwrap();
        for chunk in bytes.chunks(chunk_size) {
            probe.feed(chunk).unwrap();
        }
        assert!(probe.complete());
        let report = probe.report().unwrap();
        assert_eq!(report.prefetch_count, 27);
        assert_eq!(report.reference_revision, 7);
        assert_eq!(report.archive_revision, 44);
        assert!(report.reference_crc_verified && report.reference_whirlpool_verified);
        assert!(report.archive_crc_verified && report.archive_whirlpool_verified);
        assert!(!report.authenticated);
        assert_eq!(report.files[0].id, 3);
        assert_eq!(report.file_slots, 4);
        assert_ne!(report.file_slots as usize, report.files.len());
        assert_eq!(report.files[0].script.as_ref().unwrap().instructions, 205);
        assert_eq!(probe.file(3).unwrap(), chain.data);
        assert!(probe.file(0).is_err());
        probe.finish().unwrap();
    }
}

#[test]
fn archive_digest_absence_is_not_reported_as_validation() {
    let chain = Chain::new(false);
    let mut probe = chain.begin();
    chain.reference(&mut probe);
    probe.feed(&reply(12, 2163, &chain.archive)).unwrap();
    assert!(!probe.report().unwrap().archive_whirlpool_verified);
    assert!(probe.report().unwrap().reference_whirlpool_verified);
}

#[test]
fn corrupt_reference_or_archive_is_terminal() {
    let chain = Chain::new(true);
    for reference in [true, false] {
        let mut probe = chain.begin();
        if !reference {
            chain.reference(&mut probe);
        }
        let mut container = if reference {
            chain.reference.clone()
        } else {
            chain.archive.clone()
        };
        *container.last_mut().unwrap() ^= 1;
        let wire = if reference {
            reply(255, 12, &container)
        } else {
            reply(12, 2163, &container)
        };
        assert!(probe.feed(&wire).unwrap_err().contains("CRC"));
        assert!(!probe.complete());
        assert!(probe.report().is_none());
        assert!(probe.take_outgoing().is_empty());
        assert!(probe.file(3).is_err());
        assert!(probe.feed(&[]).is_err());
    }
}

#[test]
fn crc_consistency_does_not_hide_whirlpool_mismatch() {
    let mut chain = Chain::new(true);
    chain.version[6 + 12 * 72 + 8] ^= 1;
    let mut probe = chain.begin();
    assert!(
        probe
            .feed(&reply(255, 12, &chain.reference))
            .unwrap_err()
            .contains("Whirlpool")
    );
    let mut chain = Chain::new(true);
    chain.reference[5 + 14] ^= 1;
    chain.version = Chain::version(&chain.reference);
    let mut probe = chain.begin();
    chain.reference(&mut probe);
    assert!(
        probe
            .feed(&reply(12, 2163, &chain.archive))
            .unwrap_err()
            .contains("Whirlpool")
    );
}

#[test]
fn rejects_invalid_missing_or_unrequested_identities() {
    assert!(AssetProbe::new(b"test", 255, 0).is_err());
    assert!(AssetProbe::new(b"test", 12, u32::MAX).is_err());
    let chain = Chain::new(true);
    let mut probe = chain.begin();
    assert!(
        probe
            .feed(&reply(255, 11, &chain.reference))
            .unwrap_err()
            .contains("identity")
    );
    let mut probe = AssetProbe::new(b"test", 13, 0).unwrap();
    probe.feed(&[0; 109]).unwrap();
    assert!(
        probe
            .feed(&reply(255, 255, &chain.version))
            .unwrap_err()
            .contains("index absent")
    );
    let mut probe = AssetProbe::new(b"test", 12, 2164).unwrap();
    probe.feed(&[0; 109]).unwrap();
    probe.feed(&reply(255, 255, &chain.version)).unwrap();
    assert!(
        probe
            .feed(&reply(255, 12, &chain.reference))
            .unwrap_err()
            .contains("archive absent")
    );
}

#[test]
fn rejects_incomplete_close_and_trailing_data() {
    let chain = Chain::new(true);
    let mut probe = chain.begin();
    assert!(probe.finish().is_err());
    assert!(probe.take_outgoing().is_empty());
    let mut probe = chain.begin();
    chain.reference(&mut probe);
    probe.feed(&reply(12, 2163, &chain.archive)).unwrap();
    assert!(probe.feed(&[0]).is_err());
    assert!(probe.report().is_none());
    assert!(probe.file(3).is_err());
}

#[test]
fn bounds_compressed_and_advertised_expanded_lengths_before_allocation() {
    let chain = Chain::new(true);
    let mut probe = chain.begin();
    let mut header = vec![255, 0, 0, 0, 12, 0];
    header.extend_from_slice(&(MAX_CONTAINER_BYTES as u32).to_be_bytes());
    assert!(probe.feed(&header).is_err());
    let mut probe = chain.begin();
    let container = [
        vec![2],
        0u32.to_be_bytes().to_vec(),
        (MAX_EXPANDED_BYTES as u32 + 1).to_be_bytes().to_vec(),
    ]
    .concat();
    assert!(
        probe
            .feed(&reply(255, 12, &container))
            .unwrap_err()
            .contains("expanded byte limit")
    );
}

#[test]
fn total_received_bytes_are_bounded_and_failure_releases_results() {
    let mut probe = AssetProbe::new(b"test", 12, 2163).unwrap();
    assert!(
        probe
            .feed(&vec![0; MAX_RECEIVED_BYTES + 1])
            .unwrap_err()
            .contains("byte limit")
    );
    assert!(probe.report().is_none());
    assert!(probe.take_outgoing().is_empty());
}

#[test]
fn reference_tables_beyond_the_archive_data_limit_are_accepted() {
    let count = 73_523usize;
    let mut reference = vec![7];
    reference.extend_from_slice(&7i32.to_be_bytes());
    reference.push(0);
    reference.extend_from_slice(&(count as u32 | 0x8000_0000).to_be_bytes());
    for index in 0..count {
        reference.extend_from_slice(&u16::from(index != 0).to_be_bytes());
    }
    reference.extend_from_slice(&vec![0; count * 8]);
    for _ in 0..count {
        reference.extend_from_slice(&1u16.to_be_bytes());
    }
    for _ in 0..count {
        reference.extend_from_slice(&0u16.to_be_bytes());
    }
    assert!(parse_reference_table(&reference, &limits()).is_err());
    let table = parse_reference_table(&reference, &table_limits()).unwrap();
    assert_eq!(table.archives.len(), count);
    assert_eq!(table_limits().max_total_files, 1_000_000);
}
