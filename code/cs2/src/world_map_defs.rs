const BUCKETS: usize = 16;
const BOUND_START: i32 = 12800;
const WINDOWS_1252: [u16; 32] = [
    0x20ac, 0, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021, 0x02c6, 0x2030, 0x0160, 0x2039,
    0x0152, 0, 0x017d, 0, 0, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014, 0x02dc,
    0x2122, 0x0161, 0x203a, 0x0153, 0, 0x017e, 0x0178,
];

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct WorldMapDecodeError;

impl std::fmt::Display for WorldMapDecodeError {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str("world map definition ended early")
    }
}

impl std::error::Error for WorldMapDecodeError {}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct WorldMapPlacement {
    pub plane: i32,
    pub start_x: i32,
    pub start_y: i32,
    pub end_x: i32,
    pub end_y: i32,
    pub map_min_x: i32,
    pub map_min_y: i32,
    pub map_max_x: i32,
    pub map_max_y: i32,
}

impl WorldMapPlacement {
    pub fn within_plane_bounds(&self, plane: i32, x: i32, y: i32) -> bool {
        self.plane == plane && self.within_bounds(x, y)
    }

    pub fn within_map_bounds(&self, x: i32, y: i32) -> bool {
        x >= self.map_min_x && x <= self.map_max_x && y >= self.map_min_y && y <= self.map_max_y
    }

    pub fn within_bounds(&self, x: i32, y: i32) -> bool {
        x >= self.start_x && x <= self.end_x && y >= self.start_y && y <= self.end_y
    }

    pub fn to_game(&self, x: i32, y: i32) -> (i32, i32, i32) {
        (
            self.plane,
            self.start_x.wrapping_sub(self.map_min_x).wrapping_add(x),
            self.start_y.wrapping_sub(self.map_min_y).wrapping_add(y),
        )
    }

    pub fn to_map(&self, x: i32, y: i32) -> (i32, i32, i32) {
        (
            0,
            self.map_min_x.wrapping_sub(self.start_x).wrapping_add(x),
            self.map_min_y.wrapping_sub(self.start_y).wrapping_add(y),
        )
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct WorldMapElement {
    pub element_id: i32,
    pub filename_prefix: String,
    pub display_name: String,
    pub bitpacked_placement: i32,
    pub color: i32,
    pub should_render: bool,
    pub default_zoom_level: i32,
    pub map_size_id: i32,
    pub placements: Vec<WorldMapPlacement>,
    pub bounds_min_x: i32,
    pub bounds_max_x: i32,
    pub bounds_min_y: i32,
    pub bounds_max_y: i32,
}

struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl Reader<'_> {
    fn byte(&mut self) -> Result<u8, WorldMapDecodeError> {
        let value = *self.bytes.get(self.position).ok_or(WorldMapDecodeError)?;
        self.position += 1;
        Ok(value)
    }

    fn unsigned_short(&mut self) -> Result<i32, WorldMapDecodeError> {
        let high = i32::from(self.byte()?);
        Ok((high << 8) | i32::from(self.byte()?))
    }

    fn int(&mut self) -> Result<i32, WorldMapDecodeError> {
        let high = self.unsigned_short()?;
        Ok((high << 16) | self.unsigned_short()?)
    }

    fn string(&mut self) -> Result<String, WorldMapDecodeError> {
        let mut units: Vec<u16> = Vec::new();
        loop {
            let value = self.byte()?;
            if value == 0 {
                break;
            }
            let unit = if (128..160).contains(&value) {
                match WINDOWS_1252[usize::from(value) - 128] {
                    0 => 63,
                    mapped => mapped,
                }
            } else {
                u16::from(value)
            };
            units.push(unit);
        }
        Ok(String::from_utf16_lossy(&units))
    }
}

impl WorldMapElement {
    pub fn decode(element_id: i32, bytes: &[u8]) -> Result<Self, WorldMapDecodeError> {
        let mut reader = Reader { bytes, position: 0 };
        let filename_prefix = reader.string()?;
        let display_name = reader.string()?;
        let bitpacked_placement = reader.int()?;
        let color = reader.int()?;
        let should_render = reader.byte()? == 1;
        let mut default_zoom_level = i32::from(reader.byte()?);
        if default_zoom_level == 255 {
            default_zoom_level = 0;
        }
        let map_size_id = i32::from(reader.byte()?);
        let count = reader.byte()?;
        let mut placements = Vec::with_capacity(usize::from(count));
        for _ in 0..count {
            let plane = i32::from(reader.byte()?);
            let mut values = [0; 8];
            for value in &mut values {
                *value = reader.unsigned_short()?;
            }
            placements.push(WorldMapPlacement {
                plane,
                start_x: values[0],
                start_y: values[1],
                end_x: values[2],
                end_y: values[3],
                map_min_x: values[4],
                map_min_y: values[5],
                map_max_x: values[6],
                map_max_y: values[7],
            });
        }
        let mut element = Self {
            element_id,
            filename_prefix,
            display_name,
            bitpacked_placement,
            color,
            should_render,
            default_zoom_level,
            map_size_id,
            placements,
            bounds_min_x: BOUND_START,
            bounds_max_x: 0,
            bounds_min_y: BOUND_START,
            bounds_max_y: 0,
        };
        element.recalculate_bounds();
        Ok(element)
    }

