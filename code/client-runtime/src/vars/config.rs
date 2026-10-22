pub mod decode;
pub mod idk;

use super::WorldVarHost;
use decode::RawItem;
use openrs_assets::{
    ArchiveFile, Integrity, Limits, ReferenceTable, decode_container, parse_reference_table,
    split_archive,
};
use openrs_cs2::{
    ConfigKind, ContainerType, HostError, InteractionColours, ItemContainer, ItemDefinition,
    ItemModel, ItemRecord, ParamDefinition, ParamTable,
};
use openrs_login::item_containers::ItemContainers;
use openrs_protocol::js5::VersionEntry;
use std::{collections::BTreeMap, sync::Arc};

pub const CONFIG_INDEX: u8 = 2;
pub const NPC_INDEX: u8 = 18;
pub const ITEM_INDEX: u8 = 19;
pub const SEQ_INDEX: u8 = 20;
pub const DEFAULTS_INDEX: u8 = 28;
pub const INVENTORY_GROUP: u32 = 5;
pub const PARAM_GROUP: u32 = 11;
pub const STRUCT_GROUP: u32 = 26;
pub const INTERACTIONS_FILE: u32 = 7;
pub const MAX_REFERENCE_BYTES: usize = 4 * 1024 * 1024;
pub const MAX_ARCHIVE_BYTES: usize = 8 * 1024 * 1024;
pub const MAX_INDEX_BYTES: usize = 48 * 1024 * 1024;
pub const MAX_LOADED_ARCHIVES: usize = 4096;
pub const MAX_DERIVATION_DEPTH: u32 = 16;
pub const MAX_ITEMS: usize = 1 << 20;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConfigError {
    Asset(openrs_assets::Error),
    UnknownIndex(u8),
    ReferenceUnavailable,
    ReferenceAlreadyLoaded,
    ArchiveAbsent(u32),
    ArchiveAlreadyLoaded(u32),
    LoadedArchiveLimit,
    ByteLimit,
}

impl std::fmt::Display for ConfigError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "config store: {self:?}")
    }
}

impl std::error::Error for ConfigError {}

impl From<openrs_assets::Error> for ConfigError {
    fn from(error: openrs_assets::Error) -> Self {
        Self::Asset(error)
    }
}

fn limits(max_bytes: usize) -> Limits {
    Limits {
        max_container_bytes: max_bytes,
        max_uncompressed_bytes: max_bytes,
        max_archives: 65_536,
        max_files_per_archive: 65_536,
        max_total_files: 1 << 21,
        max_archive_id: u16::MAX as u32,
        max_file_id: 65_535,
    }
}

fn corrupt(_: openrs_scene::packet::PacketError) -> HostError {
    HostError::State("corrupt definition".into())
}

#[derive(Debug, Default)]
struct ConfigIndex {
    reference: Option<ReferenceTable>,
    archives: BTreeMap<u32, Vec<ArchiveFile>>,
    bytes: usize,
}

impl ConfigIndex {
    fn load_reference(
        &mut self,
        container: &[u8],
        version: &VersionEntry,
    ) -> Result<(), ConfigError> {
        if self.reference.is_some() {
            return Err(ConfigError::ReferenceAlreadyLoaded);
        }
        let limits = limits(MAX_REFERENCE_BYTES);
        let bytes = decode_container(
            container,
            Integrity {
                crc32: Some(version.crc as u32),
                whirlpool: Some(&version.whirlpool),
            },
            &limits,
        )?;
        self.reference = Some(parse_reference_table(&bytes, &limits)?);
        Ok(())
    }

