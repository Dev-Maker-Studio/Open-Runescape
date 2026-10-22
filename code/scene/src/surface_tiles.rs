use crate::surface::{RegionAvailability, StandardMapLayout, SurfaceHeights};
use crate::terrain::{Terrain, PLANES};
use std::fmt;

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct SurfaceTile {
    pub underlay_id: u8,
    pub overlay_id: u8,
    pub render_flags: u8,
    pub overlay_shape: u8,
    pub overlay_rotation: u8,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SurfaceTilesError {
    UnexpectedRegion(u16),
    DuplicateRegion(u16),
    AvailabilityMismatch(u16),
}

impl fmt::Display for SurfaceTilesError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::UnexpectedRegion(id) => write!(f, "unexpected surface tile region {id}"),
            Self::DuplicateRegion(id) => write!(f, "duplicate surface tile region {id}"),
            Self::AvailabilityMismatch(id) => {
                write!(f, "surface tile region availability mismatch {id}")
            }
        }
    }
}

impl std::error::Error for SurfaceTilesError {}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SurfaceTiles {
    layout: StandardMapLayout,
    tiles: Box<[Option<SurfaceTile>]>,
}

impl SurfaceTiles {
    pub fn compose(
        surface: &SurfaceHeights,
        present: &[(u16, &Terrain)],
    ) -> Result<Self, SurfaceTilesError> {
        let mut sources = vec![None; surface.region_availability().len()];
        for &(id, terrain) in present {
            let index = surface
                .region_availability()
                .iter()
                .position(|&(region, _)| region == id)
                .ok_or(SurfaceTilesError::UnexpectedRegion(id))?;
            if sources[index].is_some() {
                return Err(SurfaceTilesError::DuplicateRegion(id));
            }
            if surface.region_availability()[index].1 != RegionAvailability::Present {
                return Err(SurfaceTilesError::AvailabilityMismatch(id));
            }
            sources[index] = Some(terrain);
        }
        for (&(id, availability), source) in surface.region_availability().iter().zip(&sources) {
            if (availability == RegionAvailability::Present) != source.is_some() {
                return Err(SurfaceTilesError::AvailabilityMismatch(id));
            }
        }
        let layout = surface.layout().clone();
        let size = layout.map_size().tiles();
        let mut tiles = vec![None; PLANES * size * size];
        for x in 0..size {
            for y in 0..size {
                let source = surface.source_tile(x, y).unwrap();
                let region = surface
                    .region_availability()
                    .iter()
                    .position(|&(id, _)| id == source.region_id)
                    .unwrap();
                for plane in 0..PLANES {
                    tiles[(plane * size + x) * size + y] = match sources[region] {
                        Some(terrain) => {
                            let tile = terrain
                                .tile(plane, source.region_tile[0], source.region_tile[1])
                                .unwrap();
                            Some(SurfaceTile {
                                underlay_id: tile.underlay_id,
                                overlay_id: tile.overlay_id,
                                render_flags: tile.render_flags,
                                overlay_shape: tile.overlay_shape,
                                overlay_rotation: tile.overlay_rotation,
                            })
                        }
                        None if surface.region_availability()[region].1
                            == RegionAvailability::ProvenAbsent =>
                        {
                            Some(SurfaceTile::default())
                        }
                        None => None,
                    };
                }
            }
        }
        Ok(Self {
            layout,
            tiles: tiles.into_boxed_slice(),
        })
    }

    pub fn layout(&self) -> &StandardMapLayout {
        &self.layout
    }

    pub fn tiles(&self) -> &[Option<SurfaceTile>] {
        &self.tiles
    }

    pub fn tile(&self, plane: usize, x: usize, y: usize) -> Option<&SurfaceTile> {
        let size = self.layout.map_size().tiles();
        if plane < PLANES && x < size && y < size {
            self.tiles[(plane * size + x) * size + y].as_ref()
        } else {
            None
        }
    }
}
