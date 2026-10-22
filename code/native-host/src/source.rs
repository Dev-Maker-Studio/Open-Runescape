use renderer::frame::{self, Frame};
use serde::Deserialize;
use std::{fs::File, io::Read, path::Path};

pub const MAX_FRAME: usize = 128 * 1024 * 1024;
pub const MAX_RECORD: usize = 32 * 1024 * 1024;

#[derive(Clone, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Latest {
    pub frame: u32,
    pub slot: u8,
    pub file: String,
    pub width: u32,
    pub height: u32,
    pub tick: u64,
    pub input_seq: u32,
    pub nanos: u64,
    pub sha256: String,
}

fn read(path: &Path, max: usize) -> Result<Vec<u8>, String> {
    let f = File::open(path).map_err(|_| "spool not ready".to_string())?;
    if f.metadata().map_err(|e| e.to_string())?.len() > max as u64 {
        return Err("spool file exceeds limit".into());
    }
    let mut b = Vec::new();
    f.take(max as u64 + 1)
        .read_to_end(&mut b)
        .map_err(|e| e.to_string())?;
    if b.len() > max {
        return Err("spool file exceeds limit".into());
    }
    Ok(b)
}

pub fn latest(dir: &Path) -> Result<Latest, String> {
    let m: Latest = serde_json::from_slice(&read(&dir.join("latest.json"), 16384)?)
        .map_err(|_| "invalid latest.json".to_string())?;
    if m.slot > 1
        || m.file != format!("frame-{}.orf", m.slot)
        || m.width == 0
        || m.height == 0
        || m.width > 8192
        || m.height > 8192
    {
        return Err("invalid spool metadata".into());
    }
    Ok(m)
}

fn word(b: &[u8], n: usize) -> Result<u32, String> {
    b.get(n..n + 4)
        .map(|b| u32::from_le_bytes(b.try_into().unwrap()))
        .ok_or("truncated ORF".into())
}

pub fn parse(bytes: &[u8]) -> Result<Frame, String> {
    if bytes.len() > MAX_FRAME || !matches!(bytes.get(..4), Some(b"ORF3" | b"ORF4")) || bytes.len() < 44 {
        return Err("expected bounded ORF3 or ORF4".into());
    }
    let flags = word(bytes, 4)?;
    let mut f = Frame {
        width: word(bytes, 8)?,
        height: word(bytes, 12)?,
        clear: [16, 20, 24, 28].map(|at| f32::from_bits(word(bytes, at).unwrap())),
        ..Default::default()
    };
    if f.width == 0 || f.height == 0 || f.width > 8192 || f.height > 8192
        || f.clear.iter().any(|v| !v.is_finite()) {
        return Err("invalid ORF dimensions or clear".into());
    }
    let mut total = 32usize;
    let mut at = 32;
    let mut count = 0;
    loop {
        let (kind, raw, packed) = (
            word(bytes, at)?,
            word(bytes, at + 4)? as usize,
            word(bytes, at + 8)? as usize,
        );
        at += 12;
        count += 1;
        total = total.checked_add(raw).and_then(|n| n.checked_add(12)).ok_or("ORF resource limit")?;
        if count > 100000
            || raw > MAX_RECORD
            || packed > MAX_RECORD
            || total > MAX_FRAME
        {
            return Err("ORF resource limit".into());
        }
        if kind == 5 {
            if raw != 0 || packed != 0 || at != bytes.len() {
                return Err("invalid ORF end".into());
            }
            break;
        }
        if kind > 6 { return Err("unknown ORF record".into()); }
        if kind == 6 && &bytes[..4] != b"ORF4" { return Err("typed texture requires ORF4".into()); }
        let len = if packed == 0 { raw } else { packed };
        let p = bytes.get(at..at + len).ok_or("truncated ORF payload")?;
        let inflated;
        let p = if packed == 0 {
            p
        } else {
            inflated = frame::inflate_record(p, raw).ok_or("invalid bounded ORF compression")?;
            &inflated
        };
        frame::append_record(kind, p, &mut f).ok_or("invalid ORF record")?;
        at += len;
    }
    frame::normalize_depth(&mut f, flags).ok_or("invalid ORF depth uniforms")?;
    Ok(f)
}

