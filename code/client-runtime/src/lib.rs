use openrs_protocol::js5::{self, HandshakeDecoder, HandshakeReply, Priority, ReplyDecoder};
use serde::Serialize;

mod assets;
pub mod audio_events;
pub mod camera_commands;
pub mod chat_send;
mod cs2;
mod cs2_host;
pub mod definitions;
pub mod environment;
pub mod floors;
mod game;
mod input;
mod inventory;
mod lobby;
mod loc_anim;
pub mod locs;
pub mod lowering;
pub mod materials;
pub mod menu_targets;
pub mod minimenu;
#[cfg_attr(not(target_arch = "wasm32"), allow(dead_code))]
mod hint_screen;
mod model_hover;
#[cfg_attr(not(target_arch = "wasm32"), allow(dead_code))]
mod picker;
mod player_stats;
mod scripts;
pub mod shadows;
pub mod surface_ground;
pub mod surface_palette;
mod terrain;
pub mod textures;
pub mod ui;
mod transfer;
pub mod vars;
pub mod world_draw;
mod world_live;
pub mod world_overlay;
mod world_pose;
pub mod world_resources;
pub mod world_scene;
mod world_session;
pub use assets::{AssetFileReport, AssetProbe, AssetReport, AssetScriptReport};
pub use cs2::{CoreScriptReport, run_core_script};
pub use cs2_host::{
    Cs2Run, HostRunError, HostRunReport, RunInputs, parse_inputs, run_script_bytes,
};
pub use game::{GameHandshakeProbe, GameHandshakeReport};
pub use inventory::{WorldContainerReport, WorldInventoryReport, WorldItemReport};
pub use lobby::{
    LobbyConfig, LobbyEvent, LobbyFeed, LobbyLocationReport, LobbyPhase, LobbyReport, LobbySession,
    LobbyWorldListReport, LobbyWorldReport,
};
pub use player_stats::{WorldPlayerStatsReport, WorldSkillReport};
pub use scripts::{ScriptEntryReport, ScriptGraphProbe, ScriptGraphReport};
pub use terrain::{
    WorldTerrainLimitsReport, WorldTerrainProbe, WorldTerrainRegionReport, WorldTerrainReport,
};
pub use world_session::{
    ChatIdentityParams, WorldBaseReport, WorldConfig, WorldEvent, WorldFeed, WorldInitialReport, WorldReport,
    WorldSession, WorldTick, WorldTileReport, WorldVariableReport,
};

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct IndexVersion {
    pub index: usize,
    pub crc: i32,
    pub revision: i32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProbeReport {
    pub revision: u32,
    pub subrevision: u32,
    pub prefetch_count: usize,
    pub indices: Vec<IndexVersion>,
    pub signature_bytes: usize,
    pub authenticated: bool,
}

pub struct Probe {
    handshake: HandshakeDecoder,
    replies: ReplyDecoder,
    outgoing: Vec<u8>,
    accepted: bool,
    failed: bool,
    prefetch_count: usize,
    report: Option<ProbeReport>,
}

impl Probe {
    pub fn new(token: &[u8]) -> Result<Self, String> {
        Ok(Self {
            handshake: HandshakeDecoder::new(js5::REVISION727_PREFETCH_COUNT)
                .map_err(|e| e.to_string())?,
            replies: ReplyDecoder::new(64 * 1024).map_err(|e| e.to_string())?,
            outgoing: js5::encode_handshake(token).map_err(|e| e.to_string())?,
            accepted: false,
            failed: false,
            prefetch_count: 0,
            report: None,
        })
    }

    pub fn take_outgoing(&mut self) -> Vec<u8> {
        std::mem::take(&mut self.outgoing)
    }

    pub fn complete(&self) -> bool {
        self.report.is_some() && !self.failed
    }

    pub fn report(&self) -> Option<&ProbeReport> {
        self.report.as_ref().filter(|_| !self.failed)
    }

    pub fn feed(&mut self, mut bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("probe has failed".into());
        }
        let result = (|| {
            while !bytes.is_empty() {
                if self.report.is_some() {
                    return Err("unexpected bytes after version table".into());
                }
                if !self.accepted {
                    let feed = self.handshake.feed(bytes).map_err(|e| e.to_string())?;
                    if feed.consumed == 0 {
                        return Err("handshake decoder made no progress".into());
                    }
                    bytes = &bytes[feed.consumed..];
                    if let Some(reply) = feed.item {
                        match reply {
                            HandshakeReply::Accepted { prefetch_keys } => {
                                self.prefetch_count = prefetch_keys.len();
                                self.accepted = true;
                                self.outgoing.extend_from_slice(&js5::acknowledge());
                                self.outgoing.extend_from_slice(&js5::encode_status(false));
                                self.outgoing.extend_from_slice(&js5::encode_request(
                                    js5::VERSION_TABLE_INDEX,
                                    js5::VERSION_TABLE_ARCHIVE,
                                    Priority::High,
                                ));
                            }
                            HandshakeReply::Rejected { status } => {
                                return Err(format!("JS5 handshake rejected: {status}"));
                            }
                        }
                    }
                } else {
                    let feed = self.replies.feed(bytes).map_err(|e| e.to_string())?;
                    if feed.consumed == 0 && feed.item.is_none() {
                        return Err("reply decoder made no progress".into());
                    }
                    bytes = &bytes[feed.consumed..];
                    if let Some(reply) = feed.item {
                        if reply.index != js5::VERSION_TABLE_INDEX
                            || reply.archive != js5::VERSION_TABLE_ARCHIVE
                            || reply.priority != Priority::High
                        {
                            return Err("unexpected JS5 reply identity".into());
                        }
                        let table = js5::decode_version_table(&reply.container)
                            .map_err(|e| e.to_string())?;
                        self.report = Some(ProbeReport {
                            revision: js5::REVISION,
                            subrevision: js5::SUBREVISION,
                            prefetch_count: self.prefetch_count,
                            indices: table
                                .entries
                                .into_iter()
                                .enumerate()
                                .map(|(index, entry)| IndexVersion {
                                    index,
                                    crc: entry.crc,
                                    revision: entry.revision,
                                })
                                .collect(),
                            signature_bytes: table.signature.len(),
                            authenticated: false,
                        });
                    }
                }
            }
            Ok(())
        })();
        if result.is_err() {
            self.failed = true;
            self.outgoing.clear();
            self.report = None;
        }
        result
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.complete() {
            return Ok(());
        }
        self.failed = true;
        self.outgoing.clear();
        Err("connection ended before a complete version table".into())
    }
}

