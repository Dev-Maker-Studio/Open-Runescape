use super::{ComponentError, ComponentEventKind, ComponentFailure, ComponentId, ComponentModel};
use super::HookScript;
use super::HookSlot;
use std::sync::Arc;

const NPC_HEAD: i32 = 2;
const PLAYER_HEAD: i32 = 3;
const PLAYER_MODEL: i32 = 5;
const NPC_MODEL: i32 = 6;
const RAW_MODEL: i32 = 1;
const TEXT_TYPE: u8 = 4;
const SPRITE_TYPE: u8 = 5;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ItemModelView {
    pub rotation_x: i32,
    pub rotation_y: i32,
    pub rotation_z: i32,
    pub offset_x: i32,
    pub offset_y: i32,
    pub zoom: i32,
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct NpcModelDefinition {
    pub model_ids: Option<Vec<i32>>,
    pub head_models: Option<Vec<i32>>,
    pub recolor_dst: Option<Vec<i16>>,
    pub retexture_dst: Option<Vec<i16>>,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct NpcCustomisation {
    pub uid: i64,
    pub model_ids: Vec<i32>,
    pub recolor_dst: Option<Vec<i16>>,
    pub retexture_dst: Option<Vec<i16>>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub(crate) enum MediaOp {
    Item { wear_col: bool, mode: i32 },
    NpcHead,
    NpcModel,
    PlayerModel,
    PlayerHeadSelf,
    PlayerModelSelf,
    ModelAnim,
    VideoGraphic,
    VideoText,
    NpcCustomBody,
    NpcCustomHead,
    NpcCustomRecol,
    NpcCustomRetex,
    CallOnResize,
}

impl MediaOp {
    pub(crate) fn int_arguments(self) -> usize {
        match self {
            Self::Item { .. }
            | Self::NpcCustomBody
            | Self::NpcCustomHead
            | Self::NpcCustomRecol
            | Self::NpcCustomRetex => 2,
            Self::NpcHead | Self::NpcModel | Self::PlayerModel | Self::ModelAnim => 1,
            Self::PlayerHeadSelf
            | Self::PlayerModelSelf
            | Self::CallOnResize
            | Self::VideoGraphic
            | Self::VideoText => 0,
        }
    }

    pub(crate) fn pops_late(self) -> bool {
        matches!(self, Self::VideoGraphic | Self::VideoText)
    }
}

#[derive(Clone, Debug, Default)]
pub(crate) struct MediaLookup {
    pub item: Option<ItemModelView>,
    pub npc: Option<Arc<NpcModelDefinition>>,
    pub player_index: i32,
}

impl ComponentModel {
    pub(crate) fn media_primary(&self, id: ComponentId) -> i32 {
        self.arena[id.0].primary_media_id
    }

    pub(crate) fn media_animation(&self, id: ComponentId) -> i32 {
        self.arena[id.0].animation
    }

    pub(crate) fn video_matches(&self, id: ComponentId, op: MediaOp) -> bool {
        let wanted = match op {
            MediaOp::VideoGraphic => SPRITE_TYPE,
            MediaOp::VideoText => TEXT_TYPE,
            _ => return true,
        };
        self.arena[id.0].component_type == Some(wanted)
    }

    pub(crate) fn apply_media(
        &mut self,
        interface: i32,
        id: ComponentId,
        op: MediaOp,
        values: &[i32],
        lookup: &MediaLookup,
    ) -> Result<(), ComponentFailure> {
        let first = values.first().copied().unwrap_or(0);
        match op {
            MediaOp::Item { wear_col, mode } => {
                self.notify(interface, id, ComponentEventKind::Item);
                self.notify(interface, id, ComponentEventKind::Angle);
                self.notify(interface, id, ComponentEventKind::Offset);
                let stack_size = values[1];
                let state = &mut self.arena[id.0];
                if first == -1 {
                    state.media_type = RAW_MODEL;
                    state.primary_media_id = -1;
                    state.container_item_id = -1;
                    return Ok(());
                }
                state.container_item_id = first;
                state.item_stack_size = stack_size;
                state.wear_col = wear_col;
                let view = lookup.item.ok_or(ComponentFailure::NoComponent)?;
                state.sprite_pitch = view.rotation_x;
                state.sprite_roll = view.rotation_y;
                state.sprite_yaw = view.rotation_z;
                state.offset_x = view.offset_x;
                state.offset_y = view.offset_y;
                state.sprite_scale = view.zoom;
                state.render_stack = mode;
                if state.aspect_width > 0 {
                    state.sprite_scale = state.sprite_scale.wrapping_mul(32) / state.aspect_width;
                } else if state.base_width > 0 {
                    state.sprite_scale = state.sprite_scale.wrapping_mul(32) / state.base_width;
                }
                Ok(())
            }
            MediaOp::NpcHead | MediaOp::NpcModel => {
                let state = &mut self.arena[id.0];
                state.media_type = if op == MediaOp::NpcHead {
                    NPC_HEAD
                } else {
                    NPC_MODEL
                };
                state.npc_customisation = None;
                state.primary_media_id = first;
                self.notify(interface, id, ComponentEventKind::Content);
                Ok(())
            }
            MediaOp::PlayerModel => {
                let state = &mut self.arena[id.0];
                state.media_type = PLAYER_MODEL;
                state.primary_media_id = first;
                self.notify(interface, id, ComponentEventKind::Content);
                Ok(())
            }
            MediaOp::PlayerHeadSelf | MediaOp::PlayerModelSelf => {
                let state = &mut self.arena[id.0];
                state.media_type = if op == MediaOp::PlayerHeadSelf {
                    PLAYER_HEAD
                } else {
                    PLAYER_MODEL
                };
                state.primary_media_id = lookup.player_index;
                state.secondary_media_id = 0;
                self.notify(interface, id, ComponentEventKind::Content);
                Ok(())
            }
            MediaOp::ModelAnim => {
                if first != self.arena[id.0].animation {
                    self.arena[id.0].animation = first;
                    self.redraw(id);
                }
                self.notify(interface, id, ComponentEventKind::Anim);
                Ok(())
            }
            MediaOp::CallOnResize => self.call_on_resize(id),
            MediaOp::NpcCustomBody
            | MediaOp::NpcCustomHead
            | MediaOp::NpcCustomRecol
            | MediaOp::NpcCustomRetex => self.apply_npc_custom(id, op, values, lookup),
            MediaOp::VideoGraphic | MediaOp::VideoText => {
                if first != self.arena[id.0].video_file_id {
                    self.arena[id.0].video_file_id = first;
                    self.redraw(id);
                }
                self.notify(interface, id, ComponentEventKind::Video);
                Ok(())
            }
        }
    }

    fn apply_npc_custom(
        &mut self,
        id: ComponentId,
        op: MediaOp,
        values: &[i32],
        lookup: &MediaLookup,
    ) -> Result<(), ComponentFailure> {
        let slot = values[0].wrapping_sub(1);
        let value = values[1];
        let media_type = self.arena[id.0].media_type;
        let allowed = match op {
            MediaOp::NpcCustomBody => media_type == NPC_MODEL,
            MediaOp::NpcCustomHead => media_type == NPC_HEAD,
            _ => media_type == NPC_MODEL || media_type == NPC_HEAD,
        };
        if !allowed {
            return Err(ComponentFailure::Rejected);
        }
        let definition = lookup.npc.as_deref().ok_or(ComponentFailure::NoComponent)?;
        let body = match op {
            MediaOp::NpcCustomBody => true,
            MediaOp::NpcCustomHead => false,
            _ => media_type == NPC_MODEL,
        };
        if self.arena[id.0].npc_customisation.is_none() {
            let source = if body {
                definition.model_ids.as_ref()
            } else {
                definition.head_models.as_ref()
            }
            .ok_or(ComponentFailure::NoComponent)?;
            self.arena[id.0].npc_customisation = Some(NpcCustomisation {
                uid: 0,
                model_ids: source.clone(),
                recolor_dst: definition.recolor_dst.clone(),
                retexture_dst: definition.retexture_dst.clone(),
            });
        }
        self.next_unique_id = self.next_unique_id.wrapping_add(1);
        let uid = (i64::from(self.next_unique_id.wrapping_sub(1)) << 32) | 0xffff_ffff;
        let customisation = self.arena[id.0]
            .npc_customisation
            .as_mut()
            .ok_or(ComponentFailure::NoComponent)?;
        customisation.uid = uid;
        match op {
            MediaOp::NpcCustomBody | MediaOp::NpcCustomHead => {
                let source = if op == MediaOp::NpcCustomBody {
                    definition.model_ids.as_ref()
                } else {
                    definition.head_models.as_ref()
                }
                .ok_or(ComponentFailure::NoComponent)?;
                let index = usize::try_from(slot).ok().filter(|index| *index < source.len());
                let Some(index) = index else {
                    return Err(ComponentFailure::Rejected);
                };
                *customisation
                    .model_ids
                    .get_mut(index)
                    .ok_or(ComponentFailure::Index)? = value;
            }
            _ => {
                let recolor = op == MediaOp::NpcCustomRecol;
                let source = if recolor {
                    definition.recolor_dst.as_ref()
                } else {
                    definition.retexture_dst.as_ref()
                };
                let index = source.and_then(|source| {
                    usize::try_from(slot).ok().filter(|index| *index < source.len())
                });
                let Some(index) = index else {
                    return Err(ComponentFailure::Rejected);
                };
                let target = if recolor {
                    customisation.recolor_dst.as_mut()
                } else {
                    customisation.retexture_dst.as_mut()
                }
                .ok_or(ComponentFailure::NoComponent)?;
                *target.get_mut(index).ok_or(ComponentFailure::Index)? = value as i16;
            }
        }
        self.redraw(id);
        Ok(())
    }
}

#[derive(Clone, Debug)]
pub(crate) struct TextView {
    pub font_id: i32,
    pub text: Arc<str>,
    pub width: i32,
    pub line_spacing: i32,
    pub sprite_id: i32,
}

impl ComponentModel {
    pub(crate) fn text_view(&self, id: ComponentId) -> TextView {
        let state = &self.arena[id.0];
        TextView {
            font_id: state.font_id,
            text: state.text.clone(),
            width: state.width,
            line_spacing: state.line_spacing,
            sprite_id: state.sprite_id,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct HookRequest {
    pub source_hash: i32,
    pub params: HookScript,
    pub request_id: i32,
}

impl ComponentModel {
    pub fn set_hook_request_count(&mut self, count: i32) {
        self.hook_request_count = count;
    }

    pub fn hook_request_count(&self) -> i32 {
        self.hook_request_count
    }

    pub fn pending_hook_requests(&self) -> &[HookRequest] {
        &self.pending_hook_requests
    }

    pub fn take_hook_requests(&mut self) -> Vec<HookRequest> {
        std::mem::take(&mut self.pending_hook_requests)
    }

    pub(crate) fn call_on_resize(&mut self, id: ComponentId) -> Result<(), ComponentFailure> {
        if self.hook_request_count >= 10 {
            return Err(ComponentFailure::Rejected);
        }
        let state = &self.arena[id.0];
        if let Some(script) = state.hooks.script(HookSlot::OnResize) {
            let request = HookRequest {
                source_hash: state.id_hash,
                params: script.clone(),
                request_id: self.hook_request_count.wrapping_add(1),
            };
            self.pending_hook_requests.insert(0, request);
        }
        Ok(())
    }
}

pub(crate) struct PausePress {
    pub(crate) hash: i32,
    pub(crate) slot: i32,
    pub(crate) failure: Option<ComponentFailure>,
}

impl ComponentModel {
    pub fn active_dialogue_continue(&self) -> Option<ComponentId> {
        self.active_dialogue_continue
    }

    pub(crate) fn resume_pause_button(
        &mut self,
        id: ComponentId,
    ) -> Result<Option<PausePress>, ComponentFailure> {
        let settings = self.settings_of(id).ok_or(ComponentFailure::NoComponent)?;
        if !settings.continue_option_enabled() || self.active_dialogue_continue.is_some() {
            return Ok(None);
        }
        let state = &self.arena[id.0];
        let (hash, slot) = (state.id_hash, state.slot_id);
        let listed = match self.list_component(hash, slot) {
            Ok(Some(listed)) => Ok(listed),
            Ok(None) | Err(ComponentError::Unavailable) => Err(ComponentFailure::NoComponent),
            Err(ComponentError::Failure(failure)) => Err(failure),
        };
        if let Ok(listed) = listed {
            self.active_dialogue_continue = Some(listed);
            self.redraw(listed);
        }
        Ok(Some(PausePress {
            hash,
            slot,
            failure: listed.err(),
        }))
    }

    pub(crate) fn has_open_interfaces(&self) -> bool {
        !self.open_interfaces.is_empty()
    }

    pub fn forget_dialogue_continue(&mut self) {
        self.active_dialogue_continue = None;
    }

    pub(crate) fn clear_dialogue_continue(&mut self) {
        if let Some(id) = self.active_dialogue_continue.take() {
            self.redraw(id);
        }
    }
}
