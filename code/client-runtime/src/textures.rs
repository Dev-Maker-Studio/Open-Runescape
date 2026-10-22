use openrs_assets::{
    MaterialSource, MaterialTable, TextureGraph, TextureSources, parse_material_table,
    render_material,
};
use std::collections::{BTreeSet, HashMap};

pub const SPRITE_INDEX: u32 = 8;
pub const TEXTURE_INDEX: u32 = 9;
const MAX_DEPTH: usize = 16;
const MAX_SPRITES: usize = 4096;
const MAX_GRAPHS: usize = 4096;
const MAX_BYTES: usize = 64 * 1024 * 1024;

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct TextureNeeds {
    pub sprites: Vec<u32>,
    pub graphs: Vec<u32>,
}

#[derive(Default)]
pub struct TextureSet {
    table: Option<MaterialTable>,
    sprites: HashMap<u32, Vec<u8>>,
    graphs: HashMap<u32, Vec<u8>>,
    bytes: usize,
}

struct Sources<'a> {
    set: &'a TextureSet,
}

impl TextureSources for Sources<'_> {
    fn sprite(&self, id: u32) -> Option<&[u8]> {
        self.set.sprites.get(&id).map(Vec::as_slice)
    }

    fn material(&self, id: u32) -> Option<MaterialSource<'_>> {
        let kind = self.set.table.as_ref()?.get(id as usize)?;
        let texture = self.set.graphs.get(&id)?;
        Some(MaterialSource {
            texture,
            is_small: kind.is_small,
            is_brick_tile: kind.is_brick_tile,
        })
    }
}

impl TextureSet {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn set_table(&mut self, payload: &[u8]) -> Result<usize, String> {
        let table = parse_material_table(payload).map_err(|error| error.to_string())?;
        let len = table.len();
        self.table = Some(table);
        Ok(len)
    }

    pub fn install_table(&mut self, table: MaterialTable) {
        self.table = Some(table);
    }

    pub fn has_table(&self) -> bool {
        self.table.is_some()
    }

    pub fn sprite_count(&self) -> usize {
        self.sprites.len()
    }

    pub fn graph_count(&self) -> usize {
        self.graphs.len()
    }

    pub fn add_sprite(&mut self, id: u32, bytes: &[u8]) -> Result<(), String> {
        if self.sprites.len() >= MAX_SPRITES && !self.sprites.contains_key(&id) {
            return Err("sprite limit exceeded".into());
        }
        self.charge(bytes.len(), self.sprites.get(&id).map_or(0, Vec::len))?;
        self.sprites.insert(id, bytes.to_vec());
        Ok(())
    }

    pub fn add_graph(&mut self, id: u32, bytes: &[u8]) -> Result<(), String> {
        if self.graphs.len() >= MAX_GRAPHS && !self.graphs.contains_key(&id) {
            return Err("texture graph limit exceeded".into());
        }
        TextureGraph::decode(bytes).map_err(|error| error.to_string())?;
        self.charge(bytes.len(), self.graphs.get(&id).map_or(0, Vec::len))?;
        self.graphs.insert(id, bytes.to_vec());
        Ok(())
    }

    fn charge(&mut self, added: usize, replaced: usize) -> Result<(), String> {
        let next = self.bytes - replaced + added;
        if next > MAX_BYTES {
            return Err("texture byte limit exceeded".into());
        }
        self.bytes = next;
        Ok(())
    }

    pub fn needs(&self, material: u32) -> Result<TextureNeeds, String> {
        let table = self.table.as_ref().ok_or("material table unavailable")?;
        let mut sprites = BTreeSet::new();
        let mut graphs = BTreeSet::new();
        let mut seen = BTreeSet::new();
        let mut stack = vec![(material, 0usize)];
        while let Some((id, depth)) = stack.pop() {
            if depth > MAX_DEPTH {
                return Err("material nesting too deep".into());
            }
            if !seen.insert(id) {
                continue;
            }
            if table.get(id as usize).is_none() {
                return Err(format!("material {id} has no table entry"));
            }
            let Some(bytes) = self.graphs.get(&id) else {
                graphs.insert(id);
                continue;
            };
            let graph = TextureGraph::decode(bytes).map_err(|error| error.to_string())?;
            for sprite in graph.texture_file_ids() {
                if !self.sprites.contains_key(sprite) {
                    sprites.insert(*sprite);
                }
            }
            for child in graph.material_file_ids() {
                stack.push((*child, depth + 1));
            }
        }
        Ok(TextureNeeds {
            sprites: sprites.into_iter().collect(),
            graphs: graphs.into_iter().collect(),
        })
    }

    pub fn size(&self, material: u32) -> Result<usize, String> {
        let kind = self
            .table
            .as_ref()
            .ok_or("material table unavailable")?
            .get(material as usize)
            .ok_or_else(|| format!("material {material} has no table entry"))?;
        Ok(if kind.is_small { 64 } else { 128 })
    }

    pub fn render_argb(&self, material: u32) -> Result<Vec<u32>, String> {
        let needs = self.needs(material)?;
        if !needs.sprites.is_empty() || !needs.graphs.is_empty() {
            return Err(format!("material {material} inputs incomplete"));
        }
        let kind = self
            .table
            .as_ref()
            .and_then(|table| table.get(material as usize))
            .ok_or_else(|| format!("material {material} has no table entry"))?;
        let texture = self
            .graphs
            .get(&material)
            .ok_or_else(|| format!("material {material} graph missing"))?;
        render_material(texture, kind, &Sources { set: self }).map_err(|error| error.to_string())
    }

    pub fn render_rgba(&self, material: u32) -> Result<Vec<u8>, String> {
        let pixels = self.render_argb(material)?;
        let mut out = Vec::with_capacity(pixels.len() * 4);
        for pixel in pixels {
            out.push((pixel >> 16) as u8);
            out.push((pixel >> 8) as u8);
            out.push(pixel as u8);
            out.push((pixel >> 24) as u8);
        }
        Ok(out)
    }
}

pub fn argb_le_bytes(pixels: &[u32]) -> Vec<u8> {
    let mut bytes = Vec::with_capacity(pixels.len() * 4);
    for pixel in pixels {
        bytes.extend_from_slice(&pixel.to_le_bytes());
    }
    bytes
}
