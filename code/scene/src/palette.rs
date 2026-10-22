use crate::floor::{pack_hsl, Underlay};
use crate::surface::NativeMapSize;
use crate::terrain::PLANES;
use std::error::Error;
use std::fmt::{Display, Formatter, Result as FmtResult};

#[derive(Clone, Copy, Debug)]
pub enum UnderlayLookup<'a> {
    Present(&'a Underlay),
    ProvenAbsent,
    Unavailable,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum PaletteCell {
    Known(i32),
    Unavailable,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum PaletteError {
    UnderlayCount { expected: usize, actual: usize },
}

impl Display for PaletteError {
    fn fmt(&self, f: &mut Formatter<'_>) -> FmtResult {
        match self {
            Self::UnderlayCount { expected, actual } => {
                write!(
                    f,
                    "native palette requires {expected} underlay IDs, received {actual}"
                )
            }
        }
    }
}

impl Error for PaletteError {}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct UnderlayPalette {
    map_size: NativeMapSize,
    cells: Box<[PaletteCell]>,
    unavailable_definition_ids: Box<[u8]>,
}

impl UnderlayPalette {
    pub fn map_size(&self) -> NativeMapSize {
        self.map_size
    }

    pub fn cells(&self) -> &[PaletteCell] {
        &self.cells
    }

    pub fn cell(&self, plane: usize, x: usize, y: usize) -> Option<PaletteCell> {
        let size = self.map_size.tiles();
        if plane < PLANES && x < size && y < size {
            Some(self.cells[(plane * size + x) * size + y])
        } else {
            None
        }
    }

    pub fn unavailable_definition_ids(&self) -> &[u8] {
        &self.unavailable_definition_ids
    }
}

pub fn calculate_underlay_palette<'a>(
    map_size: NativeMapSize,
    underlay_ids: &[Option<u8>],
    mut lookup: impl FnMut(u8) -> UnderlayLookup<'a>,
) -> Result<UnderlayPalette, PaletteError> {
    let size = map_size.tiles();
    let expected = PLANES * size * size;
    if underlay_ids.len() != expected {
        return Err(PaletteError::UnderlayCount {
            expected,
            actual: underlay_ids.len(),
        });
    }
    let mut definitions = [Contribution::default(); 256];
    let mut seen = [false; 256];
    let mut unavailable_definition_ids = Vec::new();
    for &id in underlay_ids.iter().flatten() {
        if id == 0 || seen[usize::from(id)] {
            continue;
        }
        seen[usize::from(id)] = true;
        let definition_id = id - 1;
        definitions[usize::from(id)] = match lookup(definition_id) {
            UnderlayLookup::Present(underlay) => Contribution::underlay(underlay),
            UnderlayLookup::ProvenAbsent => Contribution::underlay(&Underlay::default()),
            UnderlayLookup::Unavailable => {
                unavailable_definition_ids.push(definition_id);
                Contribution::unavailable()
            }
        };
    }
    unavailable_definition_ids.sort_unstable();
    let mut carried = vec![PaletteCell::Known(0); size * size];
    let mut cells = Vec::with_capacity(expected);
    for plane in 0..PLANES {
        let mut columns = vec![Contribution::default(); size];
        for x in -5..size as i32 {
            for (y, column) in columns.iter_mut().enumerate() {
                let forward_x = x + 5;
                if forward_x < size as i32 {
                    column.add(contribution(
                        underlay_ids[(plane * size + forward_x as usize) * size + y],
                        &definitions,
                    ));
                }
                let backward_x = x - 5;
                if backward_x >= 0 {
                    column.subtract(contribution(
                        underlay_ids[(plane * size + backward_x as usize) * size + y],
                        &definitions,
                    ));
                }
            }
            if x < 0 {
                continue;
            }
            let mut window = Contribution::default();
            for y in -5..size as i32 {
                let forward_y = y + 5;
                if forward_y < size as i32 {
                    window.add(columns[forward_y as usize]);
                }
                let backward_y = y - 5;
                if backward_y >= 0 {
                    window.subtract(columns[backward_y as usize]);
                }
                if y < 0 {
                    continue;
                }
                let cell = &mut carried[x as usize * size + y as usize];
                if window.unavailable > 0 {
                    *cell = PaletteCell::Unavailable;
                } else if window.divider > 0 && window.count > 0 {
                    *cell = PaletteCell::Known(pack_hsl(
                        window.hue.wrapping_mul(256) / window.divider,
                        window.saturation / window.count,
                        window.lightness / window.count,
                    ));
                }
            }
        }
        cells.extend_from_slice(&carried);
    }
    Ok(UnderlayPalette {
        map_size,
        cells: cells.into_boxed_slice(),
        unavailable_definition_ids: unavailable_definition_ids.into_boxed_slice(),
    })
}

#[derive(Clone, Copy, Default)]
struct Contribution {
    hue: i32,
    saturation: i32,
    lightness: i32,
    divider: i32,
    count: i32,
    unavailable: i32,
}

impl Contribution {
    fn underlay(underlay: &Underlay) -> Self {
        Self {
            hue: underlay.hue,
            saturation: underlay.saturation,
            lightness: underlay.lightness,
            divider: underlay.hue_multiplier,
            count: 1,
            unavailable: 0,
        }
    }

    fn unavailable() -> Self {
        Self {
            unavailable: 1,
            ..Self::default()
        }
    }

    fn add(&mut self, value: Self) {
        self.hue = self.hue.wrapping_add(value.hue);
        self.saturation = self.saturation.wrapping_add(value.saturation);
        self.lightness = self.lightness.wrapping_add(value.lightness);
        self.divider = self.divider.wrapping_add(value.divider);
        self.count = self.count.wrapping_add(value.count);
        self.unavailable += value.unavailable;
    }

    fn subtract(&mut self, value: Self) {
        self.hue = self.hue.wrapping_sub(value.hue);
        self.saturation = self.saturation.wrapping_sub(value.saturation);
        self.lightness = self.lightness.wrapping_sub(value.lightness);
        self.divider = self.divider.wrapping_sub(value.divider);
        self.count = self.count.wrapping_sub(value.count);
        self.unavailable -= value.unavailable;
    }
}

fn contribution(id: Option<u8>, definitions: &[Contribution; 256]) -> Contribution {
    match id {
        Some(id) => definitions[usize::from(id)],
        None => Contribution::unavailable(),
    }
}
