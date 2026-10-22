use crate::terrain::{Terrain, MAP_SIZE, PLANES};
use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum NativeMapSize {
    Tiles72,
    Tiles104,
    Tiles120,
    Tiles136,
    Tiles168,
}

impl NativeMapSize {
    pub fn from_tiles(tiles: usize) -> Result<Self, SurfaceError> {
        match tiles {
            72 => Ok(Self::Tiles72),
            104 => Ok(Self::Tiles104),
            120 => Ok(Self::Tiles120),
            136 => Ok(Self::Tiles136),
            168 => Ok(Self::Tiles168),
            _ => Err(SurfaceError::UnsupportedMapSize { tiles }),
        }
    }

    pub fn tiles(self) -> usize {
        match self {
            Self::Tiles72 => 72,
            Self::Tiles104 => 104,
            Self::Tiles120 => 120,
            Self::Tiles136 => 136,
            Self::Tiles168 => 168,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum SurfaceError {
    UnsupportedMapSize {
        tiles: usize,
    },
    UnalignedBase {
        base_tile: [i32; 2],
    },
    UnsupportedWorldBounds {
        base_tile: [i32; 2],
    },
    RegionCount {
        expected: usize,
        actual: usize,
    },
    RegionOrder {
        index: usize,
        expected: u16,
        actual: u16,
    },
    UnexpectedRegion {
        region_id: u16,
    },
    DuplicateRegion {
        region_id: u16,
    },
}

impl Display for SurfaceError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        match self {
            Self::UnsupportedMapSize { tiles } => write!(f, "unsupported native map size {tiles}"),
            Self::UnalignedBase { base_tile } => {
                write!(
                    f,
                    "standard-map base {base_tile:?} is not aligned to eight tiles"
                )
            }
            Self::UnsupportedWorldBounds { base_tile } => {
                write!(
                    f,
                    "standard-map base {base_tile:?} exceeds supported unsigned region bounds"
                )
            }
            Self::RegionCount { expected, actual } => {
                write!(
                    f,
                    "standard map requires {expected} ordered region IDs, received {actual}"
                )
            }
            Self::RegionOrder {
                index,
                expected,
                actual,
            } => {
                write!(
                    f,
                    "standard region {index} must be {expected}, received {actual}"
                )
            }
            Self::UnexpectedRegion { region_id } => {
                write!(f, "region {region_id} is outside the standard map")
            }
            Self::DuplicateRegion { region_id } => {
                write!(f, "region {region_id} was classified more than once")
            }
        }
    }
}

impl Error for SurfaceError {}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct StandardMapLayout {
    base_tile: [i32; 2],
    map_size: NativeMapSize,
    center_chunk: [i32; 2],
    region_ids: Box<[u16]>,
}

impl StandardMapLayout {
    pub fn new(
        base_tile: [i32; 2],
        map_size: NativeMapSize,
        expected_region_ids: &[u16],
    ) -> Result<Self, SurfaceError> {
        if base_tile.iter().any(|coordinate| coordinate % 8 != 0) {
            return Err(SurfaceError::UnalignedBase { base_tile });
        }
        let size = map_size.tiles() as i32;
        if base_tile
            .iter()
            .any(|&coordinate| coordinate < 0 || coordinate > 16384 - size)
        {
            return Err(SurfaceError::UnsupportedWorldBounds { base_tile });
        }
        let half_chunks = size >> 4;
        let center_chunk = base_tile.map(|coordinate| coordinate / 8 + half_chunks);
        let first_region = center_chunk.map(|coordinate| (coordinate - half_chunks) / 8);
        let last_region = center_chunk.map(|coordinate| (coordinate + half_chunks) / 8);
        if last_region.iter().any(|&coordinate| coordinate > 255) {
            return Err(SurfaceError::UnsupportedWorldBounds { base_tile });
        }
        let mut region_ids = Vec::new();
        for x in first_region[0]..=last_region[0] {
            for y in first_region[1]..=last_region[1] {
                region_ids.push(((x << 8) + y) as u16);
            }
        }
        if expected_region_ids.len() != region_ids.len() {
            return Err(SurfaceError::RegionCount {
                expected: region_ids.len(),
                actual: expected_region_ids.len(),
            });
        }
        for (index, (&expected, &actual)) in region_ids.iter().zip(expected_region_ids).enumerate()
        {
            if expected != actual {
                return Err(SurfaceError::RegionOrder {
                    index,
                    expected,
                    actual,
                });
            }
        }
        Ok(Self {
            base_tile,
            map_size,
            center_chunk,
            region_ids: region_ids.into_boxed_slice(),
        })
    }