pub fn load(dir: &Path, m: &Latest) -> Result<Option<Frame>, String> {
    let bytes = read(&dir.join(&m.file), MAX_FRAME)?;
    use sha2::{Digest, Sha256};
    let digest = format!("{:x}", Sha256::digest(&bytes));
    if latest(dir)? != *m || digest != m.sha256 {
        return Ok(None);
    }
    let f = parse(&bytes)?;
    if (f.width, f.height) != (m.width, m.height) {
        return Err("spool dimensions disagree".into());
    }
    Ok(Some(f))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn orf(kind: u32, payload: &[u8], compressed: bool) -> Vec<u8> {
        let mut b = frame::write(&Frame {
            width: 64,
            height: 64,
            ..Default::default()
        });
        b.truncate(32);
        let packed = miniz_oxide::deflate::compress_to_vec_zlib(payload, 3);
        b.extend(
            [
                kind,
                payload.len() as u32,
                if compressed { packed.len() as u32 } else { 0 },
            ]
            .into_iter()
            .flat_map(u32::to_le_bytes),
        );
        b.extend(if compressed { &packed } else { payload });
        b.extend([5u32, 0, 0].into_iter().flat_map(u32::to_le_bytes));
        b
    }

    #[test]
    #[ignore]
    fn benchmark_immutable_orf() {
        let path = std::env::var_os("OPENRS_NATIVE_BENCH_ORF").expect("OPENRS_NATIVE_BENCH_ORF");
        let bytes = read(Path::new(&path), MAX_FRAME).unwrap();
        let mut at = 32;
        let mut raw_bytes = 0usize;
        let mut records = 0usize;
        let mut inflated_bytes = 0usize;
        let mut largest_inflated = 0usize;
        while at < bytes.len() {
            let kind = word(&bytes, at).unwrap();
            let raw = word(&bytes, at + 4).unwrap() as usize;
            let packed = word(&bytes, at + 8).unwrap() as usize;
            raw_bytes += raw;
            records += 1;
            if packed != 0 {
                inflated_bytes += raw;
                largest_inflated = largest_inflated.max(raw);
            }
            at += 12 + if packed == 0 { raw } else { packed };
            if kind == 5 { break; }
        }
        let f = parse(&bytes).unwrap();
        assert_eq!(frame::write(&f), frame::write(&staged_reference(&bytes)));
        let owned_bytes: usize = f.meshes.iter().map(|m| m.vertices.capacity() + m.indices.capacity() * 4).sum::<usize>()
            + f.textures.iter().map(|t| t.rgba.capacity()).sum::<usize>()
            + f.uniforms.iter().map(|u| u.bytes.capacity()).sum::<usize>()
            + f.instances.iter().map(|i| i.bytes.capacity()).sum::<usize>();
        drop(f);
        eprintln!("ORF buffers input={} expanded_frame={} compressed_record_output_sum={} largest_inflated={} owned_capacity_sum={}", bytes.len(), 32 + raw_bytes + records * 12, inflated_bytes, largest_inflated, owned_bytes);
        let mut elapsed = [Vec::new(), Vec::new()];
        for n in 0..32 {
            for mode in if n % 2 == 0 { [0, 1] } else { [1, 0] } {
                let start = std::time::Instant::now();
                let f = std::hint::black_box(if mode == 0 {
                    parse(std::hint::black_box(&bytes)).unwrap()
                } else {
                    staged_reference(std::hint::black_box(&bytes))
                });
                if n >= 2 { elapsed[mode].push(start.elapsed().as_secs_f64() * 1000.0); }
                drop(f);
            }
        }
        for (name, times) in ["direct", "staged_reference"].into_iter().zip(&mut elapsed) {
            times.sort_by(f64::total_cmp);
            eprintln!("ORF benchmark path={name} repeats={} median_ms={:.3} mean_ms={:.3} min_ms={:.3}", times.len(), (times[14] + times[15]) / 2.0, times.iter().sum::<f64>() / times.len() as f64, times[0]);
        }
    }

    fn staged_reference(bytes: &[u8]) -> Frame {
        let mut plain = bytes[..32].to_vec();
        let mut at = 32;
        loop {
            let kind = word(bytes, at).unwrap();
            let raw = word(bytes, at + 4).unwrap();
            let packed = word(bytes, at + 8).unwrap();
            at += 12;
            plain.extend([kind, raw, 0].into_iter().flat_map(u32::to_le_bytes));
            if kind == 5 { break; }
            let len = if packed == 0 { raw } else { packed } as usize;
            let p = &bytes[at..at + len];
            if packed == 0 { plain.extend(p); } else {
                plain.extend(miniz_oxide::inflate::decompress_to_vec_zlib_with_limit(p, raw as usize).unwrap());
            }
            at += len;
        }
        frame::parse(&plain).unwrap()
    }

    #[test]
    fn direct_records_match_staging_and_normalize_depth_once() {
        let mut f = frame::parse(include_bytes!("../../renderer/tests/fixtures/typed.orf")).unwrap();
        f.uniforms.push(frame::Uniform { id: 3, bytes: (0..16).flat_map(|i| (i as f32 - 4.0).to_le_bytes()).collect() });
        let mut bytes = frame::write(&f);
        bytes[4..8].copy_from_slice(&(frame::GL_DEPTH | frame::DEFLATE).to_le_bytes());
        let expected = staged_reference(&bytes);
        let actual = parse(&bytes).unwrap();
        assert_eq!(frame::write(&actual), frame::write(&expected));
        assert_eq!(actual.textures[0].rgba, f.textures[0].rgba);
        let matrix: Vec<f32> = actual.uniforms[0].bytes.chunks_exact(4).map(|v| f32::from_le_bytes(v.try_into().unwrap())).collect();
        assert_eq!(matrix[2], -1.5);
        assert_eq!(matrix[6], 2.5);
    }

    #[test]
    fn rejects_chunk_limits_shapes_and_bad_depth() {
        let header = frame::write(&Frame { width: 64, height: 64, ..Default::default() })[..32].to_vec();
        let reject = |kind: u32, raw: u32, packed: u32, payload: &[u8]| {
            let mut b = header.clone();
            b.extend([kind, raw, packed].into_iter().flat_map(u32::to_le_bytes));
            b.extend(payload);
            b.extend([5u32, 0, 0].into_iter().flat_map(u32::to_le_bytes));
            assert!(parse(&b).is_err());
        };
        reject(2, MAX_RECORD as u32 + 1, 0, &[]);
        reject(2, 8, MAX_RECORD as u32 + 1, &[]);
        reject(2, 8, 0, &[0; 7]);
        reject(7, 0, 0, &[]);
        reject(5, 1, 0, &[0]);
        reject(3, 12, 0, &[0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0]);
        let packed = miniz_oxide::deflate::compress_to_vec_zlib(&vec![0; 65536], 3);
        reject(2, 8, packed.len() as u32, &packed);
        reject(2, 65537, packed.len() as u32, &packed);
        let mut b = header.clone();
        for _ in 0..99999 {
            b.extend([2u32, 8, 0, 0, 0].into_iter().flat_map(u32::to_le_bytes));
        }
        b.extend([5u32, 0, 0].into_iter().flat_map(u32::to_le_bytes));
        assert!(parse(&b).is_ok());
        assert!(frame::parse(&b).is_some());
        b.splice(32..32, [2u32, 8, 0, 0, 0].into_iter().flat_map(u32::to_le_bytes));
        assert!(parse(&b).is_err());
        assert!(frame::parse(&b).is_none());
        let mut f = Frame { width: 64, height: 64, ..Default::default() };
        f.uniforms.push(frame::Uniform { id: 0, bytes: vec![0; 63] });
        let mut b = frame::write(&f);
        b[4..8].copy_from_slice(&frame::GL_DEPTH.to_le_bytes());
        assert!(parse(&b).is_err());
    }

    #[test]
    fn cumulative_inflation_budget_includes_chunk_headers() {
        let mut payload: Vec<u8> = [0u32, 209715, 209715 * 80].into_iter().flat_map(u32::to_le_bytes).collect();
        payload.resize(12 + 209715 * 80, 0);
        assert_eq!(payload.len() * 8 + 32, MAX_FRAME);
        let packed = miniz_oxide::deflate::compress_to_vec_zlib(&payload, 3);
        let mut bytes = frame::write(&Frame { width: 64, height: 64, ..Default::default() })[..32].to_vec();
        for _ in 0..8 {
            bytes.extend([3, payload.len() as u32, packed.len() as u32].into_iter().flat_map(u32::to_le_bytes));
            bytes.extend(&packed);
        }
        drop(payload);
        bytes.extend([5u32, 0, 0].into_iter().flat_map(u32::to_le_bytes));
        assert!(parse(&bytes).is_err());
        assert!(frame::parse(&bytes).is_none());
    }

    #[test]
    fn stored_and_compressed_shapes_keep_host_limits() {
        let mut uniform: Vec<u8> = [7u32, 65537].into_iter().flat_map(u32::to_le_bytes).collect();
        uniform.resize(8 + 65537, 0);
        let mut mesh: Vec<u8> = [7u32, renderer::pass::Pass::Model as u32, 1, 1].into_iter().flat_map(u32::to_le_bytes).collect();
        mesh.resize(16 + renderer::pass::Pass::Model.stride() as usize, 0);
        mesh.extend(1u32.to_le_bytes());
        let instances = [7u32, 1, 0].into_iter().flat_map(u32::to_le_bytes).collect();
        for (kind, payload) in [(0, mesh), (2, uniform), (3, instances)] {
            for compressed in [false, true] {
                let bytes = orf(kind, &payload, compressed);
                assert!(parse(&bytes).is_err());
                assert!(frame::parse(&bytes).is_none());
            }
        }
    }

    #[test]
    fn headers_truncations_end_markers_and_compressed_tails_are_rejected() {
        let bytes = frame::write(&renderer::scene::build("model", 0, 64, 64).unwrap());
        for len in 0..bytes.len() {
            assert!(parse(&bytes[..len]).is_err(), "accepted prefix {len}");
            assert!(frame::parse(&bytes[..len]).is_none(), "accepted shared prefix {len}");
        }
        for (at, value) in [(8, 0u32), (12, 0), (8, 8193), (12, 8193), (16, f32::NAN.to_bits()), (20, f32::INFINITY.to_bits()), (28, f32::NEG_INFINITY.to_bits())] {
            let mut bad = bytes.clone();
            bad[at..at + 4].copy_from_slice(&value.to_le_bytes());
            assert!(parse(&bad).is_err());
        }
        for (at, value) in [(bytes.len() - 8, 1u32), (bytes.len() - 4, 1)] {
            let mut bad = bytes.clone();
            bad[at..at + 4].copy_from_slice(&value.to_le_bytes());
            assert!(parse(&bad).is_err());
            assert!(frame::parse(&bad).is_none());
        }
        let compressed = orf(2, &[0; 8], true);
        for tail in [vec![0], miniz_oxide::deflate::compress_to_vec_zlib(&[0; 8], 3)] {
            let mut bad = compressed.clone();
            bad[40..44].copy_from_slice(&(word(&compressed, 40).unwrap() + tail.len() as u32).to_le_bytes());
            bad.splice(bad.len() - 12..bad.len() - 12, tail);
            assert!(parse(&bad).is_err());
            assert!(frame::parse(&bad).is_none());
        }
    }

    #[test]
    fn draws_accept_only_canonical_or_four_legacy_zero_bytes() {
        for (textures, name) in [(0, ""), (2, "skip")] {
            let mut canonical = vec![0; 60 + textures * 4];
            canonical[12..16].copy_from_slice(&(textures as u32).to_le_bytes());
            canonical[56 + textures * 4..60 + textures * 4]
                .copy_from_slice(&(name.len() as u32).to_le_bytes());
            canonical.extend(name.as_bytes());
            for compressed in [false, true] {
                assert_eq!(parse(&orf(4, &canonical, compressed)).unwrap().draws.len(), 1);
                let mut legacy = canonical.clone();
                legacy.extend([0; 4]);
                assert_eq!(parse(&orf(4, &legacy, compressed)).unwrap().draws.len(), 1);
                for extra in [1, 2, 3, 5, 8] {
                    let mut bad = canonical.clone();
                    bad.extend(vec![0; extra]);
                    assert!(parse(&orf(4, &bad, compressed)).is_err());
                }
                for at in 0..4 {
                    let mut bad = legacy.clone();
                    bad[canonical.len() + at] = 1;
                    assert!(parse(&orf(4, &bad, compressed)).is_err());
                }
            }
        }
        assert!(frame::append_record(2, &[0; 12], &mut Frame::default()).is_none());
    }
}

