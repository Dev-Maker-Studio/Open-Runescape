use std::{collections::BTreeMap, sync::Arc};

mod hooks;
mod interact;
mod media;
mod options;
mod query;
mod drag;
mod menu;
mod sub;
pub use drag::{DragState, DragTarget};
pub use sub::InterfaceRequest;
pub(crate) use query::OpenIdMiss;
pub use interact::{
    ComponentSettings, EntityInterface, InterfaceSelection, OptionInteraction,
};

pub use hooks::{ComponentHooks, HookScript, HookSlot, HookValue, TriggerSlot};
pub use media::{HookRequest, ItemModelView, NpcCustomisation, NpcModelDefinition};
pub(crate) use media::{MediaLookup, MediaOp};
pub use options::{ComponentOptions, KeyTriggers, ParamNode, ParamType};

pub const CANVAS_HEIGHT_FACTOR: i32 = -969_250_379;
pub const CONTAINER_TYPE: u8 = 0;
pub const DEFAULT_MAX_SLOTS: usize = 1 << 20;
const MAX_DEPTH: usize = 256;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ComponentFailure {
    NoComponent,
    Index,
    Interface,
    DivideByZero,
    Rejected,
    SlotLimit,
    Depth,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ComponentError {
    Unavailable,
    Failure(ComponentFailure),
}

impl From<ComponentFailure> for ComponentError {
    fn from(failure: ComponentFailure) -> Self {
        Self::Failure(failure)
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ComponentEventKind {
    Text,
    Color,
    Hide,
    Position,
    Graphic,
    Angle,
    ScrollPosition,
    Font,
    Monospaced,
    ClickMask,
    TextAntiMacro,
    Offset,
    Content,
    Item,
    Anim,
    Video,
    Recolor(i32),
    Retexture(i32),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ComponentEvent {
    pub kind: ComponentEventKind,
    pub hash: i32,
}

#[derive(Clone, Copy, Debug, Eq, Ord, PartialEq, PartialOrd)]
pub struct ComponentId(usize);

impl ComponentId {
    pub fn index(self) -> usize {
        self.0
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ComponentHook {
    pub interface: i32,
    pub component: ComponentId,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ComponentState {
    pub id_hash: i32,
    pub parent_id: i32,
    pub slot_id: i32,
    pub component_type: Option<u8>,
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
    pub base_position_x: i32,
    pub base_position_y: i32,
    pub base_width: i32,
    pub base_height: i32,
    pub aspect_x_type: i8,
    pub aspect_y_type: i8,
    pub aspect_width_type: i8,
    pub aspect_height_type: i8,
    pub aspect_width: i32,
    pub aspect_height: i32,
    pub aspect_x: i32,
    pub aspect_y: i32,
    pub hidden: bool,
    pub scroll_x: i32,
    pub scroll_y: i32,
    pub scroll_width: i32,
    pub scroll_height: i32,
    pub sprite_id: i32,
    pub text: Arc<str>,
    pub color: i32,
    pub transparency: i32,
    pub rendered: bool,
    pub redrawn: bool,
    pub filled: bool,
    pub line_width: i32,
    pub alpha: bool,
    pub shadow: bool,
    pub border_thickness: i32,
    pub sprite_shadow: i32,
    pub flip_vertical: bool,
    pub flip_horizontal: bool,
    pub angle_2d: i32,
    pub tiling: bool,
    pub line_direction: bool,
    pub max_text_lines: i32,
    pub prevent_click_through: bool,
    pub sprite_scale: i32,
    pub font_id: i32,
    pub monospaced: bool,
    pub uses_orthogonal: bool,
    pub origin_x: i32,
    pub origin_y: i32,
    pub text_horizontal_alignment: i32,
    pub text_vertical_alignment: i32,
    pub line_spacing: i32,
    pub target_verb: Option<Arc<str>>,
    pub resume_text: Option<Arc<str>>,
    pub click_mask: bool,
    pub text_anti_macro: bool,
    pub drag_deadzone: i32,
    pub drag_dead_time: i32,
    pub drag_type: i32,
    pub depth_child: Option<ComponentId>,
    pub recolor: Option<([i16; 5], [i16; 5])>,
    pub retexture: Option<([i16; 5], [i16; 5])>,
    pub offset_x: i32,
    pub offset_y: i32,
    pub sprite_pitch: i32,
    pub sprite_roll: i32,
    pub sprite_yaw: i32,
    pub skybox_id: i32,
    pub light_x: i32,
    pub light_y: i32,
    pub light_z: i32,
    pub sun_x: i32,
    pub sun_y: i32,
    pub tint_red: i32,
    pub tint_green: i32,
    pub tint_blue: i32,
    pub tint_scalar: i32,
    pub media_type: i32,
    pub primary_media_id: i32,
    pub secondary_media_id: i32,
    pub wear_col: bool,
    pub container_item_id: i32,
    pub item_stack_size: i32,
    pub render_stack: i32,
    pub animation: i32,
    pub video_file_id: i32,
    pub npc_customisation: Option<NpcCustomisation>,
    pub options: ComponentOptions,
    pub hooks: ComponentHooks,
    slot_children: Option<Vec<Option<ComponentId>>>,
    item_slots: Option<Vec<Option<ComponentId>>>,
}

impl Default for ComponentState {
    fn default() -> Self {
        Self {
            id_hash: -1,
            parent_id: -1,
            slot_id: -1,
            component_type: None,
            x: 0,
            y: 0,
            width: 0,
            height: 0,
            base_position_x: 0,
            base_position_y: 0,
            base_width: 0,
            base_height: 0,
            aspect_x_type: 0,
            aspect_y_type: 0,
            aspect_width_type: 0,
            aspect_height_type: 0,
            aspect_width: 0,
            aspect_height: 0,
            aspect_x: 1,
            aspect_y: 1,
            hidden: false,
            scroll_x: 0,
            scroll_y: 0,
            scroll_width: 0,
            scroll_height: 0,
            sprite_id: -1,
            text: Arc::from(""),
            color: 0,
            transparency: 0,
            rendered: false,
            redrawn: false,
            filled: false,
            line_width: 1,
            alpha: false,
            shadow: false,
            border_thickness: 0,
            sprite_shadow: 0,
            flip_vertical: false,
            flip_horizontal: false,
            angle_2d: 0,
            tiling: false,
            line_direction: false,
            max_text_lines: 0,
            prevent_click_through: false,
            sprite_scale: 100,
            font_id: -1,
            monospaced: true,
            uses_orthogonal: false,
            origin_x: 0,
            origin_y: 0,
            text_horizontal_alignment: 0,
            text_vertical_alignment: 0,
            line_spacing: 0,
            target_verb: Some(Arc::from("")),
            resume_text: None,
            click_mask: true,
            text_anti_macro: false,
            drag_deadzone: 0,
            drag_dead_time: 0,
            drag_type: 0,
            depth_child: None,
            recolor: None,
            retexture: None,
            offset_x: 0,
            offset_y: 0,
            sprite_pitch: 0,
            sprite_roll: 0,
            sprite_yaw: 0,
            skybox_id: -1,
            light_x: 0,
            light_y: 0,
            light_z: 0,
            sun_x: 0,
            sun_y: 0,
            tint_red: 0,
            tint_green: 0,
            tint_blue: 0,
            tint_scalar: 0,
            media_type: 1,
            primary_media_id: 0,
            secondary_media_id: -1,
            wear_col: false,
            container_item_id: -1,
            item_stack_size: 0,
            render_stack: 2,
            animation: -1,
            video_file_id: -1,
            npc_customisation: None,
            options: ComponentOptions::default(),
            hooks: ComponentHooks::default(),
            slot_children: None,
            item_slots: None,
        }
    }
}

#[derive(Clone, Debug)]
struct InterfaceState {
    prevent_interaction: bool,
    components: Vec<Option<ComponentId>>,
    sub_components: Option<Vec<Option<ComponentId>>>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub(crate) enum IntGetter {
    X,
    Y,
    Width,
    Height,
    Hide,
    Colour,
    Graphic,
    Trans,
    ScrollX,
    ScrollY,
    ScrollWidth,
    ScrollHeight,
    ModelZoom,
    ModelAngleX,
    ModelAngleY,
    ModelAngleZ,
    ModelOffsetX,
    ModelOffsetY,
    Model,
    Layer,
    Angle2d,
    SlotId,
    InvObject,
    InvCount,
    FontId,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub(crate) enum Setter {
    Fill,
    LineWidth,
    Alpha,
    TextShadow,
    Outline,
    GraphicShadow,
    VerticalFlip,
    HorizontalFlip,
    Angle2d,
    Tiling,
    LineDirection,
    MaxLines,
    NoClickThrough,
    ScrollPosition,
    ModelZoom,
    TextFont,
    FontMono,
    ModelOrthogonal,
    ModelOrigin,
    TextAlign,
    TargetVerb,
    PauseText,
    ClickMask,
    TextAntiMacro,
    DragDeadzone,
    ModelAngle,
    ModelLighting,
    ModelSunRotation,
    ModelTint,
    Model,
    ModelItemContainer,
    DragDeadTime,
    DragRenderBehaviour,
    Recolor,
    Retexture,
}

#[derive(Clone, Debug)]
pub struct ComponentModel {
    arena: Vec<ComponentState>,
    interfaces: BTreeMap<i32, InterfaceState>,
    capacity: i32,
    hooks: [Option<ComponentHook>; 2],
    canvas_width: i32,
    canvas_height: i32,
    max_slots: usize,
    events: Vec<ComponentEvent>,
    param_types: BTreeMap<i32, ParamType>,
    open_interfaces: BTreeMap<i32, i32>,
    open_overlays: BTreeMap<i32, i32>,
    interface_requests: Vec<InterfaceRequest>,
    drag: DragState,
    query: query::QueryState,
    interact: interact::InteractState,
    next_unique_id: u32,
    hook_request_count: i32,
    pending_hook_requests: Vec<HookRequest>,
    active_dialogue_continue: Option<ComponentId>,
}

fn checked_index(length: usize, index: i64) -> Result<usize, ComponentFailure> {
    usize::try_from(index)
        .ok()
        .filter(|index| *index < length)
        .ok_or(ComponentFailure::Index)
}

fn copy_between(
    source: &[Option<ComponentId>],
    source_start: i64,
    destination: &mut [Option<ComponentId>],
    destination_start: i64,
    length: i64,
) -> Result<(), ComponentFailure> {
    for offset in 0..length {
        let from = checked_index(source.len(), source_start + offset)?;
        let to = checked_index(destination.len(), destination_start + offset)?;
        destination[to] = source[from];
    }
    Ok(())
}

fn shift(
    array: &mut [Option<ComponentId>],
    source_start: i64,
    destination_start: i64,
    length: i64,
) -> Result<(), ComponentFailure> {
    if source_start == destination_start {
        return Ok(());
    }
    let backwards = destination_start > source_start && destination_start < length + source_start;
    for step in 0..length.max(0) {
        let offset = if backwards { length - 1 - step } else { step };
        let from = checked_index(array.len(), source_start + offset)?;
        let to = checked_index(array.len(), destination_start + offset)?;
        array[to] = array[from];
    }
    Ok(())
}

impl ComponentModel {
    pub fn new(capacity: i32, canvas_width: i32, canvas_height: i32) -> Self {
        Self {
            arena: Vec::new(),
            interfaces: BTreeMap::new(),
            capacity: capacity.max(0),
            hooks: [None, None],
            canvas_width,
            canvas_height,
            max_slots: DEFAULT_MAX_SLOTS,
            events: Vec::new(),
            param_types: BTreeMap::new(),
            open_interfaces: BTreeMap::new(),
            open_overlays: BTreeMap::new(),
            interface_requests: Vec::new(),
            drag: DragState::default(),
            query: query::QueryState::default(),
            interact: interact::InteractState::default(),
            next_unique_id: 0,
            hook_request_count: 0,
            pending_hook_requests: Vec::new(),
            active_dialogue_continue: None,
        }
    }

    pub fn set_open_interface(&mut self, hash: i32, interface: i32) {
        self.open_interfaces.insert(hash, interface);
        self.open_overlays.insert(hash, 0);
        self.query.opened(hash);
    }

    pub fn close_open_interface(&mut self, hash: i32) {
        self.open_interfaces.remove(&hash);
        self.open_overlays.remove(&hash);
        self.query.closed(hash);
    }

    pub fn open_interface(&self, hash: i32) -> Option<i32> {
        self.open_interfaces.get(&hash).copied()
    }

    pub fn add_param_type(&mut self, id: i32, param_type: ParamType) {
        self.param_types.insert(id, param_type);
    }

    pub fn param_type(&self, id: i32) -> Option<&ParamType> {
        self.param_types.get(&id)
    }

    pub fn with_max_slots(mut self, max_slots: usize) -> Self {
        self.max_slots = max_slots;
        self
    }

    pub fn add_interface(
        &mut self,
        id: i32,
        prevent_interaction: bool,
        components: Vec<Option<ComponentState>>,
    ) -> Result<Vec<Option<ComponentId>>, ComponentFailure> {
        if id < 0 || id >= self.capacity || self.interfaces.contains_key(&id) {
            return Err(ComponentFailure::Index);
        }
        let handles: Vec<Option<ComponentId>> = components
            .into_iter()
            .map(|state| {
                state.map(|state| {
                    self.arena.push(state);
                    ComponentId(self.arena.len() - 1)
                })
            })
            .collect();
        self.interfaces.insert(
            id,
            InterfaceState {
                prevent_interaction,
                components: handles.clone(),
                sub_components: None,
            },
        );
        Ok(handles)
    }

    pub fn component(&self, id: ComponentId) -> &ComponentState {
        &self.arena[id.0]
    }

    pub fn component_mut(&mut self, id: ComponentId) -> &mut ComponentState {
        &mut self.arena[id.0]
    }

    pub fn interface_component(&self, interface: i32, index: usize) -> Option<ComponentId> {
        self.interfaces
            .get(&interface)
            .and_then(|state| state.components.get(index).copied().flatten())
    }

    pub fn slot_children(&self, id: ComponentId) -> Option<&[Option<ComponentId>]> {
        self.arena[id.0].slot_children.as_deref()
    }

    pub fn slot_child(&self, id: ComponentId, slot: usize) -> Option<ComponentId> {
        self.slot_children(id)
            .and_then(|children| children.get(slot).copied().flatten())
    }

    pub fn hook(&self, secondary: bool) -> Option<ComponentHook> {
        self.hooks[usize::from(secondary)]
    }

    pub fn set_hook(&mut self, secondary: bool, hook: Option<ComponentHook>) {
        self.hooks[usize::from(secondary)] = hook;
    }

    pub fn events(&self) -> &[ComponentEvent] {
        &self.events
    }

    pub fn take_events(&mut self) -> Vec<ComponentEvent> {
        std::mem::take(&mut self.events)
    }

    pub(crate) fn in_range(&self, hash: i32) -> bool {
        let interface = hash >> 16;
        interface >= 0 && interface < self.capacity
    }

    pub(crate) fn resolve(&self, hash: i32) -> Result<Option<ComponentId>, ComponentError> {
        let interface = hash >> 16;
        if interface < 0 || interface >= self.capacity {
            return Ok(None);
        }
        let Some(state) = self.interfaces.get(&interface) else {
            return Err(ComponentError::Unavailable);
        };
        Ok(self.lookup(state, hash)?)
    }

    fn lookup(
        &self,
        state: &InterfaceState,
        hash: i32,
    ) -> Result<Option<ComponentId>, ComponentFailure> {
        let first = state
            .components
            .first()
            .ok_or(ComponentFailure::Index)?
            .ok_or(ComponentFailure::NoComponent)?;
        if (hash as u32) >> 16 != (self.arena[first.0].id_hash as u32) >> 16 {
            return Err(ComponentFailure::Interface);
        }
        state
            .components
            .get((hash & 0xffff) as usize)
            .copied()
            .ok_or(ComponentFailure::Index)
    }

    fn interface(&self, interface: i32) -> Result<&InterfaceState, ComponentFailure> {
        self.interfaces
            .get(&interface)
            .ok_or(ComponentFailure::NoComponent)
    }

    fn redraw(&mut self, id: ComponentId) {
        let state = &mut self.arena[id.0];
        if state.rendered {
            state.redrawn = true;
        }
    }

    fn push_event(&mut self, kind: ComponentEventKind, hash: i32) {
        let event = ComponentEvent { kind, hash };
        self.events.retain(|existing| *existing != event);
        self.events.push(event);
    }

    fn emits(&self, interface: i32, id: ComponentId) -> bool {
        self.arena[id.0].slot_id == -1
            && self
                .interfaces
                .get(&interface)
                .is_some_and(|state| !state.prevent_interaction)
    }

    fn notify(&mut self, interface: i32, id: ComponentId, kind: ComponentEventKind) {
        if self.emits(interface, id) {
            let hash = self.arena[id.0].id_hash;
            self.push_event(kind, hash);
        }
    }

    pub(crate) fn find(
        &mut self,
        secondary: bool,
        hash: i32,
        slot: i32,
    ) -> Result<bool, ComponentError> {
        let found = self.resolve(hash)?;
        let target = match (found, slot) {
            (_, -1) => found,
            (Some(parent), slot) => match self.arena[parent.0].slot_children.as_ref() {
                Some(children) if slot < children.len() as i32 => {
                    let index = usize::try_from(slot).map_err(|_| ComponentFailure::Index)?;
                    children[index]
                }
                _ => None,
            },
            (None, _) => None,
        };
        let hook = target.map(|component| ComponentHook {
            interface: hash >> 16,
            component,
        });
        self.hooks[usize::from(secondary)] = hook;
        Ok(hook.is_some())
    }

    pub(crate) fn create(
        &mut self,
        secondary: bool,
        hash: i32,
        type_id: i32,
        slot: i32,
    ) -> Result<(), ComponentError> {
        self.resolve(hash)?;
        let interface_id = ((hash as u32) >> 16) as i64;
        if interface_id >= i64::from(self.capacity) {
            return Err(ComponentFailure::Index.into());
        }
        let Some(interface) = self.interfaces.get(&(interface_id as i32)) else {
            return Err(ComponentFailure::NoComponent.into());
        };
        if type_id == 0 {
            return Err(ComponentFailure::Rejected.into());
        }
        let parent = interface
            .components
            .get((hash & 0xffff) as usize)
            .ok_or(ComponentFailure::Index)?
            .ok_or(ComponentFailure::NoComponent)?;
        let size = slot.wrapping_add(1);
        if self.arena[parent.0].slot_children.is_none() {
            if size < 0 {
                return Err(ComponentFailure::Index.into());
            }
            if size as usize > self.max_slots {
                return Err(ComponentFailure::SlotLimit.into());
            }
            self.arena[parent.0].slot_children = Some(vec![None; size as usize]);
            self.arena[parent.0].item_slots = None;
        }
        let length = self.arena[parent.0]
            .slot_children
            .as_ref()
            .map_or(0, Vec::len);
        if (length as i64) <= i64::from(slot) {
            if size < 0 {
                return Err(ComponentFailure::Index.into());
            }
            if size as usize > self.max_slots {
                return Err(ComponentFailure::SlotLimit.into());
            }
            if let Some(children) = self.arena[parent.0].slot_children.as_mut() {
                children.resize(size as usize, None);
            }
            if let Some(items) = self.arena[parent.0].item_slots.as_mut() {
                items.resize(size as usize, None);
            }
        }
        if slot > 0 {
            let previous = self.slot_child(parent, (slot - 1) as usize).is_some();
            if !previous {
                return Err(ComponentFailure::Rejected.into());
            }
        }
        let index = usize::try_from(slot).map_err(|_| ComponentFailure::Index)?;
        let parent_hash = self.arena[parent.0].id_hash;
        let created = ComponentState {
            component_type: u8::try_from(type_id).ok().filter(|id| *id <= 9),
            id_hash: parent_hash,
            parent_id: parent_hash,
            slot_id: slot,
            ..ComponentState::default()
        };
        self.arena.push(created);
        let id = ComponentId(self.arena.len() - 1);
        if let Some(children) = self.arena[parent.0].slot_children.as_mut() {
            children[index] = Some(id);
        }
        if let Some(items) = self.arena[parent.0].item_slots.as_mut() {
            items[index] = Some(id);
        }
        self.hooks[usize::from(secondary)] = Some(ComponentHook {
            interface: interface_id as i32,
            component: id,
        });
        self.redraw(parent);
        Ok(())
    }

    pub(crate) fn delete(&mut self, secondary: bool) -> Result<(), ComponentFailure> {
        let hook = self.hooks[usize::from(secondary)].ok_or(ComponentFailure::NoComponent)?;
        let slot = self.arena[hook.component.0].slot_id;
        if slot == -1 {
            return Err(ComponentFailure::Rejected);
        }
        let hash = self.arena[hook.component.0].id_hash;
        let state = self.interface(hook.interface)?;
        let parent = self
            .lookup(state, hash)?
            .ok_or(ComponentFailure::NoComponent)?;
        let children = self.arena[parent.0]
            .slot_children
            .as_mut()
            .ok_or(ComponentFailure::NoComponent)?;
        let index = usize::try_from(slot).map_err(|_| ComponentFailure::Index)?;
        *children.get_mut(index).ok_or(ComponentFailure::Index)? = None;
        self.redraw(parent);
        Ok(())
    }

    pub(crate) fn delete_all(&mut self, id: ComponentId) {
        self.arena[id.0].slot_children = None;
        self.arena[id.0].item_slots = None;
        self.redraw(id);
    }

    pub fn item_slots(&self, id: ComponentId) -> Option<&[Option<ComponentId>]> {
        let state = &self.arena[id.0];
        state
            .item_slots
            .as_deref()
            .or(state.slot_children.as_deref())
    }

    pub fn sub_components(&self, interface: i32) -> Option<&[Option<ComponentId>]> {
        self.interfaces
            .get(&interface)
            .and_then(|state| state.sub_components.as_deref())
    }

    pub(crate) fn send_to_order(
        &mut self,
        interface: i32,
        target: Option<ComponentId>,
        front: bool,
    ) -> Result<(), ComponentFailure> {
        let Some(target) = target else {
            return Ok(());
        };
        let slot_id = self.arena[target.0].slot_id;
        let state = self
            .interfaces
            .get(&interface)
            .ok_or(ComponentFailure::NoComponent)?;
        if slot_id == -1 {
            let state = self
                .interfaces
                .get_mut(&interface)
                .ok_or(ComponentFailure::NoComponent)?;
            let order = state
                .sub_components
                .get_or_insert_with(|| state.components.clone());
            if let Some(index) = order.iter().position(|entry| *entry == Some(target)) {
                if front {
                    let tail = order.len() as i64 - index as i64 - 1;
                    shift(order, index as i64 + 1, index as i64, tail)?;
                    let last = order.len() - 1;
                    order[last] = Some(target);
                } else {
                    shift(order, 0, 1, index as i64)?;
                    order[0] = Some(target);
                }
            }
            return Ok(());
        }
        let parent_hash = self.arena[target.0].parent_id;
        let parent = if front {
            self.lookup(state, parent_hash)?
        } else {
            state
                .components
                .get((parent_hash & 0xffff) as usize)
                .copied()
                .ok_or(ComponentFailure::Index)?
        };
        let Some(parent) = parent else {
            return Ok(());
        };
        let slot = i64::from(slot_id);
        let node = &mut self.arena[parent.0];
        if node.item_slots.is_none() {
            let children = node
                .slot_children
                .clone()
                .ok_or(ComponentFailure::NoComponent)?;
            let size = children.len();
            node.item_slots = Some(vec![None; size]);
            let items = node
                .item_slots
                .as_mut()
                .ok_or(ComponentFailure::NoComponent)?;
            let end = size.checked_sub(1).ok_or(ComponentFailure::Index)?;
            if front {
                items[end] = Some(target);
                copy_between(&children, 0, items, 0, slot)?;
                copy_between(&children, slot + 1, items, slot, size as i64 - slot - 1)?;
            } else {
                items[0] = Some(target);
                copy_between(&children, 0, items, 1, slot)?;
                copy_between(&children, slot + 1, items, slot + 1, size as i64 - slot - 1)?;
            }
            return Ok(());
        }
        let items = node
            .item_slots
            .as_mut()
            .ok_or(ComponentFailure::NoComponent)?;
        if let Some(index) = items.iter().position(|entry| *entry == Some(target)) {
            if front {
                let tail = items.len() as i64 - index as i64 - 1;
                shift(items, index as i64 + 1, index as i64, tail)?;
                let last = items.len() - 1;
                items[last] = Some(target);
            } else {
                shift(items, 0, 1, index as i64)?;
                items[0] = Some(target);
            }
        }
        Ok(())
    }

    pub(crate) fn get_int(&self, id: ComponentId, getter: IntGetter) -> i32 {
        let state = &self.arena[id.0];
        match getter {
            IntGetter::X => state.x,
            IntGetter::Y => state.y,
            IntGetter::Width => state.width,
            IntGetter::Height => state.height,
            IntGetter::Hide => i32::from(state.hidden),
            IntGetter::Colour => state.color,
            IntGetter::Graphic => state.sprite_id,
            IntGetter::Trans => state.transparency,
            IntGetter::ScrollX => state.scroll_x,
            IntGetter::ScrollY => state.scroll_y,
            IntGetter::ScrollWidth => state.scroll_width,
            IntGetter::ScrollHeight => state.scroll_height,
            IntGetter::ModelZoom => state.sprite_scale,
            IntGetter::ModelAngleX => state.sprite_pitch,
            IntGetter::ModelAngleY => state.sprite_roll,
            IntGetter::ModelAngleZ => state.sprite_yaw,
            IntGetter::ModelOffsetX => state.offset_x,
            IntGetter::ModelOffsetY => state.offset_y,
            IntGetter::Layer => state.parent_id,
            IntGetter::Angle2d => state.angle_2d,
            IntGetter::SlotId => state.slot_id,
            IntGetter::InvObject => state.container_item_id,
            IntGetter::InvCount => {
                if state.container_item_id != -1 {
                    state.item_stack_size
                } else {
                    0
                }
            }
            IntGetter::FontId => state.font_id,
            IntGetter::Model => {
                if state.media_type == 1 {
                    state.primary_media_id
                } else {
                    -1
                }
            }
        }
    }

    pub(crate) fn get_text(&self, id: ComponentId) -> Arc<str> {
        Arc::clone(&self.arena[id.0].text)
    }

    pub(crate) fn set_colour(&mut self, interface: i32, id: ComponentId, color: i32) {
        self.arena[id.0].color = color;
        self.redraw(id);
        self.notify(interface, id, ComponentEventKind::Color);
    }

    pub(crate) fn apply(
        &mut self,
        interface: i32,
        id: ComponentId,
        setter: Setter,
        values: &[i32],
        text: Option<Arc<str>>,
    ) {
        let flag = values.first().is_some_and(|value| *value == 1);
        let number = values.first().copied().unwrap_or(0);
        let state = &mut self.arena[id.0];
        let mut redraw = true;
        let mut event = None;
        let mut extra = [None, None];
        match setter {
            Setter::Fill => state.filled = flag,
            Setter::LineWidth => state.line_width = number,
            Setter::Alpha => state.alpha = flag,
            Setter::TextShadow => state.shadow = flag,
            Setter::Outline => state.border_thickness = number,
            Setter::GraphicShadow => state.sprite_shadow = number,
            Setter::VerticalFlip => state.flip_vertical = flag,
            Setter::HorizontalFlip => state.flip_horizontal = flag,
            Setter::Angle2d => state.angle_2d = number,
            Setter::Tiling => state.tiling = flag,
            Setter::LineDirection => state.line_direction = flag,
            Setter::MaxLines => state.max_text_lines = number,
            Setter::NoClickThrough => {
                state.prevent_click_through = flag;
                redraw = false;
            }
            Setter::ScrollPosition => {
                state.scroll_x = number
                    .min(state.scroll_width.wrapping_sub(state.width))
                    .max(0);
                state.scroll_y = values[1]
                    .min(state.scroll_height.wrapping_sub(state.height))
                    .max(0);
                event = Some(ComponentEventKind::ScrollPosition);
            }
            Setter::ModelZoom => {
                state.sprite_scale = number;
                event = Some(ComponentEventKind::Angle);
            }
            Setter::TextFont => {
                state.font_id = number;
                event = Some(ComponentEventKind::Font);
            }
            Setter::FontMono => {
                state.monospaced = flag;
                event = Some(ComponentEventKind::Monospaced);
            }
            Setter::ModelOrthogonal => state.uses_orthogonal = flag,
            Setter::ModelOrigin => {
                state.origin_x = number;
                state.origin_y = values[1];
            }
            Setter::TextAlign => {
                state.text_horizontal_alignment = number;
                state.text_vertical_alignment = values[1];
                state.line_spacing = values[2];
            }
            Setter::TargetVerb => {
                state.target_verb = text;
                redraw = false;
            }
            Setter::PauseText => {
                state.resume_text = text;
                redraw = false;
            }
            Setter::ClickMask => {
                state.click_mask = flag;
                event = Some(ComponentEventKind::ClickMask);
            }
            Setter::TextAntiMacro => {
                state.text_anti_macro = flag;
                event = Some(ComponentEventKind::TextAntiMacro);
            }
            Setter::DragDeadzone => {
                state.drag_deadzone = number;
                redraw = false;
            }
            Setter::DragDeadTime => {
                state.drag_dead_time = number;
                redraw = false;
            }
            Setter::DragRenderBehaviour => {
                if (0..=2).contains(&number) {
                    state.drag_type = number;
                }
                redraw = false;
            }
            Setter::Recolor | Setter::Retexture => {
                if (0..5).contains(&number) {
                    let slot = number as usize;
                    let pair = if setter == Setter::Recolor {
                        &mut state.recolor
                    } else {
                        &mut state.retexture
                    };
                    let (source, destination) = pair.get_or_insert(([0; 5], [0; 5]));
                    source[slot] = values[1] as i16;
                    destination[slot] = values[2] as i16;
                    event = Some(if setter == Setter::Recolor {
                        ComponentEventKind::Recolor(number)
                    } else {
                        ComponentEventKind::Retexture(number)
                    });
                } else {
                    redraw = false;
                }
            }
            Setter::ModelAngle => {
                state.offset_x = number;
                state.offset_y = values[1];
                state.sprite_pitch = values[2];
                state.sprite_roll = values[3];
                state.sprite_yaw = values[4];
                state.sprite_scale = values[5];
                extra = [
                    Some(ComponentEventKind::Angle),
                    Some(ComponentEventKind::Offset),
                ];
            }
            Setter::ModelLighting => {
                state.skybox_id = number;
                state.light_x = values[1];
                state.light_y = values[2];
                state.light_z = values[3];
            }
            Setter::ModelSunRotation => {
                state.sun_x = number;
                state.sun_y = values[1];
            }
            Setter::ModelTint => {
                state.tint_red = number;
                state.tint_green = values[1];
                state.tint_blue = values[2];
                state.tint_scalar = values[3];
            }
            Setter::Model => {
                state.media_type = 1;
                state.primary_media_id = number;
                extra = [Some(ComponentEventKind::Content), None];
            }
            Setter::ModelItemContainer => {
                state.primary_media_id = number;
                state.secondary_media_id = values[1];
                state.media_type = if values[2] == 1 { 9 } else { 8 };
                state.wear_col = values[3] == 1;
                redraw = false;
                extra = [Some(ComponentEventKind::Content), None];
            }
        }
        if redraw {
            self.redraw(id);
        }
        if let Some(kind) = event {
            self.notify(interface, id, kind);
        }
        for kind in extra.into_iter().flatten() {
            self.notify(interface, id, kind);
        }
    }

    pub(crate) fn set_scroll_size(
        &mut self,
        interface: i32,
        id: ComponentId,
        width: i32,
        height: i32,
    ) -> Result<(), ComponentFailure> {
        self.arena[id.0].scroll_width = width;
        self.arena[id.0].scroll_height = height;
        self.redraw(id);
        if self.arena[id.0].component_type == Some(CONTAINER_TYPE) {
            let statics = self.interface(interface)?.components.clone();
            self.update_entity(&statics, id, 0)?;
        }
        Ok(())
    }

    pub(crate) fn set_aspect(
        &mut self,
        interface: i32,
        id: ComponentId,
        x: i32,
        y: i32,
    ) -> Result<(), ComponentFailure> {
        self.arena[id.0].aspect_x = x;
        self.arena[id.0].aspect_y = y;
        self.redraw(id);
        self.relayout(interface, id)?;
        if self.arena[id.0].component_type == Some(CONTAINER_TYPE) {
            let statics = self.interface(interface)?.components.clone();
            self.update_entity(&statics, id, 0)?;
        }
        Ok(())
    }

    pub(crate) fn set_depth_child(
        &mut self,
        id: ComponentId,
        hash: i32,
        slot: i32,
    ) -> Result<(), ComponentError> {
        if hash == -1 && slot == -1 {
            self.arena[id.0].depth_child = None;
            return Ok(());
        }
        let found = self.resolve(hash)?;
        let target = match (found, slot) {
            (_, -1) => found,
            (Some(parent), slot) => match self.arena[parent.0].slot_children.as_ref() {
                Some(children) if slot < children.len() as i32 => {
                    let index = usize::try_from(slot).map_err(|_| ComponentFailure::Index)?;
                    children[index]
                }
                _ => None,
            },
            (None, _) => None,
        };
        self.arena[id.0].depth_child = target;
        Ok(())
    }

    pub(crate) fn next_sub_id(&self, id: ComponentId) -> i32 {
        match self.arena[id.0].slot_children.as_ref() {
            None => 0,
            Some(children) => children
                .iter()
                .position(Option::is_none)
                .unwrap_or(children.len()) as i32,
        }
    }

    pub(crate) fn set_trans(&mut self, id: ComponentId, transparency: i32) {
        self.arena[id.0].transparency = transparency;
        self.redraw(id);
    }

    pub(crate) fn set_graphic(&mut self, interface: i32, id: ComponentId, sprite: i32) {
        if sprite != self.arena[id.0].sprite_id {
            self.arena[id.0].sprite_id = sprite;
            self.redraw(id);
        }
        self.notify(interface, id, ComponentEventKind::Graphic);
    }

    pub(crate) fn set_hide(&mut self, interface: i32, id: ComponentId, value: i32) {
        let hidden = value == 1;
        if hidden != self.arena[id.0].hidden {
            self.arena[id.0].hidden = hidden;
            self.redraw(id);
        }
        self.notify(interface, id, ComponentEventKind::Hide);
    }

    pub(crate) fn set_text(&mut self, interface: i32, id: ComponentId, text: Option<Arc<str>>) {
        let changed = match &text {
            Some(text) => **text != *self.arena[id.0].text,
            None => true,
        };
        if changed {
            self.arena[id.0].text = text.unwrap_or_else(|| Arc::from(""));
            self.redraw(id);
        }
        self.notify(interface, id, ComponentEventKind::Text);
    }

    pub(crate) fn set_position(
        &mut self,
        interface: i32,
        id: ComponentId,
        args: [i32; 4],
    ) -> Result<(), ComponentFailure> {
        let state = &mut self.arena[id.0];
        state.base_position_x = args[0];
        state.base_position_y = args[1];
        state.aspect_x_type = args[2].clamp(0, 5) as i8;
        state.aspect_y_type = args[3].clamp(0, 5) as i8;
        self.redraw(id);
        self.relayout(interface, id)?;
        if self.arena[id.0].component_type == Some(CONTAINER_TYPE) {
            let statics = self.interface(interface)?.components.clone();
            self.update_entity(&statics, id, 0)?;
        }
        self.notify(interface, id, ComponentEventKind::Position);
        Ok(())
    }

    pub(crate) fn set_size(
        &mut self,
        interface: i32,
        id: ComponentId,
        args: [i32; 4],
    ) -> Result<(), ComponentFailure> {
        let state = &mut self.arena[id.0];
        state.base_width = args[0];
        state.base_height = args[1];
        state.aspect_width = 0;
        state.aspect_height = 0;
        state.aspect_width_type = args[2].clamp(0, 4) as i8;
        state.aspect_height_type = args[3].clamp(0, 4) as i8;
        self.redraw(id);
        self.relayout(interface, id)?;
        if self.arena[id.0].component_type == Some(CONTAINER_TYPE) {
            let statics = self.interface(interface)?.components.clone();
            self.update_entity(&statics, id, 0)?;
        }
        Ok(())
    }

    fn relayout(&mut self, interface: i32, id: ComponentId) -> Result<(), ComponentFailure> {
        let state = self.interface(interface)?;
        let parent_id = self.arena[id.0].parent_id;
        let parent = if parent_id != -1 {
            self.lookup(state, parent_id)?
        } else {
            None
        };
        let (width, height) = match parent {
            Some(parent) => (self.arena[parent.0].width, self.arena[parent.0].height),
            None => (
                self.canvas_width,
                self.canvas_height.wrapping_mul(CANVAS_HEIGHT_FACTOR),
            ),
        };
        self.init_sizes(id, width, height)?;
        self.adjust_position(id, width, height);
        Ok(())
    }

    fn init_sizes(
        &mut self,
        id: ComponentId,
        parent_width: i32,
        parent_height: i32,
    ) -> Result<(), ComponentFailure> {
        let state = &mut self.arena[id.0];
        match state.aspect_width_type {
            0 => state.width = state.base_width,
            1 => state.width = parent_width.wrapping_sub(state.base_width),
            2 => state.width = state.base_width.wrapping_mul(parent_width) >> 14,
            _ => {}
        }
        match state.aspect_height_type {
            0 => state.height = state.base_height,
            1 => state.height = parent_height.wrapping_sub(state.base_height),
            2 => state.height = parent_height.wrapping_mul(state.base_height) >> 14,
            _ => {}
        }
        if state.aspect_width_type == 4 {
            if state.aspect_y == 0 {
                return Err(ComponentFailure::DivideByZero);
            }
            state.width = state
                .aspect_x
                .wrapping_mul(state.height)
                .wrapping_div(state.aspect_y);
        }
        if state.aspect_height_type == 4 {
            if state.aspect_x == 0 {
                return Err(ComponentFailure::DivideByZero);
            }
            state.height = state
                .width
                .wrapping_mul(state.aspect_y)
                .wrapping_div(state.aspect_x);
        }
        Ok(())
    }

    fn adjust_position(&mut self, id: ComponentId, parent_width: i32, parent_height: i32) {
        let state = &mut self.arena[id.0];
        let free_width = parent_width.wrapping_sub(state.width);
        let free_height = parent_height.wrapping_sub(state.height);
        let scaled_x = state.base_position_x.wrapping_mul(parent_width) >> 14;
        let scaled_y = parent_height.wrapping_mul(state.base_position_y) >> 14;
        state.x = match state.aspect_x_type {
            0 => state.base_position_x,
            1 => state
                .base_position_x
                .wrapping_add(free_width.wrapping_div(2)),
            2 => free_width.wrapping_sub(state.base_position_x),
            3 => scaled_x,
            4 => free_width.wrapping_div(2).wrapping_add(scaled_x),
            _ => free_width.wrapping_sub(scaled_x),
        };
        state.y = match state.aspect_y_type {
            0 => state.base_position_y,
            1 => free_height
                .wrapping_div(2)
                .wrapping_add(state.base_position_y),
            2 => free_height.wrapping_sub(state.base_position_y),
            3 => scaled_y,
            4 => free_height.wrapping_div(2).wrapping_add(scaled_y),
            _ => free_height.wrapping_sub(scaled_y),
        };
    }

    fn update_entity(
        &mut self,
        components: &[Option<ComponentId>],
        id: ComponentId,
        depth: usize,
    ) -> Result<(), ComponentFailure> {
        if depth >= MAX_DEPTH {
            return Err(ComponentFailure::Depth);
        }
        let state = &self.arena[id.0];
        let width = if state.scroll_width != 0 {
            state.scroll_width
        } else {
            state.width
        };
        let height = if state.scroll_height != 0 {
            state.scroll_height
        } else {
            state.height
        };
        let hash = state.id_hash;
        self.update_dimensions(components, hash, width, height, depth)?;
        let slots = self.arena[id.0].slot_children.clone();
        if let Some(slots) = slots {
            self.update_dimensions(&slots, hash, width, height, depth)?;
        }
        Ok(())
    }

    fn update_dimensions(
        &mut self,
        components: &[Option<ComponentId>],
        hash: i32,
        width: i32,
        height: i32,
        depth: usize,
    ) -> Result<(), ComponentFailure> {
        for child in components.iter().flatten().copied() {
            if self.arena[child.0].parent_id != hash {
                continue;
            }
            self.init_sizes(child, width, height)?;
            self.adjust_position(child, width, height);
            let state = &mut self.arena[child.0];
            if state.scroll_x > state.scroll_width.wrapping_sub(state.width) {
                state.scroll_x = state.scroll_width.wrapping_sub(state.width);
            }
            if state.scroll_x < 0 {
                state.scroll_x = 0;
            }
            if state.scroll_y > state.scroll_height.wrapping_sub(state.height) {
                state.scroll_y = state.scroll_height.wrapping_sub(state.height);
            }
            if state.scroll_y < 0 {
                state.scroll_y = 0;
            }
            if state.component_type == Some(CONTAINER_TYPE) {
                self.update_entity(components, child, depth + 1)?;
            }
        }
        Ok(())
    }
}
