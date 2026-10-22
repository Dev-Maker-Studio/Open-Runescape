#![allow(
    clippy::needless_range_loop,
    clippy::too_many_arguments,
    clippy::manual_swap,
    clippy::explicit_counter_loop,
    clippy::approx_constant,
    clippy::type_complexity,
    clippy::let_and_return
)]

mod bricks;
mod ops;
mod random;
mod raster;
mod sprite;

use crate::{is_opaque_effect, Error, MaterialType, Reader};
use ops::Kind;
use std::rc::Rc;

const MAX_DEPTH: usize = 4096;
const MAX_NESTING: usize = 8;

impl Reader<'_> {
    pub(crate) fn short(&mut self) -> Result<i32, Error> {
        Ok(i32::from(self.u16()? as i16))
    }

    pub(crate) fn byte(&mut self) -> Result<i32, Error> {
        Ok(i32::from(self.u8()? as i8))
    }

    pub(crate) fn medium(&mut self) -> Result<i32, Error> {
        let bytes = self.take(3)?;
        Ok((i32::from(bytes[0]) << 16) | (i32::from(bytes[1]) << 8) | i32::from(bytes[2]))
    }
}

pub struct MaterialSource<'a> {
    pub texture: &'a [u8],
    pub is_small: bool,
    pub is_brick_tile: bool,
}

pub trait TextureSources {
    fn sprite(&self, id: u32) -> Option<&[u8]>;
    fn material(&self, id: u32) -> Option<MaterialSource<'_>>;
}

#[derive(Debug, Clone)]
pub(crate) struct Node {
    pub(crate) kind: Kind,
    pub(crate) mono: bool,
    pub(crate) capacity: u8,
    pub(crate) children: Vec<usize>,
}

#[derive(Debug, Clone)]
pub struct TextureGraph {
    nodes: Vec<Node>,
    opaque: usize,
    opacity: usize,
    hdr: usize,
    texture_file_ids: Vec<u32>,
    material_file_ids: Vec<u32>,
}

impl TextureGraph {
    pub fn decode(bytes: &[u8]) -> Result<Self, Error> {
        let mut reader = Reader::new(bytes);
        let count = usize::from(reader.u8()?);
        let mut nodes = Vec::with_capacity(count);
        for _ in 0..count {
            reader.u8()?;
            let implementation = reader.u8()?;
            let capacity = reader.u8()?;
            let opcodes = reader.u8()?;
            let mut kind = ops::new_kind(implementation)?;
            let mut mono = ops::default_mono(&kind);
            for _ in 0..opcodes {
                let opcode = reader.u8()?;
                ops::decode_opcode(&mut kind, &mut mono, opcode, &mut reader)?;
            }
            ops::post_decode(&mut kind)?;
            let mut children = Vec::new();
            for _ in 0..ops::child_count(&kind) {
                children.push(usize::from(reader.u8()?));
            }
            nodes.push(Node {
                kind,
                mono,
                capacity,
                children,
            });
        }
        for node in &nodes {
            if node.children.iter().any(|&child| child >= count) {
                return Err(Error::InvalidTexture("child index"));
            }
        }
        let mut texture_file_ids = Vec::new();
        let mut material_file_ids = Vec::new();
        for node in &nodes {
            if let Some(id) = ops::sprite_id(&node.kind) {
                if id > 0 {
                    texture_file_ids.push(id as u32);
                }
            }
            if let Some(id) = ops::material_id(&node.kind) {
                if id > 0 {
                    material_file_ids.push(id as u32);
                }
            }
        }
        let opaque = usize::from(reader.u8()?);
        let opacity = usize::from(reader.u8()?);
        let hdr = usize::from(reader.u8()?);
        if opaque >= count || opacity >= count || hdr >= count {
            return Err(Error::InvalidTexture("output index"));
        }
        Ok(Self {
            nodes,
            opaque,
            opacity,
            hdr,
            texture_file_ids,
            material_file_ids,
        })
    }

    pub fn operation_count(&self) -> usize {
        self.nodes.len()
    }

