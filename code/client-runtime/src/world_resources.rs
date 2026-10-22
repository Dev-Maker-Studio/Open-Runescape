use crate::world_scene::ShapeSource;
use openrs_entity::config::{NpcType, Resources};
use openrs_login::world_packets::zone::CustomizeCounts;
use openrs_protocol::entity_info::NpcTypeShape;
use openrs_scene::loc_type::decode_loc_type;
use std::cell::RefCell;
use std::collections::{BTreeSet, HashMap, HashSet};
use std::rc::Rc;

pub const INDEX_FRAMESET: u8 = 0;
pub const INDEX_ANIM_BASE: u8 = 1;
pub const INDEX_CONFIG: u8 = 2;
pub const INDEX_MODELS: u8 = 7;
pub const INDEX_LOC: u8 = 16;
pub const INDEX_NPC: u8 = 18;
pub const INDEX_ITEM: u8 = 19;
pub const INDEX_SEQUENCE: u8 = 20;
pub const INDEX_GFX: u8 = 21;
pub const INDEX_DEFAULTS: u8 = 28;
pub const CONFIG_IDK: u32 = 3;
pub const CONFIG_BAS: u32 = 32;

pub type ArchiveKey = (u8, u32);
pub type FrameFiles = Vec<(i32, Vec<u8>)>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Lookup {
    Present(Vec<u8>),
    Absent,
    Unknown,
}

impl Lookup {
    pub fn bytes(self) -> Option<Vec<u8>> {
        match self {
            Lookup::Present(bytes) => Some(bytes),
            _ => None,
        }
    }
}

#[derive(Default)]
struct Inner {
    files: HashMap<(u8, u32, u32), Vec<u8>>,
    loaded: HashSet<ArchiveKey>,
    needs: BTreeSet<ArchiveKey>,
    version: u64,
}

#[derive(Clone, Default)]
pub struct ResourceStore {
    inner: Rc<RefCell<Inner>>,
}

impl ResourceStore {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn lookup(&self, index: u8, archive: u32, file: u32) -> Lookup {
        let mut inner = self.inner.borrow_mut();
        if let Some(bytes) = inner.files.get(&(index, archive, file)) {
            return Lookup::Present(bytes.clone());
        }
        if inner.loaded.contains(&(index, archive)) {
            return Lookup::Absent;
        }
        inner.needs.insert((index, archive));
        Lookup::Unknown
    }

    pub fn needs(&self) -> Vec<ArchiveKey> {
        let inner = self.inner.borrow();
        inner
            .needs
            .iter()
            .filter(|key| !inner.loaded.contains(key))
            .copied()
            .collect()
    }

    pub fn clear_needs(&self) {
        self.inner.borrow_mut().needs.clear();
    }

    pub fn version(&self) -> u64 {
        self.inner.borrow().version
    }

    pub fn is_loaded(&self, index: u8, archive: u32) -> bool {
        self.inner.borrow().loaded.contains(&(index, archive))
    }

    pub fn provide_group(&self, index: u8, archive: u32, files: Vec<(u32, Vec<u8>)>) {
        let mut inner = self.inner.borrow_mut();
        for (file, bytes) in files {
            inner.files.insert((index, archive, file), bytes);
        }
        inner.loaded.insert((index, archive));
        inner.needs.remove(&(index, archive));
        inner.version += 1;
    }

    pub fn add_file(&self, index: u8, archive: u32, file: u32, bytes: Vec<u8>) {
        self.inner
            .borrow_mut()
            .files
            .insert((index, archive, file), bytes);
    }

    pub fn finish_group(&self, index: u8, archive: u32) {
        let mut inner = self.inner.borrow_mut();
        inner.loaded.insert((index, archive));
        inner.needs.remove(&(index, archive));
        inner.version += 1;
    }

    pub fn provide_absent(&self, index: u8, archive: u32) {
        let mut inner = self.inner.borrow_mut();
        inner.loaded.insert((index, archive));
        inner.needs.remove(&(index, archive));
        inner.version += 1;
    }

    pub fn provide_file(&self, index: u8, archive: u32, file: u32, bytes: Vec<u8>) {
        let mut inner = self.inner.borrow_mut();
        inner.files.insert((index, archive, file), bytes);
        inner.loaded.insert((index, archive));
        inner.needs.remove(&(index, archive));
        inner.version += 1;
    }