    pub fn base_tile(&self) -> [i32; 2] {
        self.base_tile
    }

    pub fn map_size(&self) -> NativeMapSize {
        self.map_size
    }

    pub fn center_chunk(&self) -> [i32; 2] {
        self.center_chunk
    }

    pub fn region_ids(&self) -> &[u16] {
        &self.region_ids
    }

    fn scene_origin(&self, region_id: u16) -> [i32; 2] {
        let origin = region_origin(region_id);
        [origin[0] - self.base_tile[0], origin[1] - self.base_tile[1]]
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum RegionAvailability {
    Present,
    ProvenAbsent,
    Unavailable,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct SourceTile {
    pub region_id: u16,
    pub region_tile: [usize; 2],
    pub world_tile: [i32; 2],
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct SurfaceHeights {
    layout: StandardMapLayout,
    heights: Box<[Option<i32>]>,
    region_availability: Box<[(u16, RegionAvailability)]>,
}

impl SurfaceHeights {
    pub fn compose(
        layout: &StandardMapLayout,
        present: &[(u16, &Terrain)],
        proven_absent: &[u16],
    ) -> Result<Self, SurfaceError> {
        let stride = layout.map_size.tiles() + 1;
        let mut result = Self {
            layout: layout.clone(),
            heights: vec![None; PLANES * stride * stride].into_boxed_slice(),
            region_availability: layout
                .region_ids
                .iter()
                .map(|&id| (id, RegionAvailability::Unavailable))
                .collect(),
        };
        for &(region_id, _) in present {
            result.classify(region_id, RegionAvailability::Present)?;
        }
        for &region_id in proven_absent {
            result.classify(region_id, RegionAvailability::ProvenAbsent)?;
        }
        let size = layout.map_size.tiles();
        for plane in 0..PLANES {
            for coordinate in 0..=size {
                result.set(plane, size, coordinate, Some(0));
                result.set(plane, coordinate, size, Some(0));
            }
        }
        for &(region_id, terrain) in present {
            let scene_origin = layout.scene_origin(region_id);
            let world_origin = region_origin(region_id);
            for plane in 0..PLANES {
                for x in 0..MAP_SIZE {
                    for y in 0..MAP_SIZE {
                        if let Some([scene_x, scene_y]) =
                            result.interior(scene_origin[0] + x as i32, scene_origin[1] + y as i32)
                        {
                            let value =
                                terrain.surface_height_native_units(world_origin, plane, x, y);
                            result.set(plane, scene_x, scene_y, value);
                        }
                    }
                }
            }
        }
        for &region_id in proven_absent {
            let origin = layout.scene_origin(region_id);
            for plane in 0..PLANES {
                for x in origin[0]..origin[0] + MAP_SIZE as i32 {
                    for y in origin[1]..origin[1] + MAP_SIZE as i32 {
                        if let Some([x, y]) = result.interior(x, y) {
                            result.set(plane, x, y, Some(0));
                        }
                    }
                }
            }
        }
        if layout.center_chunk[1] < 800 {
            for &region_id in layout.region_ids.iter() {
                if result.availability(region_id) == RegionAvailability::ProvenAbsent {
                    let origin = layout.scene_origin(region_id);
                    for plane in 0..PLANES {
                        result.clear_absent(plane, origin);
                    }
                }
            }
        }
        Ok(result)
    }

    pub fn layout(&self) -> &StandardMapLayout {
        &self.layout
    }

    pub fn height_native_units(&self, plane: usize, x: usize, y: usize) -> Option<i32> {
        self.index(plane, x, y)
            .and_then(|index| self.heights[index])
    }

    pub fn heights_native_units(&self) -> &[Option<i32>] {
        &self.heights
    }

    pub fn region_availability(&self) -> &[(u16, RegionAvailability)] {
        &self.region_availability
    }

    pub fn source_tile(&self, x: usize, y: usize) -> Option<SourceTile> {
        let size = self.layout.map_size.tiles();
        if x >= size || y >= size {
            return None;
        }
        let world_tile = [
            self.layout.base_tile[0] + x as i32,
            self.layout.base_tile[1] + y as i32,
        ];
        Some(SourceTile {
            region_id: (((world_tile[0] / 64) << 8) + world_tile[1] / 64) as u16,
            region_tile: [(world_tile[0] % 64) as usize, (world_tile[1] % 64) as usize],
            world_tile,
        })
    }

    fn classify(
        &mut self,
        region_id: u16,
        availability: RegionAvailability,
    ) -> Result<(), SurfaceError> {
        let entry = self
            .region_availability
            .iter_mut()
            .find(|entry| entry.0 == region_id)
            .ok_or(SurfaceError::UnexpectedRegion { region_id })?;
        if entry.1 != RegionAvailability::Unavailable {
            return Err(SurfaceError::DuplicateRegion { region_id });
        }
        entry.1 = availability;
        Ok(())
    }

    fn availability(&self, region_id: u16) -> RegionAvailability {
        self.region_availability
            .iter()
            .find(|entry| entry.0 == region_id)
            .unwrap()
            .1
    }

    fn index(&self, plane: usize, x: usize, y: usize) -> Option<usize> {
        let stride = self.layout.map_size.tiles() + 1;
        if plane < PLANES && x < stride && y < stride {
            Some((plane * stride + x) * stride + y)
        } else {
            None
        }
    }

    fn set(&mut self, plane: usize, x: usize, y: usize, value: Option<i32>) {
        let index = self.index(plane, x, y).unwrap();
        self.heights[index] = value;
    }

    fn interior(&self, x: i32, y: i32) -> Option<[usize; 2]> {
        let size = self.layout.map_size.tiles() as i32;
        if x >= 0 && y >= 0 && x < size && y < size {
            Some([x as usize, y as usize])
        } else {
            None
        }
    }

    fn clear_absent(&mut self, plane: usize, origin: [i32; 2]) {
        let [x, y] = origin;
        for next_y in y..y + MAP_SIZE as i32 {
            for next_x in x..x + MAP_SIZE as i32 {
                if let Some([next_x, next_y]) = self.interior(next_x, next_y) {
                    let height = if plane == 0 {
                        Some(0)
                    } else {
                        self.height_native_units(plane - 1, next_x, next_y)
                            .map(|height| height - 960)
                    };
                    self.set(plane, next_x, next_y, height);
                }
            }
        }
        let size = self.layout.map_size.tiles() as i32;
        if x > 0 && x < size {
            for next_y in y + 1..y + MAP_SIZE as i32 {
                if let Some([x, next_y]) = self.interior(x, next_y) {
                    self.set(
                        plane,
                        x,
                        next_y,
                        self.height_native_units(plane, x - 1, next_y),
                    );
                }
            }
        }
        if y > 0 && y < size {
            for next_x in x + 1..x + MAP_SIZE as i32 {
                if let Some([next_x, y]) = self.interior(next_x, y) {
                    self.set(
                        plane,
                        next_x,
                        y,
                        self.height_native_units(plane, next_x, y - 1),
                    );
                }
            }
        }
        if let Some([x, y]) = self.interior(x, y) {
            for neighbor in [
                (x.checked_sub(1), Some(y)),
                (Some(x), y.checked_sub(1)),
                (x.checked_sub(1), y.checked_sub(1)),
            ] {
                if let (Some(nx), Some(ny)) = neighbor {
                    let current = self.height_native_units(plane, nx, ny);
                    let reference = if plane == 0 {
                        Some(0)
                    } else {
                        self.height_native_units(plane - 1, nx, ny)
                    };
                    match (current, reference) {
                        (Some(value), Some(reference)) if value == reference => {}
                        (Some(value), Some(_)) => {
                            self.set(plane, x, y, Some(value));
                            return;
                        }
                        _ => {
                            self.set(plane, x, y, None);
                            return;
                        }
                    }
                }
            }
        }
    }
}

fn region_origin(region_id: u16) -> [i32; 2] {
    [
        i32::from(region_id >> 8) * 64,
        i32::from(region_id & 255) * 64,
    ]
}