    pub fn operation_names(&self) -> Vec<&'static str> {
        self.nodes.iter().map(|n| ops::name(&n.kind)).collect()
    }

    pub fn texture_file_ids(&self) -> &[u32] {
        &self.texture_file_ids
    }

    pub fn material_file_ids(&self) -> &[u32] {
        &self.material_file_ids
    }

    pub fn structure(&self) -> Vec<(&'static str, u8, bool, Vec<usize>)> {
        self.nodes
            .iter()
            .map(|n| (ops::name(&n.kind), n.capacity, n.mono, n.children.clone()))
            .collect()
    }

    pub fn debug_kinds(&self) -> Vec<String> {
        self.nodes
            .iter()
            .map(|n| {
                let text = match &n.kind {
                    ops::Kind::Voronoi(v) => format!(
                        "Voronoi seed={} jitter={} cx={} cy={} out={} metric={} points={}",
                        v.seed,
                        v.jitter,
                        v.cells_x,
                        v.cells_y,
                        v.output,
                        v.metric,
                        v.points.len()
                    ),
                    ops::Kind::Perlin(p) => format!(
                        "Perlin steps={} fog={} sx={} sy={} mult={:?} amp={:?}",
                        p.steps,
                        p.fog,
                        p.scale_x,
                        p.scale_y,
                        p.step_multipliers,
                        p.amplitude_multipliers
                    ),
                    other => format!("{other:?}"),
                };
                text.chars().take(260).collect()
            })
            .collect()
    }

    pub fn output_indices(&self) -> (usize, usize, usize) {
        (self.opaque, self.opacity, self.hdr)
    }

    pub fn render_rgb(
        &self,
        sources: &dyn TextureSources,
        brightness: f64,
        size: usize,
        read_reversed: bool,
        write_reversed: bool,
    ) -> Result<Vec<u32>, Error> {
        self.rgb(sources, brightness, size, read_reversed, write_reversed, 0)
    }

    pub fn render_argb(
        &self,
        sources: &dyn TextureSources,
        size: usize,
        write_reversed: bool,
    ) -> Result<Vec<u32>, Error> {
        let mut eval = Eval::new(self, sources, size, size, 0)?;
        let table = brightness_table(0.7);
        let mut pixels = vec![0u32; size * size];
        let mut position = 0usize;
        for row in 0..size {
            let (_, color_slot) = eval.rows_slot(self.opaque, row as i32)?;
            let opacity = eval.rows(self.opacity, row as i32)?;
            let color = eval.slot_rows(self.opaque, color_slot)?;
            let opacity = &opacity[0];
            if write_reversed {
                position = row;
            }
            for x in (0..size).rev() {
                let red = table[clamp_channel(color[0][x] >> 4)];
                let green = table[clamp_channel(channel(&color, 1)[x] >> 4)];
                let blue = table[clamp_channel(channel(&color, 2)[x] >> 4)];
                let alpha = if red == 0 && green == 0 && blue == 0 {
                    0
                } else {
                    clamp_channel(opacity[x] >> 4) as u32
                };
                pixels[position] = (blue as u32)
                    .wrapping_add((green as u32) << 8)
                    .wrapping_add(alpha << 24)
                    .wrapping_add((red as u32) << 16);
                position += 1;
                if write_reversed {
                    position += size - 1;
                }
            }
        }
        Ok(pixels)
    }

    fn rgb(
        &self,
        sources: &dyn TextureSources,
        brightness: f64,
        size: usize,
        read_reversed: bool,
        write_reversed: bool,
        depth: usize,
    ) -> Result<Vec<u32>, Error> {
        let mut eval = Eval::new(self, sources, size, size, depth)?;
        let table = brightness_table(brightness);
        let mut pixels = vec![0u32; size * size];
        let mut position = 0usize;
        for row in 0..size {
            let color = eval.rows(self.opaque, row as i32)?;
            if write_reversed {
                position = row;
            }
            for step in 0..size {
                let x = if read_reversed { size - 1 - step } else { step };
                let red = table[clamp_channel(color[0][x] >> 4)];
                let green = table[clamp_channel(channel(&color, 1)[x] >> 4)];
                let blue = table[clamp_channel(channel(&color, 2)[x] >> 4)];
                let mut pixel = (blue as u32)
                    .wrapping_add((green as u32) << 8)
                    .wrapping_add((red as u32) << 16);
                if pixel != 0 {
                    pixel |= 0xff00_0000;
                }
                pixels[position] = pixel;
                position += 1;
                if write_reversed {
                    position += size - 1;
                }
            }
        }
        Ok(pixels)
    }
}

