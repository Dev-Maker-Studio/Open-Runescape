use openrs_assets::{Integrity, Limits, decode_container, parse_reference_table};
use openrs_client_runtime::WorldTerrainProbe;
use openrs_client_runtime::floors::WorldFloorProbe;
use openrs_client_runtime::lowering::{lit_normal_heights, lower_plane};
use openrs_client_runtime::materials::WorldMaterialProbe;
use openrs_client_runtime::surface_ground::{SurfaceGroundInputs, compose_surface_ground};
use openrs_client_runtime::surface_palette::{compose_surface_tiles, compose_underlay_palette};
use openrs_scene::ground_geometry::GroundGeometryOptions;
use openrs_scene::ground_lowering::{
    GroundMaterialShading, UNTEXTURED_MATERIAL_ID, lighting_intensity, lighting_normals,
};
use openrs_scene::rgb::HslRgbLut;
use openrs_scene::surface::{NativeMapSize, StandardMapLayout, SurfaceHeights};
use std::collections::BTreeSet;
use std::fmt::Write as _;
use std::fs::{self, File};
use std::io::{Read, Seek, SeekFrom};
use std::path::{Path, PathBuf};
use whirlpool::{Digest, Whirlpool};

const TOKEN: &str = "ev9+VAp5/tMKeNR/7MOuH6lKWS+rGkHK";
const SECTOR: u64 = 520;
const PREFETCH_KEYS: usize = 27;

trait Driven {
    fn outgoing(&mut self) -> Vec<u8>;
    fn feed(&mut self, bytes: &[u8]) -> Result<(), String>;
    fn finish(&mut self) -> Result<(), String>;
    fn done(&self) -> bool;
}

macro_rules! driven {
    ($type:ty) => {
        impl Driven for $type {
            fn outgoing(&mut self) -> Vec<u8> {
                self.take_outgoing()
            }
            fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
                <$type>::feed(self, bytes)
            }
            fn finish(&mut self) -> Result<(), String> {
                <$type>::finish(self)
            }
            fn done(&self) -> bool {
                self.complete()
            }
        }
    };
}

driven!(WorldTerrainProbe);
driven!(WorldFloorProbe);
driven!(WorldMaterialProbe);

fn revision(container: &[u8]) -> i32 {
    let limits = Limits {
        max_container_bytes: 1 << 28,
        max_uncompressed_bytes: 1 << 28,
        max_archives: 1 << 20,
        max_files_per_archive: 1 << 20,
        max_total_files: 1 << 24,
        max_archive_id: u32::MAX,
        max_file_id: u32::MAX,
    };
    if container.is_empty() {
        return 0;
    }
    decode_container(container, Integrity::default(), &limits)
        .ok()
        .and_then(|data| parse_reference_table(&data, &limits).ok())
        .map_or(0, |table| table.revision)
}

struct PackedCache {
    root: PathBuf,
}

impl PackedCache {
    fn container(&self, index: u8, archive: u32) -> Option<Vec<u8>> {
        let idx = fs::read(self.root.join(format!("main_file_cache.idx{index}"))).ok()?;
        let entry = idx.get(archive as usize * 6..archive as usize * 6 + 6)?;
        let size = u32::from_be_bytes([0, entry[0], entry[1], entry[2]]) as usize;
        let mut sector = u32::from_be_bytes([0, entry[3], entry[4], entry[5]]) as u64;
        if size == 0 || sector == 0 {
            return None;
        }
        let header = if archive > 0xFFFF { 10 } else { 8 };
        let mut dat = File::open(self.root.join("main_file_cache.dat2")).ok()?;
        let mut out = Vec::with_capacity(size);
        let mut block = [0u8; SECTOR as usize];
        while out.len() < size {
            dat.seek(SeekFrom::Start(sector * SECTOR)).ok()?;
            dat.read_exact(&mut block).ok()?;
            let count = (size - out.len()).min(SECTOR as usize - header);
            out.extend_from_slice(&block[header..header + count]);
            let next = if header == 10 { 7 } else { 4 };
            sector = u32::from_be_bytes([0, block[next], block[next + 1], block[next + 2]]) as u64;
        }
        Some(out)
    }