    fn load_archive(&mut self, id: u32, container: &[u8]) -> Result<(), ConfigError> {
        let reference = self
            .reference
            .as_ref()
            .ok_or(ConfigError::ReferenceUnavailable)?;
        let archive = reference
            .archive(id)
            .ok_or(ConfigError::ArchiveAbsent(id))?;
        if self.archives.contains_key(&id) {
            return Err(ConfigError::ArchiveAlreadyLoaded(id));
        }
        if self.archives.len() >= MAX_LOADED_ARCHIVES {
            return Err(ConfigError::LoadedArchiveLimit);
        }
        let remaining = MAX_INDEX_BYTES
            .checked_sub(self.bytes)
            .ok_or(ConfigError::ByteLimit)?;
        let limits = limits(MAX_ARCHIVE_BYTES.min(remaining));
        let bytes = decode_container(
            container,
            Integrity {
                crc32: Some(archive.crc32),
                whirlpool: archive.whirlpool.as_ref(),
            },
            &limits,
        )?;
        let files = split_archive(&bytes, &archive.files, &limits)?;
        self.bytes += bytes.len();
        self.archives.insert(id, files);
        Ok(())
    }

    fn file(&self, archive: u32, file: u32) -> Result<Option<&[u8]>, HostError> {
        let reference = self.reference.as_ref().ok_or(HostError::Unavailable)?;
        let Some(entry) = reference.archive(archive) else {
            return Ok(None);
        };
        if entry.file(file).is_none() {
            return Ok(None);
        }
        let files = self.archives.get(&archive).ok_or(HostError::Unavailable)?;
        Ok(files
            .binary_search_by_key(&file, |file| file.id)
            .ok()
            .map(|index| files[index].data.as_slice()))
    }

    fn files_of(&self, archive: u32) -> Result<Vec<u32>, HostError> {
        let reference = self.reference.as_ref().ok_or(HostError::Unavailable)?;
        Ok(reference
            .archive(archive)
            .map(|entry| entry.files.iter().map(|file| file.id).collect())
            .unwrap_or_default())
    }
}

#[derive(Debug, Default)]
pub struct ConfigStore {
    config: ConfigIndex,
    npc: ConfigIndex,
    item: ConfigIndex,
    seq: ConfigIndex,
    defaults: ConfigIndex,
    params: BTreeMap<i32, Arc<ParamDefinition>>,
    items: BTreeMap<i32, Arc<ItemDefinition>>,
    tables: BTreeMap<(u8, i32), Arc<ParamTable>>,
    container_types: BTreeMap<i32, ContainerType>,
    colours: Option<InteractionColours>,
    closed: bool,
}

fn kind_index(kind: ConfigKind) -> u8 {
    match kind {
        ConfigKind::Npc => 0,
        ConfigKind::Seq => 1,
        ConfigKind::Struct => 2,
    }
}

impl ConfigStore {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn closed(mut self) -> Self {
        self.closed = true;
        self
    }

    fn index_mut(&mut self, index: u8) -> Result<&mut ConfigIndex, ConfigError> {
        match index {
            CONFIG_INDEX => Ok(&mut self.config),
            NPC_INDEX => Ok(&mut self.npc),
            ITEM_INDEX => Ok(&mut self.item),
            SEQ_INDEX => Ok(&mut self.seq),
            DEFAULTS_INDEX => Ok(&mut self.defaults),
            other => Err(ConfigError::UnknownIndex(other)),
        }
    }

    pub fn load_reference(
        &mut self,
        index: u8,
        container: &[u8],
        version: &VersionEntry,
    ) -> Result<(), ConfigError> {
        self.index_mut(index)?.load_reference(container, version)
    }

    pub fn load_archive(
        &mut self,
        index: u8,
        archive: u32,
        container: &[u8],
    ) -> Result<(), ConfigError> {
        self.index_mut(index)?.load_archive(archive, container)
    }

    pub fn insert_param(&mut self, id: i32, definition: ParamDefinition) {
        self.params.insert(id, Arc::new(definition));
    }

    pub fn insert_item(&mut self, id: i32, definition: ItemDefinition) {
        self.items.insert(id, Arc::new(definition));
    }

    pub fn insert_table(&mut self, kind: ConfigKind, id: i32, table: ParamTable) {
        self.tables.insert((kind_index(kind), id), Arc::new(table));
    }

    pub fn insert_container_type(&mut self, id: i32, container: ContainerType) {
        self.container_types.insert(id, container);
    }