fn channel(rows: &[Vec<i32>], index: usize) -> &Vec<i32> {
    if rows.len() == 1 {
        &rows[0]
    } else {
        &rows[index]
    }
}

fn clamp_channel(value: i32) -> usize {
    value.clamp(0, 255) as usize
}

fn brightness_table(brightness: f64) -> [u8; 256] {
    let mut table = [0u8; 256];
    for (grade, slot) in table.iter_mut().enumerate() {
        let value = (libm::pow(grade as f64 / 255.0, brightness) * 255.0) as i32;
        *slot = value.min(255) as u8;
    }
    table
}

pub fn render_material(
    texture: &[u8],
    material: &MaterialType,
    sources: &dyn TextureSources,
) -> Result<Vec<u32>, Error> {
    let graph = TextureGraph::decode(texture)?;
    let size = if material.is_small { 64 } else { 128 };
    let opaque = material.effect_combiner != 2 && is_opaque_effect(i32::from(material.effect_id));
    if opaque {
        graph.rgb(sources, 0.7, size, true, material.is_brick_tile, 0)
    } else {
        graph.render_argb(sources, size, material.is_brick_tile)
    }
}

pub(crate) struct Eval<'a> {
    graph: &'a TextureGraph,
    sources: &'a dyn TextureSources,
    depth: usize,
    nesting: usize,
    pub(crate) width: usize,
    pub(crate) height: usize,
    pub(crate) horizontal: Vec<i32>,
    pub(crate) vertical: Vec<i32>,
    pub(crate) row_end: i32,
    pub(crate) column_end: i32,
    pub(crate) scaled_width: i32,
    pub(crate) prior: Option<Rc<Vec<Vec<i32>>>>,
    pub(crate) palette_sin: Vec<i32>,
    pub(crate) palette_cos: Vec<i32>,
    pub(crate) sine_wave: Vec<i32>,
    rings: Vec<Ring>,
    pub(crate) pixels: Vec<Option<Rc<(Vec<i32>, usize, usize)>>>,
    pub(crate) state: ops::State,
}

impl<'a> Eval<'a> {
    fn new(
        graph: &'a TextureGraph,
        sources: &'a dyn TextureSources,
        width: usize,
        height: usize,
        nesting: usize,
    ) -> Result<Self, Error> {
        if width == 0 || height == 0 || nesting > MAX_NESTING {
            return Err(Error::InvalidTexture("dimensions or nesting"));
        }
        let horizontal = (0..width)
            .map(|i| ((i as i32) << 12) / width as i32)
            .collect();
        let vertical = (0..height)
            .map(|i| ((i as i32) << 12) / height as i32)
            .collect();
        let mut palette_sin = Vec::with_capacity(256);
        let mut palette_cos = Vec::with_capacity(256);
        for i in 0..256 {
            let angle = 6.283185307179586 * (f64::from(i) / 255.0);
            palette_sin.push((libm::sin(angle) * 4096.0) as i32);
            palette_cos.push((libm::cos(angle) * 4096.0) as i32);
        }
        let sine_wave = (0..4096i32)
            .map(|t| {
                let t3 = t.wrapping_mul(t.wrapping_mul(t) >> 12) >> 12;
                let six = t.wrapping_mul(6).wrapping_sub(61440);
                let poly = (t.wrapping_mul(six) >> 12).wrapping_add(40960);
                poly.wrapping_mul(t3) >> 12
            })
            .collect();
        let count = graph.nodes.len();
        Ok(Self {
            graph,
            sources,
            depth: 0,
            nesting,
            width,
            height,
            horizontal,
            vertical,
            row_end: width as i32 - 1,
            column_end: height as i32 - 1,
            scaled_width: width as i32 * 32,
            prior: None,
            palette_sin,
            palette_cos,
            sine_wave,
            rings: graph
                .nodes
                .iter()
                .map(|n| Ring::new(n.capacity, height))
                .collect(),
            pixels: vec![None; count],
            state: ops::State::default(),
        })
    }