    fn trimmed(&self, index: u8, archive: u32) -> Option<Vec<u8>> {
        let mut container = self.container(index, archive)?;
        let length = i32::from_be_bytes(container.get(1..5)?.try_into().ok()?) as usize;
        let expected = length + if container[0] == 0 { 5 } else { 9 };
        container.truncate(expected);
        Some(container)
    }

    fn version_table(&self) -> Result<Vec<u8>, String> {
        let count = fs::metadata(self.root.join("main_file_cache.idx255"))
            .map_err(|e| e.to_string())?
            .len()
            / 6;
        let mut entries = Vec::new();
        for archive in 0..count as u32 {
            let container = self.trimmed(255, archive).unwrap_or_default();
            let crc = crc32fast::hash(&container) as i32;
            entries.extend_from_slice(&crc.to_be_bytes());
            entries.extend_from_slice(&revision(&container).to_be_bytes());
            if container.is_empty() {
                entries.extend_from_slice(&[0u8; 64]);
            } else {
                entries.extend_from_slice(&Whirlpool::digest(&container));
            }
        }
        let mut body = vec![count as u8];
        body.extend_from_slice(&entries);
        let mut container = vec![0u8];
        container.extend_from_slice(&(body.len() as i32).to_be_bytes());
        container.extend_from_slice(&body);
        Ok(container)
    }

    fn respond(&self, input: &[u8]) -> Result<Vec<u8>, String> {
        let mut reply = Vec::new();
        let mut at = 0;
        while at < input.len() {
            match input[at] {
                15 => {
                    let length = *input.get(at + 1).ok_or("short handshake")? as usize;
                    at += 2 + length;
                    reply.push(0);
                    reply.extend_from_slice(&[0u8; PREFETCH_KEYS * 4]);
                }
                0 | 1 => {
                    let request = input.get(at..at + 6).ok_or("short request")?;
                    at += 6;
                    let index = request[1];
                    let archive =
                        u32::from_be_bytes([request[2], request[3], request[4], request[5]]);
                    let container = if index == 255 && archive == 255 {
                        self.version_table()?
                    } else if index == 255 {
                        self.trimmed(255, archive)
                            .ok_or_else(|| format!("missing reference table {archive}"))?
                    } else {
                        self.trimmed(index, archive)
                            .ok_or_else(|| format!("missing archive {index}/{archive}"))?
                    };
                    let flag = if request[0] == 0 { 0x80 } else { 0 };
                    reply.push(index);
                    reply.extend_from_slice(&archive.to_be_bytes());
                    reply.push(container[0] | flag);
                    reply.extend_from_slice(&container[1..5]);
                    let mut position = 10;
                    for byte in &container[5..] {
                        if position == 512 {
                            reply.push(255);
                            position = 1;
                        }
                        reply.push(*byte);
                        position += 1;
                    }
                }
                2 | 3 | 6 => at += 6,
                other => return Err(format!("unexpected request opcode {other}")),
            }
        }
        Ok(reply)
    }
}

fn drive(cache: &PackedCache, probe: &mut dyn Driven) -> Result<(), String> {
    loop {
        let outgoing = probe.outgoing();
        let reply = if outgoing.is_empty() {
            Vec::new()
        } else {
            cache.respond(&outgoing)?
        };
        if probe.done() {
            return Ok(());
        }
        if reply.is_empty() {
            probe.finish()?;
            return if probe.done() {
                Ok(())
            } else {
                Err("probe stalled".into())
            };
        }
        probe.feed(&reply)?;
    }
}

fn write(path: &Path, bytes: &[u8]) -> Result<(), String> {
    fs::write(path, bytes).map_err(|e| format!("{}: {e}", path.display()))
}