    pub fn set_colours(&mut self, colours: InteractionColours) {
        self.colours = Some(colours);
    }

    fn miss<T>(&self) -> Result<Option<T>, HostError> {
        if self.closed {
            Ok(None)
        } else {
            Err(HostError::Unavailable)
        }
    }

    pub fn param_definition(&self, id: i32) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        if let Some(found) = self.params.get(&id) {
            return Ok(Some(Arc::clone(found)));
        }
        let Ok(file) = u32::try_from(id) else {
            return Ok(None);
        };
        if self.config.reference.is_none() {
            return self.miss();
        }
        match self.config.file(PARAM_GROUP, file)? {
            Some(bytes) => Ok(Some(Arc::new(
                decode::decode_param(bytes).map_err(corrupt)?,
            ))),
            None => Ok(None),
        }
    }

    pub fn config_params(
        &self,
        kind: ConfigKind,
        id: i32,
    ) -> Result<Option<Arc<ParamTable>>, HostError> {
        if let Some(found) = self.tables.get(&(kind_index(kind), id)) {
            return Ok(Some(Arc::clone(found)));
        }
        let Ok(id) = u32::try_from(id) else {
            return Ok(None);
        };
        let (index, archive, file) = match kind {
            ConfigKind::Npc => (&self.npc, id >> 7, id & 127),
            ConfigKind::Seq => (&self.seq, id >> 7, id & 127),
            ConfigKind::Struct => (&self.config, STRUCT_GROUP, id),
        };
        if index.reference.is_none() {
            return self.miss();
        }
        let Some(bytes) = index.file(archive, file)? else {
            return Ok(None);
        };
        let table = match kind {
            ConfigKind::Npc => decode::decode_npc_params(bytes),
            ConfigKind::Seq => decode::decode_seq_params(bytes),
            ConfigKind::Struct => decode::decode_params_only(bytes),
        }
        .map_err(corrupt)?;
        Ok(table.map(Arc::new))
    }

    pub fn interaction_colours(&self) -> Result<InteractionColours, HostError> {
        if let Some(colours) = self.colours {
            return Ok(colours);
        }
        let index = &self.defaults;
        let reference = index.reference.as_ref().ok_or(HostError::Unavailable)?;
        let (archive, file) = if reference.archives.len() == 1 {
            (reference.archives[0].id, INTERACTIONS_FILE)
        } else {
            (INTERACTIONS_FILE, 0)
        };
        match index.file(archive, file)? {
            Some(bytes) => decode::decode_interaction_colours(bytes).map_err(corrupt),
            None => Ok(InteractionColours::default()),
        }
    }

    fn raw_item(&self, id: i32) -> Result<RawItem, HostError> {
        let Ok(unsigned) = u32::try_from(id) else {
            return Ok(RawItem::default());
        };
        match self.item.file(unsigned >> 8, unsigned & 255)? {
            Some(bytes) => decode::decode_item(bytes).map_err(corrupt),
            None => Ok(RawItem::default()),
        }
    }

    fn derived_item(&self, id: i32, depth: u32) -> Result<(RawItem, ItemDefinition), HostError> {
        let raw = self.raw_item(id)?;
        let mut item = raw.definition.clone();
        if depth < MAX_DERIVATION_DEPTH {
            if raw.definition.cert_template_id != -1 {
                let (_, original) = self.derived_item(raw.definition.cert_id, depth + 1)?;
                decode::derive_note(&mut item, &original);
            }
            if raw.lend_template_id != -1 {
                let (_, original) = self.derived_item(raw.lend_id, depth + 1)?;
                decode::derive_lend(&mut item, &original);
            }
            if raw.bind_template_id != -1 {
                let (_, original) = self.derived_item(raw.bind_id, depth + 1)?;
                decode::derive_bind(&mut item, &original);
            }
        }
        Ok((raw, item))
    }

    pub fn item_definition(&self, id: i32) -> Result<Option<Arc<ItemDefinition>>, HostError> {
        if let Some(found) = self.items.get(&id) {
            return Ok(Some(Arc::clone(found)));
        }
        if self.item.reference.is_none() {
            return self.miss();
        }
        Ok(Some(Arc::new(self.derived_item(id, 0)?.1)))
    }

    pub fn populate_items(&self, model: &mut ItemModel) -> Result<(), HostError> {
        let reference = self.item.reference.as_ref().ok_or(HostError::Unavailable)?;
        let mut items = Vec::new();
        for archive in &reference.archives {
            for file in self.item.files_of(archive.id)? {
                let id = (archive.id << 8 | file) as usize;
                if id >= MAX_ITEMS {
                    return Err(HostError::State("item count over limit".into()));
                }
                let (raw, item) = self.derived_item(id as i32, 0)?;
                if items.len() <= id {
                    items.resize(id + 1, ItemRecord::default());
                }
                items[id] = ItemRecord {
                    name: item.name,
                    tradeable: raw.tradeable,
                    unknown_int6: raw.unknown_int6,
                    cert_template_id: raw.definition.cert_template_id,
                    lend_template_id: raw.lend_template_id,
                    bind_template_id: raw.bind_template_id,
                    params: item.params,
                };
            }
        }
        model.items = items;
        Ok(())
    }

    pub fn populate_container_types(&self, model: &mut ItemModel) -> Result<(), HostError> {
        for (id, container) in &self.container_types {
            model.container_types.insert(*id, container.clone());
        }
        if self.config.reference.is_none() {
            return if self.closed || !self.container_types.is_empty() {
                Ok(())
            } else {
                Err(HostError::Unavailable)
            };
        }
        for id in self.config.files_of(INVENTORY_GROUP)? {
            if let Some(bytes) = self.config.file(INVENTORY_GROUP, id)? {
                model.container_types.insert(
                    id as i32,
                    decode::decode_container_type(bytes).map_err(corrupt)?,
                );
            }
        }
        Ok(())
    }
}

