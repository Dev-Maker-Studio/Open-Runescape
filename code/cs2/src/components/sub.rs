use super::ComponentModel;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum InterfaceRequest {
    Unload(i32),
    ResetSequences(i32),
    PruneMiniMenu,
    UpdateLayout(i32),
}

#[derive(Clone, Copy, Eq, PartialEq)]
enum Cursor {
    Head(usize),
    Node(i32),
    Null,
}

const BUCKETS: usize = 8;
const CLIENT_OVERLAY: i32 = 3;

impl ComponentModel {
    pub fn set_open_interface_with_overlay(&mut self, hash: i32, interface: i32, overlay: i32) {
        self.open_interfaces.insert(hash, interface);
        self.open_overlays.insert(hash, overlay);
        self.query.opened(hash);
    }

    pub fn open_overlay(&self, hash: i32) -> Option<i32> {
        self.open_interfaces
            .contains_key(&hash)
            .then(|| self.open_overlays.get(&hash).copied().unwrap_or(0))
    }

    pub fn open_entries(&self) -> Vec<(i32, i32, i32)> {
        self.open_sequence()
            .into_iter()
            .filter_map(|hash| {
                let interface = self.open_interfaces.get(&hash).copied()?;
                Some((hash, interface, self.open_overlays.get(&hash).copied().unwrap_or(0)))
            })
            .collect()
    }

    pub fn slot_settings_entries(&self) -> Vec<(i32, i32, i32, i32)> {
        self.interact
            .slot_settings
            .iter()
            .map(|((hash, slot), settings)| (*hash, *slot, settings.hash, settings.interface_id))
            .collect()
    }

    pub fn drain_interface_requests(&mut self) -> Vec<InterfaceRequest> {
        std::mem::take(&mut self.interface_requests)
    }

    fn bucket_of(&self, bucket: usize) -> Vec<i32> {
        self.query
            .order
            .iter()
            .copied()
            .filter(|hash| (*hash & 7) as usize == bucket)
            .collect()
    }

    fn first_cursor(&self, bucket: usize) -> Cursor {
        self.bucket_of(bucket)
            .first()
            .map_or(Cursor::Head(bucket), |hash| Cursor::Node(*hash))
    }

    fn next_cursor(&self, hash: i32) -> Cursor {
        if !self.open_interfaces.contains_key(&hash) {
            return Cursor::Null;
        }
        let bucket = (hash & 7) as usize;
        let members = self.bucket_of(bucket);
        let at = members.iter().position(|known| *known == hash);
        match at.and_then(|at| members.get(at + 1)) {
            Some(next) => Cursor::Node(*next),
            None => Cursor::Head(bucket),
        }
    }

    fn clear_sub_settings(&mut self, interface: i32) {
        self.interact.slot_settings.retain(|(hash, _), _| {
            ((((*hash as i64) << 32) >> 48) & 0xffff) as i32 != interface
        });
    }

    fn redraw_hash(&mut self, hash: i32) {
        if let Ok(Some(id)) = self.resolve(hash) {
            self.redraw(id);
        }
    }

    fn close_child(&mut self, hash: i32, unload: bool) -> Result<(), ()> {
        let Some(interface) = self.open_interfaces.get(&hash).copied() else {
            return Ok(());
        };
        self.close_open_interface(hash);
        self.open_overlays.remove(&hash);
        if unload {
            self.interface_requests.push(InterfaceRequest::Unload(interface));
        }
        self.clear_sub_settings(interface);
        self.redraw_hash(hash);
        self.interface_requests.push(InterfaceRequest::PruneMiniMenu);
        let mut position = 1usize;
        let mut next = self.first_cursor(0);
        let mut child = self.iter_next(&mut position, &mut next)?;
        while let Some(mut current) = child {
            if !self.open_interfaces.contains_key(&current) {
                position = 1;
                next = self.first_cursor(0);
                match self.iter_next(&mut position, &mut next)? {
                    Some(first) => current = first,
                    None => break,
                }
            }
            if self.open_overlays.get(&current).copied().unwrap_or(0) == CLIENT_OVERLAY
                && (current as u32 >> 16) as i32 == interface
            {
                self.close_child(current, true)?;
            }
            child = self.iter_next(&mut position, &mut next)?;
        }
        Ok(())
    }

    fn iter_next(&self, position: &mut usize, next: &mut Cursor) -> Result<Option<i32>, ()> {
        if *next != Cursor::Head(*position - 1) {
            return match *next {
                Cursor::Node(hash) => {
                    *next = self.next_cursor(hash);
                    Ok(Some(hash))
                }
                _ => Err(()),
            };
        }
        while *position < BUCKETS {
            *position += 1;
            let first = self.first_cursor(*position - 1);
            if first != Cursor::Head(*position - 1) {
                let Cursor::Node(hash) = first else {
                    return Err(());
                };
                *next = self.next_cursor(hash);
                return Ok(Some(hash));
            }
        }
        Ok(None)
    }

    pub(crate) fn open_sub_client(&mut self, parent: i32, interface: i32) -> Result<(), ()> {
        if let Some(current) = self.open_interfaces.get(&parent).copied() {
            self.close_child(parent, current != interface)?;
        }
        self.set_open_interface_with_overlay(parent, interface, CLIENT_OVERLAY);
        self.interface_requests.push(InterfaceRequest::ResetSequences(interface));
        let component = self.resolve(parent).ok().flatten();
        if let Some(id) = component {
            self.redraw(id);
        }
        self.clear_dialogue_continue();
        self.interface_requests.push(InterfaceRequest::PruneMiniMenu);
        if component.is_some() {
            self.interface_requests.push(InterfaceRequest::UpdateLayout(parent));
        }
        Ok(())
    }

    pub(crate) fn close_sub_client(&mut self, parent: i32) -> Result<(), ()> {
        if self.open_overlay(parent) == Some(CLIENT_OVERLAY) {
            self.close_child(parent, true)?;
        }
        Ok(())
    }
}