    pub(crate) fn sources(&self) -> &'a dyn TextureSources {
        self.sources
    }

    pub(crate) fn nested_rgb(&self, id: u32) -> Result<(Vec<i32>, usize), Error> {
        let source = self
            .sources
            .material(id)
            .ok_or(Error::InvalidTexture("missing material"))?;
        let graph = TextureGraph::decode(source.texture)?;
        let size = if source.is_small { 64 } else { 128 };
        let pixels = graph.rgb(
            self.sources,
            1.0,
            size,
            false,
            source.is_brick_tile,
            self.nesting + 1,
        )?;
        Ok((pixels.into_iter().map(|p| p as i32).collect(), size))
    }

    pub(crate) fn rows(&mut self, id: usize, y: i32) -> Result<Rc<Vec<Vec<i32>>>, Error> {
        Ok(self.rows_slot(id, y)?.0)
    }

    pub(crate) fn slot_rows(&self, id: usize, slot: usize) -> Result<Rc<Vec<Vec<i32>>>, Error> {
        self.rings[id].slots[slot]
            .clone()
            .ok_or(Error::InvalidTexture("empty slot"))
    }

    pub(crate) fn rows_slot(
        &mut self,
        id: usize,
        y: i32,
    ) -> Result<(Rc<Vec<Vec<i32>>>, usize), Error> {
        if y < 0 || y as usize >= self.height {
            return Err(Error::InvalidTexture("row out of range"));
        }
        let (slot, dirty) = self.rings[id].acquire(y as usize);
        if !dirty {
            return Ok((self.slot_rows(id, slot)?, slot));
        }
        self.depth += 1;
        if self.depth > MAX_DEPTH {
            return Err(Error::InvalidTexture("recursion"));
        }
        self.prior = self.rings[id].slots[slot].clone();
        let graph = self.graph;
        let result = self.compute(&graph.nodes[id], id, y);
        self.depth -= 1;
        let rows = Rc::new(result?);
        self.rings[id].slots[slot] = Some(rows.clone());
        Ok((rows, slot))
    }

    pub(crate) fn fill_all(&mut self, id: usize, image: Vec<Vec<Vec<i32>>>) -> Result<(), Error> {
        let ring = &mut self.rings[id];
        if ring.total != ring.capacity || image.len() != ring.total {
            return Err(Error::InvalidTexture("ring is not full height"));
        }
        for (row, data) in image.into_iter().enumerate() {
            ring.slots[row] = Some(Rc::new(data));
            ring.row_slot[row] = Some(row);
        }
        Ok(())
    }

    pub(crate) fn child(
        &mut self,
        node: &Node,
        index: usize,
        y: i32,
    ) -> Result<Rc<Vec<Vec<i32>>>, Error> {
        let id = *node
            .children
            .get(index)
            .ok_or(Error::InvalidTexture("missing child"))?;
        self.rows(id, y)
    }
}

struct Ring {
    capacity: usize,
    total: usize,
    slots: Vec<Option<Rc<Vec<Vec<i32>>>>>,
    row_slot: Vec<Option<usize>>,
    order: std::collections::VecDeque<usize>,
    used: usize,
    last: i64,
}

impl Ring {
    fn new(capacity: u8, total: usize) -> Self {
        let capacity = if capacity == 255 {
            total
        } else {
            usize::from(capacity).max(1)
        };
        Self {
            capacity,
            total,
            slots: vec![None; capacity.min(total).max(1)],
            row_slot: vec![None; total],
            order: std::collections::VecDeque::new(),
            used: 0,
            last: -1,
        }
    }

    fn acquire(&mut self, row: usize) -> (usize, bool) {
        if self.total == self.capacity {
            let dirty = self.row_slot[row].is_none();
            self.row_slot[row] = Some(row);
            return (row, dirty);
        }
        if self.capacity == 1 {
            let dirty = self.last != row as i64;
            self.last = row as i64;
            return (0, dirty);
        }
        if let Some(slot) = self.row_slot[row] {
            if let Some(position) = self.order.iter().position(|r| *r == row) {
                self.order.remove(position);
            }
            self.order.push_back(row);
            return (slot, false);
        }
        let slot = if self.used >= self.capacity {
            let oldest = self.order.pop_front().unwrap_or(0);
            let slot = self.row_slot[oldest].take().unwrap_or(0);
            slot
        } else {
            self.used += 1;
            self.used - 1
        };
        self.row_slot[row] = Some(slot);
        self.order.push_back(row);
        (slot, true)
    }
}