    pub fn clear(&self) {
        *self.inner.borrow_mut() = Inner::default();
    }

    pub fn model(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_MODELS, id, 0),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn loc_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_LOC, id >> 8, id & 0xff),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn varbit_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(
                crate::vars::varbit::VARBIT_INDEX,
                id >> crate::vars::varbit::VARBIT_ARCHIVE_BITS,
                id & 0x3ff,
            ),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn npc_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_NPC, id >> 7, id & 0x7f),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn item_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_ITEM, id >> 8, id & 0xff),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn idk_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_CONFIG, CONFIG_IDK, id),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn bas_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_CONFIG, CONFIG_BAS, id),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn sequence_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_SEQUENCE, id >> 7, id & 0x7f),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn spot_anim_type(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_GFX, id >> 8, id & 0xff),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn equipment_defaults(&self) -> Lookup {
        self.lookup(INDEX_DEFAULTS, 6, 0)
    }

    pub fn anim_base(&self, id: i32) -> Lookup {
        match u32::try_from(id) {
            Ok(id) => self.lookup(INDEX_ANIM_BASE, id, 0),
            Err(_) => Lookup::Absent,
        }
    }

    pub fn frameset_files(&self, group: i32) -> Option<Option<FrameFiles>> {
        let group = u32::try_from(group).ok()?;
        let mut inner = self.inner.borrow_mut();
        if !inner.loaded.contains(&(INDEX_FRAMESET, group)) {
            inner.needs.insert((INDEX_FRAMESET, group));
            return None;
        }
        let mut files: Vec<(i32, Vec<u8>)> = inner
            .files
            .iter()
            .filter(|((index, archive, _), _)| *index == INDEX_FRAMESET && *archive == group)
            .map(|((_, _, file), bytes)| (*file as i32, bytes.clone()))
            .collect();
        files.sort_by_key(|(file, _)| *file);
        if files.is_empty() {
            Some(None)
        } else {
            Some(Some(files))
        }
    }
}

impl Resources for ResourceStore {
    fn model(&self, id: i32) -> Option<Vec<u8>> {
        ResourceStore::model(self, id).bytes()
    }

    fn item(&self, id: i32) -> Option<Vec<u8>> {
        self.item_type(id).bytes()
    }

    fn idk(&self, id: i32) -> Option<Vec<u8>> {
        self.idk_type(id).bytes()
    }

    fn bas(&self, id: i32) -> Option<Vec<u8>> {
        self.bas_type(id).bytes()
    }

    fn npc(&self, id: i32) -> Option<Vec<u8>> {
        self.npc_type(id).bytes()
    }
}

fn len<T>(list: &Option<Vec<T>>) -> usize {
    list.as_ref().map_or(0, Vec::len)
}

impl ShapeSource for ResourceStore {
    fn npc_shape(&self, type_id: i32) -> Option<NpcTypeShape> {
        match self.npc_type(type_id) {
            Lookup::Unknown => None,
            Lookup::Absent => Some(NpcTypeShape::default()),
            Lookup::Present(bytes) => {
                let Ok(ty) = NpcType::decode(type_id, &bytes) else {
                    return Some(NpcTypeShape::default());
                };
                Some(NpcTypeShape {
                    models: len(&ty.model_ids),
                    colors: len(&ty.recolor_src),
                    textures: len(&ty.retex_src),
                    head_models: len(&ty.head_models),
                })
            }
        }
    }

    fn customize_counts(&self, loc_id: i32, shape: u8) -> Option<CustomizeCounts> {
        match self.loc_type(loc_id) {
            Lookup::Unknown => None,
            Lookup::Absent => Some(CustomizeCounts::default()),
            Lookup::Present(bytes) => {
                let Ok(loc) = decode_loc_type(loc_id, Some(&bytes), true) else {
                    return Some(CustomizeCounts::default());
                };
                let models = loc
                    .shapes
                    .as_ref()
                    .and_then(|shapes| shapes.iter().position(|value| *value == shape as i8))
                    .and_then(|slot| loc.model_ids.as_ref()?.get(slot).map(Vec::len))
                    .unwrap_or(0);
                Some(CustomizeCounts {
                    models,
                    recolors: len(&loc.recolor_src),
                    retextures: len(&loc.retexture_src),
                })
            }
        }
    }
}
