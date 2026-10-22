use crate::{AssetProbe, AssetReport};
use openrs_scene::material::{Material, MaterialTable};

pub const MATERIAL_INDEX: u32 = 26;
pub const MATERIAL_ARCHIVE: u32 = 0;
pub const MATERIAL_FILE: u32 = 0;

pub struct WorldMaterialProbe {
    asset: Option<AssetProbe>,
    table: Option<MaterialTable>,
    failed: bool,
}

impl WorldMaterialProbe {
    pub fn new(token: &[u8]) -> Result<Self, String> {
        Ok(Self {
            asset: Some(AssetProbe::new(token, MATERIAL_INDEX, MATERIAL_ARCHIVE)?),
            table: None,
            failed: false,
        })
    }

    pub fn take_outgoing(&mut self) -> Vec<u8> {
        self.asset
            .as_mut()
            .map_or_else(Vec::new, AssetProbe::take_outgoing)
    }

    pub fn complete(&self) -> bool {
        !self.failed
            && self.table.is_some()
            && self.asset.as_ref().is_some_and(AssetProbe::complete)
    }

    pub fn report(&self) -> Option<&AssetReport> {
        if self.complete() {
            self.asset.as_ref().and_then(AssetProbe::report)
        } else {
            None
        }
    }

    pub fn table(&self) -> Result<&MaterialTable, String> {
        if !self.complete() {
            return Err("material probe not complete".into());
        }
        self.table
            .as_ref()
            .ok_or_else(|| "material table unavailable".into())
    }

    pub fn material(&self, id: i32) -> Result<&Material, String> {
        self.table()?
            .get_material(id)
            .map_err(|error| error.to_string())
    }

    pub fn feed(&mut self, bytes: &[u8]) -> Result<(), String> {
        if self.failed {
            return Err("material probe has failed".into());
        }
        let result = self
            .asset
            .as_mut()
            .ok_or_else(|| "material asset probe unavailable".to_string())
            .and_then(|asset| asset.feed(bytes))
            .and_then(|()| self.publish());
        if result.is_err() {
            self.fail();
        }
        result
    }

    pub fn finish(&mut self) -> Result<(), String> {
        if self.failed {
            return Err("material probe has failed".into());
        }
        let result = self
            .asset
            .as_mut()
            .ok_or_else(|| "material asset probe unavailable".to_string())
            .and_then(AssetProbe::finish)
            .and_then(|()| self.publish());
        if result.is_err() {
            self.fail();
        }
        result
    }

    fn publish(&mut self) -> Result<(), String> {
        if self.table.is_some() {
            return Ok(());
        }
        let asset = self
            .asset
            .as_ref()
            .ok_or_else(|| "material asset probe unavailable".to_string())?;
        if !asset.complete() {
            return Ok(());
        }
        let report = asset
            .report()
            .ok_or_else(|| "material asset report unavailable".to_string())?;
        if report.files.len() != 1 || report.files[0].id != MATERIAL_FILE {
            return Err("material archive must contain only file 0".into());
        }
        let table =
            MaterialTable::decode(asset.file(MATERIAL_FILE)?).map_err(|error| error.to_string())?;
        self.table = Some(table);
        Ok(())
    }

    fn fail(&mut self) {
        self.failed = true;
        self.table = None;
        self.asset = None;
    }
}
