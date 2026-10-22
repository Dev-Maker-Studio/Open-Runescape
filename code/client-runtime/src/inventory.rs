use openrs_login::item_containers::{ContainerKey, ItemContainers, Limits, UpdateKind};
use openrs_protocol::registry::ServerPacket;
use serde::Serialize;

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldInventoryReport {
    pub containers: usize,
    pub slots: usize,
    pub tooltip_bytes: usize,
    pub updates: usize,
    pub full_updates: usize,
    pub partial_updates: usize,
    pub stops: usize,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldContainerReport {
    pub id: u16,
    pub negative_key: bool,
    pub slots: usize,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WorldItemReport {
    pub item_id: i32,
    pub amount: i32,
    pub tooltip: Option<String>,
}

pub(crate) struct LiveInventory {
    pub(crate) bank: ItemContainers,
    full_updates: usize,
    partial_updates: usize,
    stops: usize,
}

impl LiveInventory {
    pub(crate) fn new(limits: Limits) -> Result<Self, &'static str> {
        Ok(Self {
            bank: ItemContainers::new(limits).map_err(|_| "invalid world inventory limits")?,
            full_updates: 0,
            partial_updates: 0,
            stops: 0,
        })
    }

    pub(crate) fn report(&self) -> WorldInventoryReport {
        WorldInventoryReport {
            containers: self.bank.len(),
            slots: self.bank.total_slots(),
            tooltip_bytes: self.bank.tooltip_bytes(),
            updates: self.full_updates + self.partial_updates + self.stops,
            full_updates: self.full_updates,
            partial_updates: self.partial_updates,
            stops: self.stops,
        }
    }

    pub(crate) fn container(&self, key: ContainerKey) -> Option<WorldContainerReport> {
        self.bank.get(key).map(|container| WorldContainerReport {
            id: key.id,
            negative_key: key.negative_key,
            slots: container.len(),
        })
    }

    pub(crate) fn item(&self, key: ContainerKey, slot: usize) -> Option<WorldItemReport> {
        self.bank
            .get(key)
            .and_then(|container| container.slots().get(slot))
            .map(|item| WorldItemReport {
                item_id: item.item_id,
                amount: item.amount,
                tooltip: item.tooltip.clone(),
            })
    }

    pub(crate) fn apply(
        &mut self,
        opcode: ServerPacket,
        payload: &[u8],
    ) -> Result<openrs_login::item_containers::Update, &'static str> {
        let update = self
            .bank
            .apply(opcode, payload)
            .map_err(|_| "invalid world inventory packet")?;
        match update.kind {
            UpdateKind::Full => self.full_updates += 1,
            UpdateKind::Partial => self.partial_updates += 1,
            UpdateKind::StopTransmit => self.stops += 1,
        }
        Ok(update)
    }
}