#[cfg(target_arch = "wasm32")]
mod browser {
    use super::*;
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen(js_name = GameHandshakeProbe)]
    pub struct BrowserGameHandshakeProbe(GameHandshakeProbe);

    #[wasm_bindgen(js_class = GameHandshakeProbe)]
    impl BrowserGameHandshakeProbe {
        #[wasm_bindgen(constructor)]
        pub fn new() -> Self {
            Self(GameHandshakeProbe::new())
        }

        pub fn outgoing(&mut self) -> Result<Vec<u8>, JsValue> {
            self.0
                .take_outgoing()
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn complete(&self) -> bool {
            self.0.complete()
        }

        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .feed(bytes)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|error| JsValue::from_str(&error))
        }

        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("game handshake not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("game handshake report serialization failed"))
        }
    }

    impl Default for BrowserGameHandshakeProbe {
        fn default() -> Self {
            Self::new()
        }
    }

    #[wasm_bindgen(js_name = runCoreScript)]
    pub fn run_core(bytes: &[u8]) -> Result<String, JsValue> {
        let report = run_core_script(bytes).map_err(|error| JsValue::from_str(&error))?;
        serde_json::to_string(&report)
            .map_err(|_| JsValue::from_str("CS2 report serialization failed"))
    }

    fn script_id(value: JsValue) -> Result<u32, JsValue> {
        match value.as_f64() {
            Some(id)
                if id.is_finite() && id.fract() == 0.0 && (0.0..=1_000_000.0).contains(&id) =>
            {
                Ok(id as u32)
            }
            _ => Err(JsValue::from_str("invalid script identity")),
        }
    }

    #[wasm_bindgen(js_name = Js5ScriptGraphProbe)]
    pub struct BrowserScriptGraphProbe(ScriptGraphProbe);

    #[wasm_bindgen(js_class = Js5ScriptGraphProbe)]
    impl BrowserScriptGraphProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str, root: JsValue) -> Result<BrowserScriptGraphProbe, JsValue> {
            ScriptGraphProbe::new(token.as_bytes(), script_id(root)?)
                .map(Self)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.take_outgoing()
        }
        pub fn complete(&self) -> bool {
            self.0.complete()
        }
        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .feed(bytes)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|error| JsValue::from_str(&error))
        }
        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("script graph not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("script graph serialization failed"))
        }
        pub fn file(&self, id: JsValue) -> Result<Vec<u8>, JsValue> {
            self.0
                .file(script_id(id)?)
                .map(Vec::from)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn run(&self, ints_json: &str) -> Result<String, JsValue> {
            if ints_json.len() > 16 * 1024 {
                return Err(JsValue::from_str("script argument byte limit"));
            }
            let ints: Vec<i32> = serde_json::from_str(ints_json)
                .map_err(|_| JsValue::from_str("invalid script integer arguments"))?;
            if ints.len() > 1000 {
                return Err(JsValue::from_str("script argument count limit"));
            }
            let report = self
                .0
                .run(openrs_cs2::Arguments {
                    ints: &ints,
                    ..Default::default()
                })
                .map_err(|error| JsValue::from_str(&error))?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("script execution serialization failed"))
        }
    }

    fn terrain_index(value: JsValue, max: u32) -> Result<u32, JsValue> {
        match value.as_f64() {
            Some(id)
                if id.is_finite() && id.fract() == 0.0 && (0.0..=f64::from(max)).contains(&id) =>
            {
                Ok(id as u32)
            }
            _ => Err(JsValue::from_str("invalid terrain coordinate")),
        }
    }

    #[wasm_bindgen(js_name = HslRgbLookup)]
    pub struct BrowserHslRgbLookup {
        random_unit: f64,
        lut: openrs_scene::rgb::HslRgbLut,
    }

    #[wasm_bindgen(js_class = HslRgbLookup)]
    impl BrowserHslRgbLookup {
        #[wasm_bindgen(constructor)]
        pub fn new(random_unit: JsValue) -> Result<BrowserHslRgbLookup, JsValue> {
            let random_unit = random_unit
                .as_f64()
                .ok_or_else(|| JsValue::from_str("invalid RGB random unit"))?;
            let lut = openrs_scene::rgb::HslRgbLut::from_random_unit(random_unit)
                .map_err(|_| JsValue::from_str("invalid RGB random unit"))?;
            Ok(Self { random_unit, lut })
        }

        pub fn report(&self) -> String {
            serde_json::json!({
                "scope": "hsl-rgb-lookup", "randomUnit": self.random_unit,
                "gamma": self.lut.gamma(), "gammaBits": format!("{:016x}", self.lut.gamma().to_bits()),
                "entries": openrs_scene::rgb::HSL_RGB_TABLE_LEN,
                "renderedWorld": false,
            }).to_string()
        }

        pub fn rgb(&self, hsl: JsValue) -> Result<u32, JsValue> {
            self.lut
                .rgb(terrain_index(hsl, u32::from(u16::MAX))? as i32)
                .ok_or_else(|| JsValue::from_str("invalid RGB index"))
        }

        #[wasm_bindgen(js_name = unsignedRgb)]
        pub fn unsigned_rgb(&self, hsl: JsValue) -> Result<u32, JsValue> {
            self.lut
                .unsigned_rgb(terrain_index(hsl, u32::from(u16::MAX))? as i32)
                .ok_or_else(|| JsValue::from_str("invalid RGB index"))
        }

        #[wasm_bindgen(js_name = rgbTable)]
        pub fn rgb_table(&self) -> Vec<u32> {
            self.lut.hsl_to_rgb().to_vec()
        }

        #[wasm_bindgen(js_name = unsignedRgbTable)]
        pub fn unsigned_rgb_table(&self) -> Vec<u32> {
            self.lut.unsigned_hsl_to_rgb().to_vec()
        }
    }

    #[wasm_bindgen(js_name = Js5WorldTerrainProbe)]
    pub struct BrowserWorldTerrainProbe {
        probe: WorldTerrainProbe,
        surface: Option<openrs_scene::surface::SurfaceHeights>,
        tiles: Option<openrs_scene::surface_tiles::SurfaceTiles>,
        palette: Option<openrs_scene::palette::UnderlayPalette>,
        ground: Option<(usize, openrs_scene::ground_geometry::GroundGeometry)>,
        lowered: Option<crate::lowering::LoweredMesh>,
        shadow_inputs: crate::shadows::ShadowInputs,
        shadows: Option<Vec<openrs_scene::static_shadows::ShadowGrid>>,
    }

    #[wasm_bindgen(js_class = Js5WorldTerrainProbe)]
    impl BrowserWorldTerrainProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str, regions_json: &str) -> Result<BrowserWorldTerrainProbe, JsValue> {
            if regions_json.len() > 512 {
                return Err(JsValue::from_str("terrain region byte limit"));
            }
            let regions: Vec<u16> = serde_json::from_str(regions_json)
                .map_err(|_| JsValue::from_str("invalid terrain regions"))?;
            WorldTerrainProbe::new(token.as_bytes(), &regions)
                .map(|probe| Self {
                    probe,
                    surface: None,
                    tiles: None,
                    palette: None,
                    ground: None,
                    lowered: None,
                    shadow_inputs: crate::shadows::ShadowInputs::default(),
                    shadows: None,
                })
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn underwater(
            token: &str,
            regions_json: &str,
        ) -> Result<BrowserWorldTerrainProbe, JsValue> {
            if regions_json.len() > 512 {
                return Err(JsValue::from_str("terrain region byte limit"));
            }
            let regions: Vec<u16> = serde_json::from_str(regions_json)
                .map_err(|_| JsValue::from_str("invalid terrain regions"))?;
            WorldTerrainProbe::new_underwater(token.as_bytes(), &regions)
                .map(|probe| Self {
                    probe,
                    surface: None,
                    tiles: None,
                    palette: None,
                    ground: None,
                    lowered: None,
                    shadow_inputs: crate::shadows::ShadowInputs::default(),
                    shadows: None,
                })
                .map_err(|error| JsValue::from_str(&error))
        }

        #[wasm_bindgen(js_name = composeUnderwater)]
        pub fn compose_underwater(
            &mut self,
            base_x: JsValue,
            base_y: JsValue,
            map_size: JsValue,
        ) -> Result<String, JsValue> {
            use openrs_scene::surface::{NativeMapSize, StandardMapLayout, SurfaceHeights};
            let base_tile = [
                terrain_index(base_x, 16384)? as i32,
                terrain_index(base_y, 16384)? as i32,
            ];
            let map_size = NativeMapSize::from_tiles(terrain_index(map_size, 168)? as usize)
                .map_err(|_| JsValue::from_str("invalid surface layout"))?;
            let report = self
                .probe
                .report()
                .ok_or_else(|| JsValue::from_str("world terrain not complete"))?;
            let ids: Vec<u16> = report
                .regions
                .iter()
                .map(|region| region.region_id)
                .collect();
            let layout = StandardMapLayout::new(base_tile, map_size, &ids)
                .map_err(|_| JsValue::from_str("invalid surface layout"))?;
            let mut present = Vec::new();
            let mut absent = Vec::new();
            for region in &report.regions {
                if region.present {
                    present.push((
                        region.region_id,
                        self.probe
                            .terrain(region.region_id)
                            .map_err(|_| JsValue::from_str("surface terrain unavailable"))?,
                    ));
                } else {
                    absent.push(region.region_id);
                }
            }
            let present_count = present.len();
            let surface = if present.is_empty() {
                None
            } else {
                Some(
                    SurfaceHeights::compose(&layout, &present, &absent)
                        .map_err(|_| JsValue::from_str("underwater surface composition failed"))?,
                )
            };
            let summary = serde_json::to_string(&serde_json::json!({
                "scope": "underwater-surface", "mapSize": map_size.tiles(),
                "presentRegions": present_count, "absentRegions": absent.len(),
                "renderedWorld": false,
            }))
            .map_err(|_| JsValue::from_str("underwater serialization failed"))?;
            self.surface = surface;
            self.tiles = None;
            self.palette = None;
            self.ground = None;
            self.lowered = None;
            Ok(summary)
        }

        #[wasm_bindgen(js_name = hasUnderwaterSurface)]
        pub fn has_underwater_surface(&self) -> bool {
            self.surface.is_some()
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.probe.take_outgoing()
        }
        pub fn complete(&self) -> bool {
            self.probe.complete()
        }
        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            let result = self.probe.feed(bytes);
            if result.is_err() {
                self.surface = None;
                self.tiles = None;
                self.palette = None;
                self.ground = None;
                self.lowered = None;
            }
            result.map_err(|error| JsValue::from_str(&error))
        }
        pub fn finish(&mut self) -> Result<(), JsValue> {
            let result = self.probe.finish();
            if result.is_err() {
                self.surface = None;
                self.tiles = None;
                self.palette = None;
                self.ground = None;
                self.lowered = None;
            }
            result.map_err(|error| JsValue::from_str(&error))
        }
        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .probe
                .report()
                .ok_or_else(|| JsValue::from_str("world terrain not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("world terrain serialization failed"))
        }
        pub fn height(
            &self,
            region: JsValue,
            plane: JsValue,
            x: JsValue,
            y: JsValue,
        ) -> Result<i32, JsValue> {
            self.probe
                .height(
                    terrain_index(region, u32::from(u16::MAX))? as u16,
                    terrain_index(plane, 3)? as usize,
                    terrain_index(x, 63)? as usize,
                    terrain_index(y, 63)? as usize,
                )
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn compose(
            &mut self,
            base_x: JsValue,
            base_y: JsValue,
            map_size: JsValue,
        ) -> Result<String, JsValue> {
            use openrs_scene::surface::{
                NativeMapSize, RegionAvailability, StandardMapLayout, SurfaceHeights,
            };
            let base_tile = [
                terrain_index(base_x, 16384)? as i32,
                terrain_index(base_y, 16384)? as i32,
            ];
            let map_size = NativeMapSize::from_tiles(terrain_index(map_size, 168)? as usize)
                .map_err(|_| JsValue::from_str("invalid surface layout"))?;
            let report = self
                .probe
                .report()
                .ok_or_else(|| JsValue::from_str("world terrain not complete"))?;
            let ids: Vec<u16> = report
                .regions
                .iter()
                .map(|region| region.region_id)
                .collect();
            let layout = StandardMapLayout::new(base_tile, map_size, &ids)
                .map_err(|_| JsValue::from_str("invalid surface layout"))?;
            let mut present = Vec::new();
            let mut absent = Vec::new();
            for region in &report.regions {
                if region.present {
                    present.push((
                        region.region_id,
                        self.probe
                            .terrain(region.region_id)
                            .map_err(|_| JsValue::from_str("surface terrain unavailable"))?,
                    ));
                } else {
                    absent.push(region.region_id);
                }
            }
            let surface = SurfaceHeights::compose(&layout, &present, &absent)
                .map_err(|_| JsValue::from_str("surface composition failed"))?;
            let tiles = crate::surface_palette::compose_surface_tiles(&self.probe, &surface)
                .map_err(|error| JsValue::from_str(&error))?;
            let height_count = surface.heights_native_units().len();
            let known_heights = surface
                .heights_native_units()
                .iter()
                .filter(|height| height.is_some())
                .count();
            let regions: Vec<_> = surface
                .region_availability()
                .iter()
                .map(|(id, availability)| {
                    let availability = match availability {
                        RegionAvailability::Present => "present",
                        RegionAvailability::ProvenAbsent => "provenAbsent",
                        RegionAvailability::Unavailable => "unavailable",
                    };
                    serde_json::json!({"regionId": id, "availability": availability})
                })
                .collect();
            let summary = serde_json::json!({
                "scope": "surface-only", "baseTile": base_tile,
                "mapSize": map_size.tiles(), "stride": map_size.tiles() + 1,
                "planes": 4, "heightCount": height_count,
                "knownHeights": known_heights, "unavailableHeights": height_count - known_heights,
                "regions": regions, "effectsDecoded": false, "renderedWorld": false,
            });
            let summary = serde_json::to_string(&summary)
                .map_err(|_| JsValue::from_str("surface serialization failed"))?;
            self.surface = Some(surface);
            self.tiles = Some(tiles);
            self.palette = None;
            self.ground = None;
            self.lowered = None;
            self.shadows = None;
            self.shadow_inputs.clear_placements();
            Ok(summary)
        }
        #[wasm_bindgen(js_name = sceneTile)]
        pub fn scene_tile(
            &self,
            plane: JsValue,
            x: JsValue,
            y: JsValue,
        ) -> Result<String, JsValue> {
            if !self.probe.complete() {
                return Err(JsValue::from_str("world terrain not complete"));
            }
            let tiles = self
                .tiles
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let size = tiles.layout().map_size().tiles() as u32;
            let plane = terrain_index(plane, 3)? as usize;
            let x = terrain_index(x, size - 1)? as usize;
            let y = terrain_index(y, size - 1)? as usize;
            let report = tiles.tile(plane, x, y).map(|tile| {
                serde_json::json!({
                    "underlayId": tile.underlay_id, "overlayId": tile.overlay_id,
                    "renderFlags": tile.render_flags, "overlayShape": tile.overlay_shape,
                    "overlayRotation": tile.overlay_rotation,
                })
            });
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("surface tile serialization failed"))
        }
        #[wasm_bindgen(js_name = composePalette)]
        pub fn compose_palette(
            &mut self,
            floors: &BrowserWorldFloorProbe,
        ) -> Result<String, JsValue> {
            use openrs_scene::palette::PaletteCell;
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let repository = floors
                .0
                .repository()
                .map_err(|error| JsValue::from_str(&error))?;
            let palette =
                crate::surface_palette::compose_underlay_palette(&self.probe, surface, repository)
                    .map_err(|error| JsValue::from_str(&error))?;
            let tile_count = palette.cells().len();
            let known_colors = palette
                .cells()
                .iter()
                .filter(|cell| matches!(cell, PaletteCell::Known(_)))
                .count();
            let summary = serde_json::json!({
                "scope": "underlay-packed-hsl", "mapSize": palette.map_size().tiles(),
                "planes": 4, "tileCount": tile_count, "knownColors": known_colors,
                "unavailableColors": tile_count - known_colors,
                "unavailableDefinitionIds": palette.unavailable_definition_ids(),
                "rgbPrepared": false, "renderedWorld": false,
            });
            let summary = serde_json::to_string(&summary)
                .map_err(|_| JsValue::from_str("underlay palette serialization failed"))?;
            self.palette = Some(palette);
            self.ground = None;
            self.lowered = None;
            Ok(summary)
        }
        #[wasm_bindgen(js_name = prepareGround)]
        pub fn prepare_ground(
            &mut self,
            floors: &BrowserWorldFloorProbe,
            materials: &BrowserWorldMaterialProbe,
            plane: JsValue,
            max_vertices: JsValue,
        ) -> Result<String, JsValue> {
            use crate::surface_ground::{SurfaceGroundInputs, compose_surface_ground};
            use openrs_scene::ground_geometry::{GroundGeometryOptions, MAX_OUTPUT_VERTICES};
            let plane = terrain_index(plane, 3)? as usize;
            let max_vertices = terrain_index(max_vertices, MAX_OUTPUT_VERTICES as u32)? as usize;
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let tiles = self
                .tiles
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let palette = self
                .palette
                .as_ref()
                .ok_or_else(|| JsValue::from_str("underlay palette not composed"))?;
            let ground = compose_surface_ground(
                SurfaceGroundInputs {
                    terrain: &self.probe,
                    surface,
                    tiles,
                    palette,
                    floors: &floors.0,
                    materials: &materials.0,
                },
                plane,
                GroundGeometryOptions::surface(max_vertices),
            )
            .map_err(|error| JsValue::from_str(&error))?;
            let emitted_vertices: usize =
                ground.tiles.iter().map(|tile| tile.emitted_vertices).sum();
            let summary = serde_json::json!({
                "scope": "blended-surface-geometry", "plane": plane,
                "mapSize": surface.layout().map_size().tiles(),
                "tileCount": ground.tiles.len(), "vertexCount": ground.vertex_count,
                "emittedVertexCount": emitted_vertices, "terrainLowered": false,
                "texturePixelsDecoded": false, "renderedWorld": false,
            });
            let summary = serde_json::to_string(&summary)
                .map_err(|_| JsValue::from_str("ground geometry serialization failed"))?;
            self.ground = Some((plane, ground));
            self.lowered = None;
            Ok(summary)
        }
        #[wasm_bindgen(js_name = addShadowLocs)]
        pub fn add_shadow_locs(
            &mut self,
            region_id: JsValue,
            data: &[u8],
            base_x: JsValue,
            base_y: JsValue,
        ) -> Result<u32, JsValue> {
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let size = surface.layout().map_size().tiles() as i32;
            let region_id = terrain_index(region_id, u32::from(u16::MAX))? as i32;
            let base_x = terrain_index(base_x, 16384)? as i32;
            let base_y = terrain_index(base_y, 16384)? as i32;
            self.shadows = None;
            self.shadow_inputs
                .add_region(region_id, data, base_x, base_y, size)
                .map(|count| count as u32)
                .map_err(|error| JsValue::from_str(&error))
        }

        #[wasm_bindgen(js_name = addShadowLocType)]
        pub fn add_shadow_loc_type(&mut self, id: JsValue, data: &[u8]) -> Result<(), JsValue> {
            let id = terrain_index(id, 1_000_000)? as i32;
            self.shadows = None;
            self.shadow_inputs
                .add_type(id, Some(data))
                .map_err(|error| JsValue::from_str(&error))
        }

        #[wasm_bindgen(js_name = addShadowLocModel)]
        pub fn add_shadow_loc_model(&mut self, id: JsValue, data: &[u8]) -> Result<(), JsValue> {
            let id = terrain_index(id, 1_000_000)? as i32;
            self.shadows = None;
            self.shadow_inputs
                .add_model(id, data.to_vec())
                .map_err(|error| JsValue::from_str(&error))
        }

        #[wasm_bindgen(js_name = bakeShadows)]
        pub fn bake_shadows(&mut self, textures_enabled: bool) -> Result<String, JsValue> {
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let size = surface.layout().map_size().tiles() as i32;
            let (grids, stats) = self.shadow_inputs.bake(size, textures_enabled);
            let non_zero: usize = grids.iter().map(|grid| grid.non_zero()).sum();
            let summary = serde_json::json!({
                "scope": "static-shadows", "mapSize": size, "planes": grids.len(),
                "placements": stats.placements, "missingType": stats.missing_type,
                "skippedTextures": stats.skipped_textures, "walls": stats.walls,
                "interacts": stats.interacts, "nonZeroCells": non_zero,
                "interactsOutOfScene": stats.interacts_out_of_scene,
                "animatedInteracts": stats.animated_interacts,
                "modelDisplacements": stats.model_displacements,
            });
            self.shadows = Some(grids);
            self.lowered = None;
            Ok(summary.to_string())
        }

        #[wasm_bindgen(js_name = lowerGround)]
        pub fn lower_ground(
            &mut self,
            rgb: &BrowserHslRgbLookup,
            materials: &BrowserWorldMaterialProbe,
        ) -> Result<String, JsValue> {
            self.lower_with(rgb, materials, None)
        }

        #[wasm_bindgen(js_name = lowerGroundLit)]
        pub fn lower_ground_lit(
            &mut self,
            rgb: &BrowserHslRgbLookup,
            materials: &BrowserWorldMaterialProbe,
            underwater: &BrowserWorldTerrainProbe,
        ) -> Result<String, JsValue> {
            if underwater.surface.is_none() {
                return Err(JsValue::from_str("underwater surface not composed"));
            }
            self.lower_with(rgb, materials, underwater.surface.as_ref())
        }
    }

    impl BrowserWorldTerrainProbe {
        fn lower_with(
            &mut self,
            rgb: &BrowserHslRgbLookup,
            materials: &BrowserWorldMaterialProbe,
            underwater_surface: Option<&openrs_scene::surface::SurfaceHeights>,
        ) -> Result<String, JsValue> {
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let (plane, ground) = self
                .ground
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface ground not prepared"))?;
            let table = materials
                .0
                .table()
                .map_err(|error| JsValue::from_str(&error))?;
            let baked = crate::shadows::plane_shadows(self.shadows.as_deref(), *plane);
            let mesh = crate::lowering::lower_plane_shadowed(
                surface,
                underwater_surface,
                ground,
                *plane,
                &rgb.lut,
                table,
                baked,
            )
            .map_err(|error| JsValue::from_str(&error))?;
            let summary = serde_json::to_string(&mesh.report)
                .map_err(|_| JsValue::from_str("ground lowering serialization failed"))?;
            self.lowered = Some(mesh);
            Ok(summary)
        }
    }

    #[wasm_bindgen(js_class = Js5WorldTerrainProbe)]
    impl BrowserWorldTerrainProbe {
        #[wasm_bindgen(js_name = loweredVertices)]
        pub fn lowered_vertices(&self, material: JsValue) -> Result<Vec<u8>, JsValue> {
            let mesh = self
                .lowered
                .as_ref()
                .ok_or_else(|| JsValue::from_str("ground not lowered"))?;
            let index = terrain_index(material, mesh.materials.len().saturating_sub(1) as u32)?;
            mesh.materials
                .get(index as usize)
                .map(|entry| entry.vertices.clone())
                .ok_or_else(|| JsValue::from_str("lowered material out of bounds"))
        }
        #[wasm_bindgen(js_name = loweredIndices)]
        pub fn lowered_indices(&self, material: JsValue) -> Result<Vec<u32>, JsValue> {
            let mesh = self
                .lowered
                .as_ref()
                .ok_or_else(|| JsValue::from_str("ground not lowered"))?;
            let index = terrain_index(material, mesh.materials.len().saturating_sub(1) as u32)?;
            mesh.materials
                .get(index as usize)
                .map(|entry| entry.indices.clone())
                .ok_or_else(|| JsValue::from_str("lowered material out of bounds"))
        }
        #[wasm_bindgen(js_name = groundTile)]
        pub fn ground_tile(
            &self,
            plane: JsValue,
            x: JsValue,
            y: JsValue,
        ) -> Result<String, JsValue> {
            if !self.probe.complete() {
                return Err(JsValue::from_str("world terrain not complete"));
            }
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let size = surface.layout().map_size().tiles() as u32;
            let plane = terrain_index(plane, 3)? as usize;
            let x = terrain_index(x, size - 1)? as usize;
            let y = terrain_index(y, size - 1)? as usize;
            let (prepared_plane, ground) = self
                .ground
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface ground not prepared"))?;
            if *prepared_plane != plane {
                return Err(JsValue::from_str("surface ground plane not prepared"));
            }
            let report = ground
                .tiles
                .binary_search_by_key(&(x, y), |tile| (tile.x, tile.y))
                .ok()
                .map(|index| {
                    let tile = &ground.tiles[index];
                    let vertices: Vec<_> = tile
                        .vertices
                        .iter()
                        .map(|vertex| {
                            serde_json::json!({
                                "offsetX": vertex.offset_x, "offsetZ": vertex.offset_z,
                                "mainColor": vertex.main_color, "texture": vertex.texture,
                                "textureScale": vertex.texture_scale,
                            })
                        })
                        .collect();
                    serde_json::json!({
                        "x": tile.x, "y": tile.y, "shape": tile.shape,
                        "rotation": tile.rotation, "splitEdges": tile.split_edges,
                        "vertices": vertices, "blendingColors": tile.blending_colors,
                        "emittedVertices": tile.emitted_vertices, "hasShadows": tile.has_shadows,
                    })
                });
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("ground tile serialization failed"))
        }
        #[wasm_bindgen(js_name = paletteColor)]
        pub fn palette_color(
            &self,
            plane: JsValue,
            x: JsValue,
            y: JsValue,
        ) -> Result<JsValue, JsValue> {
            use openrs_scene::palette::PaletteCell;
            if !self.probe.complete() {
                return Err(JsValue::from_str("world terrain not complete"));
            }
            let palette = self
                .palette
                .as_ref()
                .ok_or_else(|| JsValue::from_str("underlay palette not composed"))?;
            let size = palette.map_size().tiles() as u32;
            let plane = terrain_index(plane, 3)? as usize;
            let x = terrain_index(x, size - 1)? as usize;
            let y = terrain_index(y, size - 1)? as usize;
            match palette.cell(plane, x, y) {
                Some(PaletteCell::Known(color)) => Ok(JsValue::from(color)),
                Some(PaletteCell::Unavailable) => Ok(JsValue::NULL),
                None => Err(JsValue::from_str("invalid palette coordinate")),
            }
        }
        #[wasm_bindgen(js_name = sceneHeight)]
        pub fn scene_height(
            &self,
            plane: JsValue,
            x: JsValue,
            y: JsValue,
        ) -> Result<JsValue, JsValue> {
            if !self.probe.complete() {
                return Err(JsValue::from_str("world terrain not complete"));
            }
            let surface = self
                .surface
                .as_ref()
                .ok_or_else(|| JsValue::from_str("surface not composed"))?;
            let size = surface.layout().map_size().tiles() as u32;
            let plane = terrain_index(plane, 3)? as usize;
            let x = terrain_index(x, size)? as usize;
            let y = terrain_index(y, size)? as usize;
            Ok(surface
                .height_native_units(plane, x, y)
                .map_or(JsValue::NULL, JsValue::from))
        }
    }

    #[wasm_bindgen(js_name = Js5WorldMaterialProbe)]
    pub struct BrowserWorldMaterialProbe(crate::materials::WorldMaterialProbe);

    #[wasm_bindgen(js_class = Js5WorldMaterialProbe)]
    impl BrowserWorldMaterialProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str) -> Result<BrowserWorldMaterialProbe, JsValue> {
            crate::materials::WorldMaterialProbe::new(token.as_bytes())
                .map(Self)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.take_outgoing()
        }

        pub fn complete(&self) -> bool {
            self.0.complete()
        }

        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .feed(bytes)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|error| JsValue::from_str(&error))
        }

        pub fn report(&self) -> Result<String, JsValue> {
            let asset = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("material probe not complete"))?;
            let table = self.0.table().map_err(|error| JsValue::from_str(&error))?;
            let present = table.slots().iter().filter(|slot| slot.is_some()).count();
            serde_json::to_string(&serde_json::json!({
                "scope": "material-metadata", "asset": asset,
                "slots": table.len(), "present": present,
                "texturePixelsDecoded": false, "renderedWorld": false,
            }))
            .map_err(|_| JsValue::from_str("material report serialization failed"))
        }

        pub fn material(&self, id: JsValue) -> Result<String, JsValue> {
            let id = terrain_index(id, u32::from(u16::MAX))? as i32;
            let d = self
                .0
                .material(id)
                .map_err(|error| JsValue::from_str(&error))?;
            serde_json::to_string(&serde_json::json!({
                "id": id, "detailsOnly": d.details_only, "isSmall": d.is_small,
                "skipTriangles": d.skip_triangles, "brightness": d.brightness,
                "alpha": d.alpha, "effectId": d.effect_id, "effectParam1": d.effect_param1,
                "colorHsl": d.color_hsl, "textureSpeedU": d.texture_speed_u,
                "textureSpeedV": d.texture_speed_v, "aBool2087": d.a_bool2087,
                "isBrickTile": d.is_brick_tile, "mipmapping": d.mipmapping,
                "repeatS": d.repeat_s, "repeatT": d.repeat_t, "hdr": d.hdr,
                "combineMode": d.combine_mode, "effectParam2": d.effect_param2,
                "effectCombiner": d.effect_combiner,
            }))
            .map_err(|_| JsValue::from_str("material serialization failed"))
        }
    }

    #[wasm_bindgen(js_name = Js5WorldFloorProbe)]
    pub struct BrowserWorldFloorProbe(crate::floors::WorldFloorProbe);

    #[wasm_bindgen(js_class = Js5WorldFloorProbe)]
    impl BrowserWorldFloorProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str) -> Result<BrowserWorldFloorProbe, JsValue> {
            crate::floors::WorldFloorProbe::new(token.as_bytes())
                .map(Self)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.take_outgoing()
        }
        pub fn complete(&self) -> bool {
            self.0.complete()
        }
        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .feed(bytes)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|error| JsValue::from_str(&error))
        }
        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("floor definitions not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("floor definitions serialization failed"))
        }
        pub fn underlay(&self, id: JsValue) -> Result<String, JsValue> {
            let id = terrain_index(id, u32::from(u16::MAX))?;
            let repository = self
                .0
                .repository()
                .map_err(|error| JsValue::from_str(&error))?;
            let lookup = repository
                .underlay(id)
                .map_err(|error| JsValue::from_str(&error))?;
            let d = lookup.definition();
            let report = serde_json::json!({
                "id": id, "present": lookup.present(), "rgb": d.rgb,
                "texture": d.texture, "scale": d.scale, "shadowed": d.shadowed,
                "occlude": d.occlude, "hue": d.hue, "saturation": d.saturation,
                "lightness": d.lightness, "hueMultiplier": d.hue_multiplier,
            });
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("underlay serialization failed"))
        }
        pub fn overlay(&self, id: JsValue) -> Result<String, JsValue> {
            let id = terrain_index(id, u32::from(u16::MAX))?;
            let repository = self
                .0
                .repository()
                .map_err(|error| JsValue::from_str(&error))?;
            let lookup = repository
                .overlay(id)
                .map_err(|error| JsValue::from_str(&error))?;
            let d = lookup.definition();
            let report = serde_json::json!({
                "id": id, "present": lookup.present(), "overlayId": d.overlay_id,
                "slot": d.slot, "tileColorHsl": d.tile_color_hsl,
                "minimapColorHsl": d.minimap_color_hsl, "texture": d.texture,
                "textureScale": d.texture_scale, "occlude": d.occlude,
                "shadowed": d.shadowed, "blendsWithUnderlay": d.blends_with_underlay,
                "waterColor": d.water_color, "waterFogDepth": d.water_fog_depth,
                "waterIntensity": d.water_intensity, "opcode20": d.opcode20,
                "unusedOpcode21": d.unused_opcode21, "unusedOpcode22": d.unused_opcode22,
                "sawOpcode8": d.saw_opcode8,
            });
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("overlay serialization failed"))
        }
    }

    #[wasm_bindgen(js_name = Js5TextureSet)]
    pub struct BrowserTextureSet(crate::textures::TextureSet);

    #[wasm_bindgen(js_class = Js5TextureSet)]
    impl BrowserTextureSet {
        #[wasm_bindgen(constructor)]
        pub fn new() -> BrowserTextureSet {
            Self(crate::textures::TextureSet::new())
        }
        #[wasm_bindgen(js_name = setTable)]
        pub fn set_table(&mut self, bytes: &[u8]) -> Result<u32, JsValue> {
            self.0
                .set_table(bytes)
                .map(|count| count as u32)
                .map_err(|error| JsValue::from_str(&error))
        }
        #[wasm_bindgen(js_name = addSprite)]
        pub fn add_sprite(&mut self, id: JsValue, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .add_sprite(terrain_index(id, 0x00ff_ffff)?, bytes)
                .map_err(|error| JsValue::from_str(&error))
        }
        #[wasm_bindgen(js_name = addGraph)]
        pub fn add_graph(&mut self, id: JsValue, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .add_graph(terrain_index(id, 0x00ff_ffff)?, bytes)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn needs(&self, material: JsValue) -> Result<String, JsValue> {
            let needs = self
                .0
                .needs(terrain_index(material, 0x00ff_ffff)?)
                .map_err(|error| JsValue::from_str(&error))?;
            Ok(serde_json::json!({"sprites": needs.sprites, "graphs": needs.graphs}).to_string())
        }
        pub fn size(&self, material: JsValue) -> Result<u32, JsValue> {
            self.0
                .size(terrain_index(material, 0x00ff_ffff)?)
                .map(|size| size as u32)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn render(&self, material: JsValue) -> Result<Vec<u8>, JsValue> {
            self.0
                .render_rgba(terrain_index(material, 0x00ff_ffff)?)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn report(&self) -> String {
            serde_json::json!({
                "scope": "material-texture-pixels", "hasTable": self.0.has_table(),
                "sprites": self.0.sprite_count(), "graphs": self.0.graph_count(),
                "renderedWorld": false,
            })
            .to_string()
        }
    }

    #[wasm_bindgen(js_name = Js5AssetProbe)]
    pub struct BrowserAssetProbe(AssetProbe);

    #[wasm_bindgen(js_class = Js5AssetProbe)]
    impl BrowserAssetProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str, index: u32, archive: u32) -> Result<BrowserAssetProbe, JsValue> {
            AssetProbe::new(token.as_bytes(), index, archive)
                .map(Self)
                .map_err(|error| JsValue::from_str(&error))
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.take_outgoing()
        }
        pub fn complete(&self) -> bool {
            self.0.complete()
        }
        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0
                .feed(bytes)
                .map_err(|error| JsValue::from_str(&error))
        }
        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|error| JsValue::from_str(&error))
        }
        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("asset archive not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("asset report serialization failed"))
        }
        pub fn file(&self, id: u32) -> Result<Vec<u8>, JsValue> {
            self.0
                .file(id)
                .map(Vec::from)
                .map_err(|error| JsValue::from_str(&error))
        }
    }

    #[wasm_bindgen(js_name = Js5Probe)]
    pub struct BrowserProbe(Probe);

    #[wasm_bindgen(js_class = Js5Probe)]
    impl BrowserProbe {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str) -> Result<BrowserProbe, JsValue> {
            Probe::new(token.as_bytes())
                .map(Self)
                .map_err(|e| JsValue::from_str(&e))
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.take_outgoing()
        }
        pub fn complete(&self) -> bool {
            self.0.complete()
        }
        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0.feed(bytes).map_err(|e| JsValue::from_str(&e))
        }
        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.finish().map_err(|e| JsValue::from_str(&e))
        }
        pub fn report(&self) -> Result<String, JsValue> {
            let report = self
                .0
                .report()
                .ok_or_else(|| JsValue::from_str("version table not complete"))?;
            serde_json::to_string(report)
                .map_err(|_| JsValue::from_str("report serialization failed"))
        }
    }
}

#[cfg(test)]
mod tests;
