use crate::HostError;

const LOW_INIT: i32 = 32767;

fn index_error(index: i32) -> HostError {
    HostError::State(format!("array index {index} out of bounds"))
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct BoundingBox {
    pub calculated: bool,
    pub left: i32,
    pub right: i32,
    pub top: i32,
    pub bottom: i32,
    pub size: i32,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct BoxSet {
    pub polygons: Option<Vec<Option<BoundingBox>>>,
}

impl BoxSet {
    pub fn extent(&self) -> Result<Option<[i32; 4]>, HostError> {
        let mut found: Option<[i32; 4]> = None;
        for polygon in self.polygons.iter().flatten() {
            let polygon = polygon.ok_or(HostError::NoEntity)?;
            if !polygon.calculated {
                continue;
            }
            let size = polygon.size;
            let (left, right) = if polygon.right < polygon.left {
                (
                    polygon.right.wrapping_sub(size),
                    size.wrapping_add(polygon.left),
                )
            } else {
                (
                    polygon.left.wrapping_sub(size),
                    polygon.right.wrapping_add(size),
                )
            };
            let (bottom, top) = if polygon.top < polygon.bottom {
                (
                    polygon.top.wrapping_sub(size),
                    size.wrapping_add(polygon.bottom),
                )
            } else {
                (
                    polygon.bottom.wrapping_sub(size),
                    polygon.top.wrapping_add(size),
                )
            };
            found = Some(match found {
                None => [left, bottom, right, top],
                Some([min_x, min_y, max_x, max_y]) => [
                    min_x.min(left),
                    min_y.min(bottom),
                    max_x.max(right),
                    max_y.max(top),
                ],
            });
        }
        Ok(found)
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ModelBounds {
    pub bounds_updated: bool,
    pub min_y: i16,
    pub vertex_count: i32,
    pub vertices_x: Option<Vec<i32>>,
    pub vertices_y: Option<Vec<i32>>,
    pub vertices_z: Option<Vec<i32>>,
}

impl ModelBounds {
    pub fn minimum_y(&mut self) -> Result<i32, HostError> {
        if !self.bounds_updated {
            self.calculate_bounds()?;
        }
        Ok(i32::from(self.min_y))
    }

    fn calculate_bounds(&mut self) -> Result<(), HostError> {
        let mut low_y = LOW_INIT;
        for index in 0..self.vertex_count.max(0) {
            let slot = index as usize;
            for column in [&self.vertices_x, &self.vertices_y, &self.vertices_z] {
                let column = column.as_ref().ok_or(HostError::NoEntity)?;
                if slot >= column.len() {
                    return Err(index_error(index));
                }
            }
            let y = self.vertices_y.as_ref().map_or(0, |column| column[slot]);
            if y < low_y {
                low_y = y;
            }
        }
        self.min_y = low_y as i16;
        self.bounds_updated = true;
        Ok(())
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum LocHeight {
    Model(Option<ModelBounds>),
    Animated(i32),
}

impl LocHeight {
    pub fn icon_height(&mut self) -> Result<i32, HostError> {
        match self {
            Self::Model(None) => Ok(0),
            Self::Model(Some(model)) => Ok(model.minimum_y()?.wrapping_neg()),
            Self::Animated(min_y) => Ok(min_y.wrapping_neg()),
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct BoundsState {
    pub entity: Option<BoxSet>,
    pub loc: Option<BoxSet>,
    pub obj: Option<BoxSet>,
    pub loc_height: Option<LocHeight>,
}
