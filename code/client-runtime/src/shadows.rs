use openrs_scene::loc_size::Size2dResolver;
use openrs_scene::loc_type::{LocType, decode_loc_type};
use openrs_scene::locs::{ScenePlacement, decode_region_locs, place_in_scene};
use openrs_scene::static_shadows::{BakeStats, ShadowGrid, bake_static_shadows};
use openrs_scene::terrain::PLANES;
use std::collections::HashMap;

const MAX_PLACEMENTS: usize = 2_000_000;
const MAX_TYPES: usize = 200_000;
const MAX_MODELS: usize = 200_000;

#[derive(Default)]
pub struct ShadowInputs {
    placements: Vec<ScenePlacement>,
    types: HashMap<i32, LocType>,
    models: HashMap<i32, Vec<u8>>,
}

impl ShadowInputs {
    pub fn placement_count(&self) -> usize {
        self.placements.len()
    }

    pub fn clear_placements(&mut self) {
        self.placements.clear();
    }

    pub fn add_region(
        &mut self,
        region_id: i32,
        data: &[u8],
        base_x: i32,
        base_y: i32,
        scene_size: i32,
    ) -> Result<usize, String> {
        let decoded =
            decode_region_locs(data).map_err(|_| "invalid region loc placements".to_string())?;
        let placed = place_in_scene(region_id, &decoded, base_x, base_y, scene_size);
        if self.placements.len() + placed.len() > MAX_PLACEMENTS {
            return Err("loc placement limit".into());
        }
        let count = placed.len();
        self.placements.extend(placed);
        Ok(count)
    }

    pub fn add_type(&mut self, id: i32, data: Option<&[u8]>) -> Result<(), String> {
        if self.types.len() >= MAX_TYPES && !self.types.contains_key(&id) {
            return Err("loc type limit".into());
        }
        let loc = decode_loc_type(id, data, true)
            .map_err(|_| "invalid loc type definition".to_string())?;
        self.types.insert(id, loc);
        Ok(())
    }

    pub fn add_model(&mut self, id: i32, bytes: Vec<u8>) -> Result<(), String> {
        if self.models.len() >= MAX_MODELS && !self.models.contains_key(&id) {
            return Err("loc model limit".into());
        }
        self.models.insert(id, bytes);
        Ok(())
    }

    pub fn bake(&self, scene_size: i32, textures_enabled: bool) -> (Vec<ShadowGrid>, BakeStats) {
        let sizes = Size2dResolver::new(|id| self.models.get(&id).map(Vec::as_slice));
        bake_static_shadows(
            PLANES,
            scene_size,
            &self.placements,
            |id| self.types.get(&id),
            |placement, loc| sizes.size2d(placement, loc),
            textures_enabled,
        )
    }
}

pub fn plane_shadows(grids: Option<&[ShadowGrid]>, plane: usize) -> Option<&ShadowGrid> {
    grids.and_then(|grids| grids.get(plane))
}

#[cfg(test)]
mod tests {
    use super::*;
    use openrs_scene::static_shadows::WALL_SHADOW;

    fn region(id_offset: u8, entries: &[(i32, i32, i32, u8)]) -> Vec<u8> {
        let mut out = vec![id_offset];
        let mut packed = 0i32;
        for (plane, x, y, description) in entries {
            let value = (plane << 12) | (x << 6) | y;
            let delta = value - packed + 1;
            packed = value;
            if delta < 128 {
                out.push(delta as u8);
            } else {
                out.extend_from_slice(&((delta + 32768) as u16).to_be_bytes());
            }
            out.push(*description);
        }
        out.push(0);
        out.push(0);
        out
    }

    #[test]
    fn walls_bake_into_the_requested_plane_only() {
        let mut inputs = ShadowInputs::default();
        inputs.add_type(5, Some(&[0])).unwrap();
        let data = region(6, &[(1, 10, 20, 0)]);
        assert_eq!(inputs.add_region(0, &data, 0, 0, 72).unwrap(), 1);
        let (grids, stats) = inputs.bake(72, true);
        assert_eq!(grids.len(), 4);
        assert_eq!(stats.walls, 1);
        assert_eq!(grids[1].get(10, 20), WALL_SHADOW);
        assert_eq!(grids[1].get(10, 21), WALL_SHADOW);
        assert_eq!(grids[0].non_zero(), 0);
        assert_eq!(grids[1].non_zero(), 2);
        assert!(plane_shadows(Some(&grids), 1).is_some());
        assert!(plane_shadows(Some(&grids), 4).is_none());
        assert!(plane_shadows(None, 0).is_none());
    }

    #[test]
    fn missing_types_and_placements_outside_the_scene_are_skipped() {
        let mut inputs = ShadowInputs::default();
        let data = region(6, &[(0, 10, 20, 0)]);
        inputs.add_region(0, &data, 0, 0, 72).unwrap();
        let (grids, stats) = inputs.bake(72, true);
        assert_eq!(stats.missing_type, 1);
        assert!(grids.iter().all(|grid| grid.non_zero() == 0));
        assert_eq!(inputs.add_region(0, &data, 1000, 1000, 72).unwrap(), 0);
        assert_eq!(inputs.placement_count(), 1);
        inputs.clear_placements();
        assert_eq!(inputs.placement_count(), 0);
    }

    #[test]
    fn invalid_inputs_are_rejected() {
        let mut inputs = ShadowInputs::default();
        assert!(inputs.add_region(0, &[6], 0, 0, 72).is_err());
        assert!(inputs.add_type(1, Some(&[14])).is_err());
    }

    #[test]
    fn interact_shadow_grows_with_the_scene_bake() {
        let mut inputs = ShadowInputs::default();
        inputs.add_type(5, Some(&[0])).unwrap();
        let data = region(6, &[(0, 10, 20, 10 << 2)]);
        inputs.add_region(0, &data, 0, 0, 72).unwrap();
        let (grids, stats) = inputs.bake(72, true);
        assert_eq!(stats.interacts, 1);
        assert_eq!(grids[0].get(10, 20), 15);
        assert_eq!(grids[0].get(11, 21), 15);
        assert_eq!(grids[0].get(12, 21), 0);
    }
}
