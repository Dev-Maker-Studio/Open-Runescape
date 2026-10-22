use crate::WorldTerrainProbe;
use crate::floors::WorldFloorProbe;
use crate::materials::WorldMaterialProbe;
use crate::surface_palette::{compose_surface_tiles, compose_underlay_palette};
use openrs_scene::floor::Overlay;
use openrs_scene::ground_geometry::{
    GroundGeometry, GroundGeometryInput, GroundGeometryOptions, build_blended_ground,
};
use openrs_scene::material::MaterialTable;
use openrs_scene::palette::UnderlayPalette;
use openrs_scene::surface::SurfaceHeights;
use openrs_scene::surface_tiles::SurfaceTiles;
use openrs_scene::terrain::PLANES;

#[derive(Clone, Copy)]
pub struct SurfaceGroundInputs<'a> {
    pub terrain: &'a WorldTerrainProbe,
    pub surface: &'a SurfaceHeights,
    pub tiles: &'a SurfaceTiles,
    pub palette: &'a UnderlayPalette,
    pub floors: &'a WorldFloorProbe,
    pub materials: &'a WorldMaterialProbe,
}

pub fn compose_surface_ground(
    inputs: SurfaceGroundInputs<'_>,
    plane: usize,
    options: GroundGeometryOptions,
) -> Result<GroundGeometry, String> {
    if plane >= PLANES {
        return Err("ground geometry plane out of bounds".into());
    }
    if options.overlay_hidden {
        return Err("underwater ground geometry is unsupported".into());
    }
    if options.secondary_ground_heights {
        return Err("secondary ground height adjustments are unsupported".into());
    }
    if !inputs.terrain.complete() {
        return Err("world terrain not complete".into());
    }
    let floors = inputs.floors.repository()?;
    let materials = inputs.materials.table()?;
    if inputs.surface.layout() != inputs.tiles.layout() {
        return Err("ground surface tile layout does not match heights".into());
    }
    if inputs.palette.map_size() != inputs.surface.layout().map_size() {
        return Err("ground palette dimensions do not match surface".into());
    }
    if compose_surface_tiles(inputs.terrain, inputs.surface)? != *inputs.tiles {
        return Err("ground surface tiles do not match verified terrain".into());
    }
    let regions = &inputs
        .terrain
        .report()
        .ok_or_else(|| "world terrain not complete".to_string())?
        .regions;
    let present = regions
        .iter()
        .filter(|region| region.present)
        .map(|region| Ok((region.region_id, inputs.terrain.terrain(region.region_id)?)))
        .collect::<Result<Vec<_>, String>>()?;
    let absent: Vec<_> = regions
        .iter()
        .filter(|region| !region.present)
        .map(|region| region.region_id)
        .collect();
    let expected_surface = SurfaceHeights::compose(inputs.surface.layout(), &present, &absent)
        .map_err(|error| error.to_string())?;
    if expected_surface != *inputs.surface {
        return Err("ground surface heights do not match verified terrain".into());
    }
    if compose_underlay_palette(inputs.terrain, inputs.surface, floors)? != *inputs.palette {
        return Err("ground palette does not match verified terrain and floors".into());
    }
    let size = inputs.surface.layout().map_size().tiles();
    let cells = size * size;
    let points = (size + 1) * (size + 1);
    let tiles = &inputs.tiles.tiles()[plane * cells..(plane + 1) * cells];
    let palette = &inputs.palette.cells()[plane * cells..(plane + 1) * cells];
    let heights = &inputs.surface.heights_native_units()[plane * points..(plane + 1) * points];
    let mut underlays = vec![None; 255];
    let mut overlays = vec![None; 255];
    let mut rendered = vec![None; 255];
    for tile in tiles.iter().flatten() {
        if let Some(id) = tile.underlay_id.checked_sub(1) {
            let index = usize::from(id);
            if underlays[index].is_none() {
                underlays[index] = Some(*floors.underlay(u32::from(id))?.definition());
            }
        }
        if let Some(id) = tile.overlay_id.checked_sub(1) {
            let index = usize::from(id);
            if overlays[index].is_none() {
                let overlay = *floors.overlay(u32::from(id))?.definition();
                rendered[index] = Some(
                    if overlay.tile_color_hsl == -1 && overlay.minimap_color_hsl == -1 {
                        -1
                    } else {
                        overlay_render_color(&overlay, materials)?
                    },
                );
                overlays[index] = Some(overlay);
            }
        }
    }
    build_blended_ground(
        GroundGeometryInput {
            width: size,
            height: size,
            tiles,
            ground_heights: heights,
            palette,
            underlays: &underlays,
            overlays: &overlays,
            overlay_render_colors: &rendered,
        },
        options,
    )
    .map_err(|error| error.to_string())
}

pub fn overlay_render_color(overlay: &Overlay, materials: &MaterialTable) -> Result<i32, String> {
    if overlay.minimap_color_hsl != -1 {
        return Ok(overlay.minimap_color_hsl);
    }
    if overlay.texture != -1 {
        let material = materials
            .get_material(overlay.texture)
            .map_err(|error| error.to_string())?;
        if !material.details_only {
            return Ok(i32::from(material.color_hsl));
        }
    }
    Ok(overlay.tile_color_hsl)
}
