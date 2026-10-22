use crate::{EnumValue, HostError, ParamTable};
use std::{collections::BTreeMap, sync::Arc};

pub const QUERY_LIMIT: usize = 250;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ItemContainer {
    pub item_ids: Vec<i32>,
    pub amounts: Vec<i32>,
    pub tooltips: Vec<Option<Arc<str>>>,
}

impl Default for ItemContainer {
    fn default() -> Self {
        Self {
            item_ids: vec![-1],
            amounts: vec![0],
            tooltips: vec![None],
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ContainerType {
    pub default_item_size: i32,
    pub max_item_size: i32,
    pub item_ids: Vec<i32>,
    pub amounts: Vec<i32>,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ItemRecord {
    pub name: Arc<str>,
    pub tradeable: bool,
    pub unknown_int6: i32,
    pub cert_template_id: i32,
    pub lend_template_id: i32,
    pub bind_template_id: i32,
    pub params: Option<ParamTable>,
}

impl Default for ItemRecord {
    fn default() -> Self {
        Self {
            name: Arc::from("null"),
            tradeable: false,
            unknown_int6: 0,
            cert_template_id: -1,
            lend_template_id: -1,
            bind_template_id: -1,
            params: None,
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct QueryResults {
    pub ids: Option<Vec<u16>>,
    pub count: i32,
    pub cursor: i32,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum QueryParam<'a> {
    None,
    Int { id: i32, default: i32, query: i32 },
    Text { id: i32, default: &'a str, query: &'a str },
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct ItemModel {
    pub containers: BTreeMap<i64, ItemContainer>,
    pub container_types: BTreeMap<i32, ContainerType>,
    pub items: Vec<ItemRecord>,
    pub query: QueryResults,
}

pub fn container_key(id: i32, other_party: bool) -> i64 {
    i64::from(id) | if other_party { i64::from(i32::MIN) } else { 0 }
}

fn integer_param(table: Option<&ParamTable>, id: i32, default: i32) -> Result<i32, HostError> {
    match table.and_then(|table| table.get(&id)) {
        None => Ok(default),
        Some(EnumValue::Int(value)) => Ok(*value),
        Some(EnumValue::Text(_)) => Err(HostError::EntityType),
    }
}

fn text_param<'a>(
    table: Option<&'a ParamTable>,
    id: i32,
    default: &'a str,
) -> Result<&'a str, HostError> {
    match table.and_then(|table| table.get(&id)) {
        None => Ok(default),
        Some(EnumValue::Text(value)) => Ok(value),
        Some(EnumValue::Int(_)) => Err(HostError::EntityType),
    }
}

pub(crate) fn compare(left: &str, right: &str) -> i32 {
    let mut a = left.encode_utf16();
    let mut b = right.encode_utf16();
    loop {
        match (a.next(), b.next()) {
            (Some(x), Some(y)) if x != y => return i32::from(x) - i32::from(y),
            (Some(_), Some(_)) => {}
            _ => break,
        }
    }
    left.encode_utf16().count() as i32 - right.encode_utf16().count() as i32
}

pub(crate) fn quicksort(names: &mut [Arc<str>], ids: &mut [u16], from: isize, to: isize) {
    if from >= to {
        return;
    }
    let middle = (to + from) / 2;
    let mut partition = from;
    let pivot = Arc::clone(&names[middle as usize]);
    names.swap(middle as usize, to as usize);
    ids.swap(middle as usize, to as usize);
    for i in from..to {
        if compare(&names[i as usize], &pivot) < (i & 1) as i32 {
            names.swap(i as usize, partition as usize);
            ids.swap(i as usize, partition as usize);
            partition += 1;
        }
    }
    names.swap(to as usize, partition as usize);
    ids.swap(to as usize, partition as usize);
    quicksort(names, ids, from, partition - 1);
    quicksort(names, ids, partition + 1, to);
}

impl ItemModel {
    pub fn container(&self, id: i32, other_party: bool) -> Option<&ItemContainer> {
        self.containers.get(&container_key(id, other_party))
    }

    pub fn container_type(&self, id: i32) -> ContainerType {
        self.container_types.get(&id).cloned().unwrap_or_default()
    }

    pub fn amount_at(&self, id: i32, slot: i32, other_party: bool) -> i32 {
        self.container(id, other_party)
            .and_then(|container| usize::try_from(slot).ok().and_then(|s| container.amounts.get(s)))
            .copied()
            .unwrap_or(0)
    }

    pub fn item_at(&self, id: i32, slot: i32, other_party: bool) -> i32 {
        self.container(id, other_party)
            .and_then(|container| {
                usize::try_from(slot)
                    .ok()
                    .and_then(|s| container.item_ids.get(s))
            })
            .copied()
            .unwrap_or(-1)
    }

    pub fn find_tooltip_slot(&self, id: i32, preferred: i32, item: i32, other_party: bool) -> i32 {
        let Some(container) = self.container(id, other_party) else {
            return -1;
        };
        if preferred >= 0
            && (preferred as usize) < container.item_ids.len()
            && container.item_ids[preferred as usize] == item
        {
            return preferred;
        }
        container
            .item_ids
            .iter()
            .position(|candidate| *candidate == item)
            .map_or(-1, |position| position as i32)
    }

    pub fn tooltip_at(&self, id: i32, slot: i32, other_party: bool) -> Arc<str> {
        self.container(id, other_party)
            .and_then(|container| {
                usize::try_from(slot)
                    .ok()
                    .and_then(|s| container.tooltips.get(s))
            })
            .and_then(Clone::clone)
            .unwrap_or_else(|| Arc::from(""))
    }

    pub fn total(&self, id: i32, item: i32, other_party: bool) -> i32 {
        let Some(container) = self.container(id, other_party) else {
            return 0;
        };
        if item == -1 {
            return 0;
        }
        let mut total = 0i32;
        for (index, amount) in container.amounts.iter().enumerate() {
            if container.item_ids.get(index) == Some(&item) {
                total = total.wrapping_add(*amount);
            }
        }
        total
    }

    pub fn free_slots(&self, id: i32) -> i32 {
        let maximum = self.container_type(id).max_item_size;
        let Some(container) = self.container(id, false) else {
            return maximum;
        };
        let empty = container.item_ids.iter().filter(|item| **item == -1).count() as i32;
        empty
            .wrapping_add(maximum)
            .wrapping_sub(container.item_ids.len() as i32)
    }

    pub fn stock_base(&self, id: i32, item: i32) -> Result<i32, HostError> {
        let kind = self.container_type(id);
        for index in 0..kind.default_item_size.max(0) as usize {
            let stocked = kind.item_ids.get(index).ok_or(HostError::NoEntity)?;
            if *stocked == item {
                return kind.amounts.get(index).copied().ok_or(HostError::NoEntity);
            }
        }
        Ok(-1)
    }

    pub fn param_total(
        &self,
        id: i32,
        param: i32,
        default: i32,
        stackable: bool,
    ) -> Result<i32, HostError> {
        let Some(container) = self.container(id, false) else {
            return Ok(0);
        };
        let mut total = 0i32;
        for (index, item) in container.item_ids.iter().enumerate() {
            if *item >= 0 && (*item as usize) < self.items.len() {
                let value = integer_param(self.items[*item as usize].params.as_ref(), param, default)?;
                total = total.wrapping_add(if stackable {
                    let amount = container.amounts.get(index).ok_or(HostError::NoEntity)?;
                    value.wrapping_mul(*amount)
                } else {
                    value
                });
            }
        }
        Ok(total)
    }

    pub fn find_next(&mut self) -> i32 {
        if let Some(ids) = &self.query.ids {
            if self.query.cursor < self.query.count {
                let value = ids
                    .get(self.query.cursor as usize)
                    .copied()
                    .map(i32::from)
                    .unwrap_or(0);
                self.query.cursor += 1;
                return value;
            }
        }
        -1
    }

    pub fn find_restart(&mut self) {
        self.query.cursor = 0;
    }

    pub fn query(
        &mut self,
        name: &str,
        tradeable_only: bool,
        param: QueryParam<'_>,
    ) -> Result<(), HostError> {
        let needle = name.to_lowercase();
        let mut results: Vec<u16> = Vec::new();
        for (index, item) in self.items.iter().enumerate() {
            if !((!tradeable_only || item.tradeable)
                && item.cert_template_id == -1
                && item.lend_template_id == -1
                && item.bind_template_id == -1
                && item.unknown_int6 == 0
                && item.name.to_lowercase().contains(&needle))
            {
                continue;
            }
            match param {
                QueryParam::None => {}
                QueryParam::Text { id, default, query } => {
                    if query != text_param(item.params.as_ref(), id, default)? {
                        continue;
                    }
                }
                QueryParam::Int { id, default, query } => {
                    if query != integer_param(item.params.as_ref(), id, default)? {
                        continue;
                    }
                }
            }
            if results.len() >= QUERY_LIMIT {
                self.query.count = -1;
                self.query.ids = None;
                return Ok(());
            }
            results.push(index as u16);
        }
        let mut names: Vec<Arc<str>> = results
            .iter()
            .map(|id| {
                self.items
                    .get(usize::from(*id))
                    .map(|item| Arc::clone(&item.name))
                    .unwrap_or_else(|| Arc::from("null"))
            })
            .collect();
        let last = results.len() as isize - 1;
        quicksort(&mut names, &mut results, 0, last);
        self.query = QueryResults {
            count: results.len() as i32,
            ids: Some(results),
            cursor: 0,
        };
        Ok(())
    }
}
