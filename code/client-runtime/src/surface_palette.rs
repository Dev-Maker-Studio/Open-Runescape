use crate::WorldTerrainProbe;
use crate::floors::{FloorLookup, FloorRepository};
use openrs_scene::palette::{UnderlayLookup, UnderlayPalette, calculate_underlay_palette};
use openrs_scene::surface::{RegionAvailability, SurfaceHeights};
use openrs_scene::surface_tiles::SurfaceTiles;

pub fn compose_underlay_palette(
    terrain: &WorldTerrainProbe,
    surface: &SurfaceHeights,
    floors: &FloorRepository,
) -> Result<UnderlayPalette, String> {
    if !terrain.complete() {
        return Err("world terrain not complete".into());
    }
    let ids = scene_underlay_ids(terrain, surface)?;
    calculate_underlay_palette(surface.layout().map_size(), &ids, |id| {
        match floors.underlay(u32::from(id)) {
            Ok(FloorLookup::Present(definition)) => UnderlayLookup::Present(definition),
            Ok(FloorLookup::Default(_)) => UnderlayLookup::ProvenAbsent,
            Err(_) => UnderlayLookup::Unavailable,
        }
    })
    .map_err(|_| "underlay palette composition failed".into())
}

pub fn scene_underlay_ids(
    terrain: &WorldTerrainProbe,
    surface: &SurfaceHeights,
) -> Result<Vec<Option<u8>>, String> {
    Ok(compose_surface_tiles(terrain, surface)?
        .tiles()
        .iter()
        .map(|tile| tile.map(|tile| tile.underlay_id))
        .collect())
}

pub fn compose_surface_tiles(
    terrain: &WorldTerrainProbe,
    surface: &SurfaceHeights,
) -> Result<SurfaceTiles, String> {
    let report = terrain
        .report()
        .ok_or_else(|| "world terrain not complete".to_string())?;
    if report.regions.len() != surface.region_availability().len()
        || report
            .regions
            .iter()
            .zip(surface.region_availability())
            .any(|(region, &(id, availability))| {
                region.region_id != id
                    || region.present != (availability == RegionAvailability::Present)
            })
    {
        return Err("surface terrain identities do not match".into());
    }
    let present = report
        .regions
        .iter()
        .filter(|region| region.present)
        .map(|region| Ok((region.region_id, terrain.terrain(region.region_id)?)))
        .collect::<Result<Vec<_>, String>>()?;
    SurfaceTiles::compose(surface, &present).map_err(|_| "surface tile composition failed".into())
}
