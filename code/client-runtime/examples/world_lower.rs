use openrs_client_runtime::floors::WorldFloorProbe;
use openrs_client_runtime::lowering::lower_plane;
use openrs_client_runtime::materials::WorldMaterialProbe;
use openrs_client_runtime::surface_ground::{SurfaceGroundInputs, compose_surface_ground};
use openrs_client_runtime::surface_palette::{compose_surface_tiles, compose_underlay_palette};
use openrs_client_runtime::{AssetProbe, WorldTerrainProbe};
use openrs_scene::ground_geometry::GroundGeometryOptions;
use openrs_scene::rgb::HslRgbLut;
use openrs_scene::surface::{NativeMapSize, StandardMapLayout, SurfaceHeights};
use std::io::{Read, Write};
use std::net::TcpStream;
use std::time::Duration;

const TOKEN: &str = "ev9+VAp5/tMKeNR/7MOuH6lKWS+rGkHK";

trait Driven {
    fn outgoing(&mut self) -> Vec<u8>;
    fn feed(&mut self, bytes: &[u8]) -> Result<(), String>;
    fn finish(&mut self) -> Result<(), String>;
    fn done(&self) -> bool;
}

impl Driven for WorldTerrainProbe {
    fn outgoing(&mut self) -> Vec<u8> {
        self.take_outgoing()
    }
    fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        WorldTerrainProbe::feed(self, bytes)
    }
    fn finish(&mut self) -> Result<(), String> {
        WorldTerrainProbe::finish(self)
    }
    fn done(&self) -> bool {
        self.complete()
    }
}

impl Driven for WorldFloorProbe {
    fn outgoing(&mut self) -> Vec<u8> {
        self.take_outgoing()
    }
    fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        WorldFloorProbe::feed(self, bytes)
    }
    fn finish(&mut self) -> Result<(), String> {
        WorldFloorProbe::finish(self)
    }
    fn done(&self) -> bool {
        self.complete()
    }
}

impl Driven for WorldMaterialProbe {
    fn outgoing(&mut self) -> Vec<u8> {
        self.take_outgoing()
    }
    fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        WorldMaterialProbe::feed(self, bytes)
    }
    fn finish(&mut self) -> Result<(), String> {
        WorldMaterialProbe::finish(self)
    }
    fn done(&self) -> bool {
        self.complete()
    }
}

impl Driven for AssetProbe {
    fn outgoing(&mut self) -> Vec<u8> {
        self.take_outgoing()
    }
    fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        AssetProbe::feed(self, bytes)
    }
    fn finish(&mut self) -> Result<(), String> {
        AssetProbe::finish(self)
    }
    fn done(&self) -> bool {
        self.complete()
    }
}

fn drive(address: &str, probe: &mut dyn Driven) -> Result<(), String> {
    let mut stream = TcpStream::connect(address).map_err(|error| error.to_string())?;
    stream
        .set_read_timeout(Some(Duration::from_secs(30)))
        .map_err(|error| error.to_string())?;
    let mut buffer = vec![0u8; 65536];
    loop {
        let outgoing = probe.outgoing();
        if !outgoing.is_empty() {
            stream
                .write_all(&outgoing)
                .map_err(|error| error.to_string())?;
        }
        if probe.done() {
            return Ok(());
        }
        let read = stream
            .read(&mut buffer)
            .map_err(|error| error.to_string())?;
        if read == 0 {
            probe.finish()?;
            return if probe.done() {
                Ok(())
            } else {
                Err("connection ended early".into())
            };
        }
        probe.feed(&buffer[..read])?;
    }
}

fn main() -> Result<(), String> {
    let args: Vec<String> = std::env::args().skip(1).collect();
    if args.len() != 8 {
        return Err(
            "usage: address baseX baseY mapSize regionsJson plane maxVertices randomUnit".into(),
        );
    }
    let address = &args[0];
    let base = [
        args[1].parse::<i32>().map_err(|e| e.to_string())?,
        args[2].parse::<i32>().map_err(|e| e.to_string())?,
    ];
    let map_size = NativeMapSize::from_tiles(args[3].parse().map_err(|_| "mapSize")?)
        .map_err(|_| "invalid map size")?;
    let regions: Vec<u16> = serde_json::from_str(&args[4]).map_err(|e| e.to_string())?;
    let plane: usize = args[5].parse().map_err(|_| "plane")?;
    let max_vertices: usize = args[6].parse().map_err(|_| "maxVertices")?;
    let unit: f64 = args[7].parse().map_err(|_| "randomUnit")?;
    let mut terrain = WorldTerrainProbe::new(TOKEN.as_bytes(), &regions)?;
    drive(address, &mut terrain)?;
    let mut floors = WorldFloorProbe::new(TOKEN.as_bytes())?;
    drive(address, &mut floors)?;
    let mut materials = WorldMaterialProbe::new(TOKEN.as_bytes())?;
    drive(address, &mut materials)?;
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
    let mesh = lower_plane(&surface, None, &ground, plane, &rgb, materials.table()?)?;
    println!(
        "{}",
        serde_json::to_string(&mesh.report).map_err(|e| e.to_string())?
    );
    Ok(())
}