#[cfg(test)]
mod typed_tests {
    use super::*;
    use renderer::frame::TextureFormat;

    #[test]
    fn shared_jvm_typed_fixture_is_bounded_and_bit_exact() {
        let fixture = include_bytes!("../../renderer/tests/fixtures/typed.orf");
        let f = parse(fixture).unwrap();
        let t = &f.textures[0];
        assert_eq!(t.format, TextureFormat::Rgba16Float);
        assert_eq!(t.sampler, 9);
        assert_eq!(t.rgba, [0x4400u16, 0xb800, 0, 0x3c00].repeat(2).into_iter().flat_map(u16::to_le_bytes).collect::<Vec<_>>());
        for (offset, value) in [(60, 0u32), (60, 2), (64, 15), (64, 17), (68, 256), (68, 6), (68, 48), (56, 6), (48, u32::MAX), (36, MAX_RECORD as u32 + 1)] {
            let mut bad = fixture.to_vec();
            bad[offset..offset + 4].copy_from_slice(&value.to_le_bytes());
            assert!(parse(&bad).is_err(), "accepted offset {offset} value {value}");
        }
        let mut old = fixture.to_vec();
        old[..4].copy_from_slice(b"ORF3");
        assert!(parse(&old).is_err());
        let payload = &fixture[44..fixture.len() - 12];
        let compressed = miniz_oxide::deflate::compress_to_vec_zlib(payload, 3);
        let mut packed = fixture[..32].to_vec();
        packed.extend([6, payload.len() as u32, compressed.len() as u32].into_iter().flat_map(u32::to_le_bytes));
        packed.extend(&compressed);
        packed.extend([5u32, 0, 0].into_iter().flat_map(u32::to_le_bytes));
        assert_eq!(parse(&packed).unwrap().textures[0].rgba, t.rgba);
        packed[36..40].copy_from_slice(&1u32.to_le_bytes());
        assert!(parse(&packed).is_err());
        let mut tail = fixture.to_vec();
        tail.push(0);
        assert!(parse(&tail).is_err());
    }
}
