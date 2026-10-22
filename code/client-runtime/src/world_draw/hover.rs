use super::DrawBuilder;
use crate::menu_targets::{GroundItemTarget, GroundStackTarget, NpcTarget, PlayerTarget};
use crate::world_scene::WorldScene;
use openrs_entity::config::{ItemType, item as load_item};

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct InteractionColours {
    pub members: i32,
    pub free: i32,
}

fn text(bytes: &[u8]) -> String {
    String::from_utf8_lossy(bytes).into_owned()
}

fn options(source: &[Option<Vec<u8>>; 5]) -> Vec<Option<String>> {
    source
        .iter()
        .map(|option| option.as_deref().map(text))
        .collect()
}

fn item_target(ty: &ItemType, colours: InteractionColours) -> GroundItemTarget {
    let color = if ty.has_tooltip_color {
        ty.tooltip_color
    } else if ty.members_only {
        colours.members
    } else {
        colours.free
    };
    GroundItemTarget {
        id: ty.id,
        name: text(&ty.name),
        color,
        actions: options(&ty.ground_options),
        primary_cursor_index: ty.primary_cursor_index,
        primary_cursor: ty.primary_cursor,
        secondary_cursor_index: ty.secondary_cursor_index,
        secondary_cursor: ty.secondary_cursor,
        use_on_allowed: false,
    }
}

impl DrawBuilder {
    pub fn player_target(&mut self, scene: &WorldScene, index: u16) -> Option<PlayerTarget> {
        let state = scene.player_states.get(&index)?;
        let (_, _, plane) = scene.player_position(index)?;
        let bytes = state.appearance.clone()?;
        let defaults = self.defaults()?;
        let appearance = self.decode_worn(&bytes, &defaults)?;
        let mut name = appearance.prefix_title.clone().unwrap_or_default();
        name.push_str(&appearance.username);
        if let Some(postfix) = &appearance.postfix_title {
            name.push_str(postfix);
        }
        let own_plane = scene.own_position().map(|(_, _, plane)| plane);
        Some(PlayerTarget {
            index: i32::from(index),
            name,
            combat_level: i32::from(appearance.combat_level),
            cb_level_related: appearance.related_level,
            cb_level_with_summ: i32::from(appearance.summoning_combat_level),
            elo: appearance.elo,
            team_id: appearance.team,
            reduce_attack_priority: false,
            different_plane: own_plane != Some(plane),
        })
    }

    pub fn npc_target(
        &mut self,
        scene: &WorldScene,
        index: u16,
        varp: &dyn Fn(i32) -> i32,
    ) -> Option<NpcTarget> {
        let (_, _, plane) = scene.npc_position(index)?;
        let ty = self.resolve_npc(scene.npc_type_id(index)?, scene, varp)?;
        let own_plane = scene.own_position().map(|(_, _, plane)| plane);
        Some(NpcTarget {
            index: i32::from(index),
            name: text(&ty.name),
            combat_level: ty.combat_level,
            visible: ty.visible,
            game_type: ty.game_type,
            actions: options(&ty.options),
            primary_cursor_index: ty.primary_cursor_index,
            primary_cursor: ty.primary_cursor,
            secondary_cursor_index: ty.secondary_cursor_index,
            secondary_cursor: ty.secondary_cursor,
            attack_cursor: ty.attack_cursor,
            different_plane: own_plane != Some(plane),
            use_on_allowed: false,
        })
    }

    pub fn ground_target(
        &mut self,
        scene: &WorldScene,
        key: (u8, i32, i32),
        colours: InteractionColours,
    ) -> Option<GroundStackTarget> {
        let items = scene.ground_items().get(&key)?;
        let base = scene.base();
        let own_plane = scene.own_position().map(|(_, _, plane)| plane);
        let mut targets = Vec::new();
        for (id, _) in self.ground_order(items) {
            let Ok(ty) = load_item(&self.store, id) else {
                break;
            };
            targets.push(item_target(&ty, colours));
        }
        Some(GroundStackTarget {
            start_x: key.1 - base.0,
            start_z: key.2 - base.1,
            same_plane: own_plane == Some(key.0),
            items: targets,
        })
    }
}