pub fn sync_containers(model: &mut ItemModel, containers: &ItemContainers) {
    model.containers = containers
        .containers()
        .iter()
        .map(|container| {
            let slots = container.slots();
            (
                container.key().native_key(),
                ItemContainer {
                    item_ids: slots.iter().map(|slot| slot.item_id).collect(),
                    amounts: slots.iter().map(|slot| slot.amount).collect(),
                    tooltips: slots
                        .iter()
                        .map(|slot| slot.tooltip.as_deref().map(Arc::from))
                        .collect(),
                },
            )
        })
        .collect();
}

#[cfg(test)]
mod tests;

#[derive(Default)]
pub(super) struct ConfigBinding<'a> {
    store: Option<&'a ConfigStore>,
    items: Option<&'a mut ItemModel>,
}

impl<'a> ConfigBinding<'a> {
    pub(super) fn items(&mut self) -> Result<&mut ItemModel, HostError> {
        self.items.as_deref_mut().ok_or(HostError::Unavailable)
    }

    fn store(&self) -> Result<&'a ConfigStore, HostError> {
        self.store.ok_or(HostError::Unavailable)
    }

    pub(super) fn item_definition(
        &self,
        id: i32,
    ) -> Result<Option<Arc<ItemDefinition>>, HostError> {
        self.store()?.item_definition(id)
    }

    pub(super) fn param_definition(
        &self,
        id: i32,
    ) -> Result<Option<Arc<ParamDefinition>>, HostError> {
        self.store()?.param_definition(id)
    }

    pub(super) fn config_params(
        &self,
        kind: ConfigKind,
        id: i32,
    ) -> Result<Option<Arc<ParamTable>>, HostError> {
        self.store()?.config_params(kind, id)
    }

    pub(super) fn interaction_colours(&self) -> Result<InteractionColours, HostError> {
        self.store()?.interaction_colours()
    }
}

impl<'a> WorldVarHost<'a> {
    pub fn with_config(mut self, store: &'a ConfigStore, items: &'a mut ItemModel) -> Self {
        self.config.store = Some(store);
        self.config.items = Some(items);
        self
    }
}