fn main() -> Result<(), String> {
    let args: Vec<String> = std::env::args().skip(1).collect();
    if args.len() != 9 && args.len() != 10 {
        return Err(
            "usage: cacheDir outDir baseX baseY mapSize regionsJson plane maxVertices randomUnit [lit=1|0]"
                .into(),
        );
    }
    let cache = PackedCache {
        root: PathBuf::from(&args[0]),
    };
    let out = PathBuf::from(&args[1]);
    fs::create_dir_all(&out).map_err(|e| e.to_string())?;
    let base = [
        args[2].parse::<i32>().map_err(|e| e.to_string())?,
        args[3].parse::<i32>().map_err(|e| e.to_string())?,
    ];
    let map_size = NativeMapSize::from_tiles(args[4].parse().map_err(|_| "mapSize")?)
        .map_err(|_| "invalid map size")?;
    let regions: Vec<u16> = serde_json::from_str(&args[5]).map_err(|e| e.to_string())?;
    let plane: usize = args[6].parse().map_err(|_| "plane")?;
    let max_vertices: usize = args[7].parse().map_err(|_| "maxVertices")?;
    let unit: f64 = args[8].parse().map_err(|_| "randomUnit")?;
    let lit = args.get(9).is_none_or(|value| value != "0");
    let mut terrain = WorldTerrainProbe::new(TOKEN.as_bytes(), &regions)?;
    drive(&cache, &mut terrain)?;
    let mut floors = WorldFloorProbe::new(TOKEN.as_bytes())?;
    drive(&cache, &mut floors)?;
    let mut materials = WorldMaterialProbe::new(TOKEN.as_bytes())?;
    drive(&cache, &mut materials)?;
    let report = terrain.report().ok_or("terrain incomplete")?;
    let ids: Vec<u16> = report.regions.iter().map(|r| r.region_id).collect();
    let layout = StandardMapLayout::new(base, map_size, &ids).map_err(|_| "layout")?;
    let mut present = Vec::new();
    let mut absent = Vec::new();
    for region in &report.regions {
        if region.present {
            present.push((region.region_id, terrain.terrain(region.region_id)?));
        } else {
            absent.push(region.region_id);
        }
    }
    let surface = SurfaceHeights::compose(&layout, &present, &absent).map_err(|_| "surface")?;
    let tiles = compose_surface_tiles(&terrain, &surface)?;
    let palette = compose_underlay_palette(&terrain, &surface, floors.repository()?)?;
    let ground = compose_surface_ground(
        SurfaceGroundInputs {
            terrain: &terrain,
            surface: &surface,
            tiles: &tiles,
            palette: &palette,
            floors: &floors,
            materials: &materials,
        },
        plane,
        GroundGeometryOptions::surface(max_vertices),
    )?;
    let rgb = HslRgbLut::from_random_unit(unit).map_err(|_| "random unit")?;
    let table = materials.table()?;
    let mut underwater_probe = WorldTerrainProbe::new_underwater(TOKEN.as_bytes(), &regions)?;
    drive(&cache, &mut underwater_probe)?;
    let underwater_report = underwater_probe
        .report()
        .ok_or("underwater terrain incomplete")?;
    let mut underwater_present = Vec::new();
    let mut underwater_absent = Vec::new();
    for region in &underwater_report.regions {
        if region.present {
            underwater_present.push((
                region.region_id,
                underwater_probe.terrain(region.region_id)?,
            ));
        } else {
            underwater_absent.push(region.region_id);
        }
    }
    let underwater = if underwater_present.is_empty() {
        None
    } else {
        Some(
            SurfaceHeights::compose(&layout, &underwater_present, &underwater_absent)
                .map_err(|_| "underwater surface")?,
        )
    };
    let used = if lit { underwater.as_ref() } else { None };
    let mesh = lower_plane(&surface, used, &ground, plane, &rgb, table)?;

    let size = surface.layout().map_size().tiles();
    let stride = size + 1;
    let mut heights = Vec::with_capacity(stride * stride);
    for y in 0..stride {
        for x in 0..stride {
            heights.push(
                surface
                    .height_native_units(plane, x, y)
                    .ok_or("surface height unavailable")?,
            );
        }
    }
    let normal_heights = lit_normal_heights(&surface, used, plane)?;
    let join = |values: &[i32]| {
        values
            .iter()
            .map(|v| v.to_string())
            .collect::<Vec<_>>()
            .join("\t")
    };
    let mut tsv = String::new();
    let _ = writeln!(tsv, "case\tworld-plane{plane}\t{size}\t{size}");
    let _ = writeln!(tsv, "position\t{}", join(&heights));
    let _ = writeln!(tsv, "normal\t{}", join(&normal_heights));
    let _ = writeln!(tsv, "shadow\t{}", join(&vec![0; heights.len()]));
    let texture_ids: BTreeSet<i32> = ground
        .tiles
        .iter()
        .flat_map(|tile| tile.vertices.iter().map(|vertex| vertex.texture))
        .filter(|id| *id != UNTEXTURED_MATERIAL_ID)
        .collect();
    for id in &texture_ids {
        let material = table.get_material(*id).map_err(|e| e.to_string())?;
        let shading = GroundMaterialShading::from_material(*id, material);
        let _ = writeln!(
            tsv,
            "material\t{}\t{}\t{}\t{}",
            id, shading.alpha, shading.brightness, shading.effect_id
        );
    }
    for tile in ground.tiles.iter() {
        let _ = writeln!(
            tsv,
            "tile\t{}\t{}\t{}\t{}\t{}",
            tile.x,
            tile.y,
            tile.emitted_vertices,
            tile.has_shadows as u8,
            tile.blending_colors.is_some() as u8
        );
        for (index, vertex) in tile.vertices.iter().enumerate() {
            let blend = tile
                .blending_colors
                .as_ref()
                .map_or(vertex.main_color, |colors| colors[index]);
            let _ = writeln!(
                tsv,
                "vertex\t{}\t{}\t{}\t{}\t{}\t{}",
                vertex.offset_x,
                vertex.offset_z,
                vertex.main_color,
                blend,
                vertex.texture,
                vertex.texture_scale
            );
        }
    }
    tsv.push_str("end\n");
    let normals = lighting_normals(&normal_heights, size, size).map_err(|e| e.to_string())?;
    let intensity =
        lighting_intensity(&vec![0u8; heights.len()], size, size).map_err(|e| e.to_string())?;
    let mut lighting = Vec::new();
    for normal in &normals {
        for component in normal {
            lighting.extend_from_slice(&component.to_le_bytes());
        }
    }
    lighting.extend_from_slice(&intensity);
    write(&out.join("rust-lighting.bin"), &lighting)?;
    let mut underwater_tsv = String::new();
    let _ = writeln!(underwater_tsv, "size\t{size}");
    let _ = writeln!(underwater_tsv, "base\t{}\t{}", base[0], base[1]);
    let _ = writeln!(underwater_tsv, "surface\t{}", join(&heights));
    let _ = writeln!(underwater_tsv, "normal\t{}", join(&normal_heights));
    write(&out.join("rust-heights.tsv"), underwater_tsv.as_bytes())?;
    write(&out.join("world-input.tsv"), tsv.as_bytes())?;

    let mut meta = Vec::new();
    for (index, material) in mesh.materials.iter().enumerate() {
        write(
            &out.join(format!("rust-{index}.vertices.bin")),
            &material.vertices,
        )?;
        let indices: Vec<u8> = material
            .indices
            .iter()
            .flat_map(|value| value.to_le_bytes())
            .collect();
        write(&out.join(format!("rust-{index}.indices.bin")), &indices)?;
        meta.push(serde_json::json!({
            "materialId": material.material_id,
            "textureScale": material.texture_scale,
            "vertexBytes": material.vertices.len(),
            "indexCount": material.indices.len(),
        }));
    }
    let summary = serde_json::json!({ "report": mesh.report, "materials": meta });
    write(
        &out.join("rust-mesh.json"),
        serde_json::to_string(&summary)
            .map_err(|e| e.to_string())?
            .as_bytes(),
    )?;
    println!("{}", mesh.report.sha256);
    Ok(())
}
