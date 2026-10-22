use openrs_assets::{
    ArchiveFile, Integrity, Limits, ReferenceTable, decode_container, parse_reference_table,
    split_archive,
};
use openrs_cs2::{QuickChatDecodeError, QuickChatDefs};

pub const MESSAGE_INDEX: u8 = 24;
pub const MENU_INDEX: u8 = 25;
pub const CATEGORY_ARCHIVE: u32 = 0;
pub const MESSAGE_ARCHIVE: u32 = 1;
pub const MAX_REFERENCE_BYTES: usize = 4 * 1024 * 1024;
pub const MAX_ARCHIVE_BYTES: usize = 8 * 1024 * 1024;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum QuickChatStoreError {
    Asset(openrs_assets::Error),
    UnknownIndex(u8),
    ReferenceUnavailable(u8),
    ReferenceAlreadyLoaded(u8),
    ArchiveAbsent(u8, u32),
    ArchiveAlreadyLoaded(u8, u32),
    Decode(u8, u32, u32),
}

impl std::fmt::Display for QuickChatStoreError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "quick chat store: {self:?}")
    }
}

impl std::error::Error for QuickChatStoreError {}

impl From<openrs_assets::Error> for QuickChatStoreError {
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

#[derive(Debug, Default)]
struct Source {
    reference: Option<ReferenceTable>,
    categories: Option<Vec<ArchiveFile>>,
    messages: Option<Vec<ArchiveFile>>,
}

impl Source {
    fn file_count(&self, archive: u32) -> i32 {
        self.reference
            .as_ref()
            .and_then(|reference| reference.archive(archive))
            .and_then(|archive| archive.files.last())
            .map_or(0, |file| file.id as i32 + 1)
    }
}

#[derive(Debug, Default)]
pub struct QuickChatStore {
    messages: Source,
    menu: Source,
}

impl QuickChatStore {
    pub fn new() -> Self {
        Self::default()
    }

    fn source_mut(&mut self, index: u8) -> Result<&mut Source, QuickChatStoreError> {
        match index {
            MESSAGE_INDEX => Ok(&mut self.messages),
            MENU_INDEX => Ok(&mut self.menu),
            other => Err(QuickChatStoreError::UnknownIndex(other)),
        }
    }

    pub fn load_reference(
        &mut self,
        index: u8,
        container: &[u8],
        integrity: Integrity<'_>,
    ) -> Result<(), QuickChatStoreError> {
        let source = self.source_mut(index)?;
        if source.reference.is_some() {
            return Err(QuickChatStoreError::ReferenceAlreadyLoaded(index));
        }
        let limits = limits(MAX_REFERENCE_BYTES);
        let bytes = decode_container(container, integrity, &limits)?;
        source.reference = Some(parse_reference_table(&bytes, &limits)?);
        Ok(())
    }

    pub fn load_archive(
        &mut self,
        index: u8,
        archive: u32,
        container: &[u8],
    ) -> Result<(), QuickChatStoreError> {
        let source = self.source_mut(index)?;
        let reference = source
            .reference
            .as_ref()
            .ok_or(QuickChatStoreError::ReferenceUnavailable(index))?;
        let entry = reference
            .archive(archive)
            .ok_or(QuickChatStoreError::ArchiveAbsent(index, archive))?;
        let limits = limits(MAX_ARCHIVE_BYTES);
        let bytes = decode_container(
            container,
            Integrity {
                crc32: Some(entry.crc32),
                whirlpool: entry.whirlpool.as_ref(),
            },
            &limits,
        )?;
        let files = split_archive(&bytes, &entry.files, &limits)?;
        let slot = match archive {
            CATEGORY_ARCHIVE => &mut source.categories,
            MESSAGE_ARCHIVE => &mut source.messages,
            _ => return Err(QuickChatStoreError::ArchiveAbsent(index, archive)),
        };
        if slot.is_some() {
            return Err(QuickChatStoreError::ArchiveAlreadyLoaded(index, archive));
        }
        *slot = Some(files);
        Ok(())
    }

    pub fn build(&self) -> Result<QuickChatDefs, QuickChatStoreError> {
        let mut defs = QuickChatDefs::new(
            self.messages.file_count(MESSAGE_ARCHIVE),
            self.menu.file_count(MESSAGE_ARCHIVE),
        );
        for (menu, index, source) in [
            (false, MESSAGE_INDEX, &self.messages),
            (true, MENU_INDEX, &self.menu),
        ] {
            for file in source.categories.iter().flatten() {
                defs.insert_category(menu, file.id as i32, &file.data)
                    .map_err(|QuickChatDecodeError| {
                        QuickChatStoreError::Decode(index, CATEGORY_ARCHIVE, file.id)
                    })?;
            }
            for file in source.messages.iter().flatten() {
                defs.insert_message(menu, file.id as i32, &file.data)
                    .map_err(|QuickChatDecodeError| {
                        QuickChatStoreError::Decode(index, MESSAGE_ARCHIVE, file.id)
                    })?;
            }
        }
        Ok(defs)
    }

    pub fn file_ids(&self, index: u8, archive: u32) -> Vec<u32> {
        let source = match index {
            MESSAGE_INDEX => &self.messages,
            MENU_INDEX => &self.menu,
            _ => return Vec::new(),
        };
        let files = match archive {
            CATEGORY_ARCHIVE => &source.categories,
            MESSAGE_ARCHIVE => &source.messages,
            _ => return Vec::new(),
        };
        files.iter().flatten().map(|file| file.id).collect()
    }
}