    pub fn recalculate_bounds(&mut self) {
        self.bounds_min_x = BOUND_START;
        self.bounds_max_x = 0;
        self.bounds_min_y = BOUND_START;
        self.bounds_max_y = 0;
        for placement in &self.placements {
            self.bounds_min_x = self.bounds_min_x.min(placement.map_min_x);
            self.bounds_max_x = self.bounds_max_x.max(placement.map_max_x);
            self.bounds_min_y = self.bounds_min_y.min(placement.map_min_y);
            self.bounds_max_y = self.bounds_max_y.max(placement.map_max_y);
        }
    }

    pub fn map_to_game(&self, x: i32, y: i32) -> Option<(i32, i32)> {
        self.placements
            .iter()
            .find(|placement| placement.within_map_bounds(x, y))
            .map(|placement| {
                let (_, game_x, game_y) = placement.to_game(x, y);
                (game_x, game_y)
            })
    }

    pub fn game_to_map_for_plane(&self, plane: i32, x: i32, y: i32) -> Option<(i32, i32)> {
        self.placements
            .iter()
            .find(|placement| placement.within_plane_bounds(plane, x, y))
            .map(|placement| {
                let (_, map_x, map_y) = placement.to_map(x, y);
                (map_x, map_y)
            })
    }

    pub fn game_to_map(&self, x: i32, y: i32) -> Option<(i32, i32)> {
        self.placements
            .iter()
            .find(|placement| placement.within_bounds(x, y))
            .map(|placement| {
                let (_, map_x, map_y) = placement.to_map(x, y);
                (map_x, map_y)
            })
    }

    pub fn contains_game_coord(&self, x: i32, y: i32) -> bool {
        self.placements
            .iter()
            .any(|placement| placement.within_bounds(x, y))
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct WorldMapDefs {
    buckets: [Vec<WorldMapElement>; BUCKETS],
}

impl Default for WorldMapDefs {
    fn default() -> Self {
        Self {
            buckets: std::array::from_fn(|_| Vec::new()),
        }
    }
}

impl WorldMapDefs {
    pub fn put(&mut self, element: WorldMapElement) {
        let bucket = (i64::from(element.element_id) & (BUCKETS as i64 - 1)) as usize;
        self.buckets[bucket].push(element);
    }

    pub fn iter(&self) -> impl Iterator<Item = &WorldMapElement> {
        self.buckets.iter().flatten()
    }

    pub fn poll(&self, element_id: i32) -> Option<&WorldMapElement> {
        let bucket = (i64::from(element_id) & (BUCKETS as i64 - 1)) as usize;
        self.buckets[bucket]
            .iter()
            .find(|element| element.element_id == element_id)
    }

    pub fn get_map_def(&self, x: i32, y: i32) -> Option<&WorldMapElement> {
        self.iter()
            .find(|element| element.should_render && element.contains_game_coord(x, y))
    }

    pub fn rendered_elements(&self, x: i32, y: i32) -> impl Iterator<Item = &WorldMapElement> {
        self.iter()
            .filter(move |element| element.should_render && element.contains_game_coord(x, y))
    }

    pub fn len(&self) -> usize {
        self.buckets.iter().map(Vec::len).sum()
    }

    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct WorldMapCategory {
    pub mec_id: i32,
    pub abs_x: i32,
    pub abs_y: i32,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct MecType {
    pub visible: bool,
    pub primary_varp: i32,
    pub primary_varbit: i32,
    pub primary_min: i32,
    pub primary_max: i32,
    pub secondary_varp: i32,
    pub secondary_varbit: i32,
    pub secondary_min: i32,
    pub secondary_max: i32,
}

impl Default for MecType {
    fn default() -> Self {
        Self {
            visible: true,
            primary_varp: -1,
            primary_varbit: -1,
            primary_min: 0,
            primary_max: 0,
            secondary_varp: -1,
            secondary_varbit: -1,
            secondary_min: 0,
            secondary_max: 0,
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct WorldMapData {
    pub defs: WorldMapDefs,
    pub categories: Vec<WorldMapCategory>,
    pub mec_types: std::collections::BTreeMap<i32, MecType>,
}

impl WorldMapData {
    pub fn find_nearest_element(
        &self,
        mec_id: i32,
        coord_x: i32,
        coord_y: i32,
        square_x: i32,
        square_y: i32,
        loading_percentage: i32,
    ) -> i32 {
        if loading_percentage < 100 {
            return -2;
        }
        let mut nearest_coord = -2;
        let mut nearest_distance = i32::MAX;
        let local_x = coord_x.wrapping_sub(square_x);
        let local_y = coord_y.wrapping_sub(square_y);
        for category in &self.categories {
            if category.mec_id != mec_id {
                continue;
            }
            let delta_x = local_x.wrapping_sub(category.abs_x);
            let delta_y = local_y.wrapping_sub(category.abs_y);
            let coord = (category.abs_x.wrapping_add(square_x) << 14)
                | category.abs_y.wrapping_add(square_y);
            let distance = delta_x
                .wrapping_mul(delta_x)
                .wrapping_add(delta_y.wrapping_mul(delta_y));
            if nearest_coord < 0 || distance < nearest_distance {
                nearest_coord = coord;
                nearest_distance = distance;
            }
        }
        nearest_coord
    }
}
