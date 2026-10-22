use crate::minimenu::{EntrySpec, MiniMenu};
use serde::Deserialize;

pub const ENTRY_CAP: i32 = 412;
pub const ATTACK: &str = "Attack";
pub const EXAMINE: &str = "Examine";
pub const FACE_HERE: &str = "Face here";
pub const LEVEL: &str = "level: ";
pub const SELF_LABEL: &str = "Self";
pub const WHITE: i32 = 0x00ff_ffff;
pub const YELLOW: i32 = 0x00ff_ff00;
pub const CYAN: i32 = 0x0000_ffff;
pub const GREY: i32 = 0x00cc_cccc;
pub const PLAYER_OP_BASE_TYPES: [i32; 8] = [44, 45, 46, 47, 48, 49, 50, 51];
pub const USE_PLAYER_SELF: i32 = 0x10;
pub const USE_PLAYER: i32 = 0x8;
pub const USE_OBJ: i32 = 0x1;
pub const USE_NPC: i32 = 0x2;
pub const USE_LOC: i32 = 0x4;
pub const USE_COMPONENT: i32 = 0x20;
pub const USE_TILE: i32 = 0x40;

pub fn text_color(color: i32) -> String {
    format!("<col={color:x}>")
}

pub fn combat_color(target: i32, mine: i32) -> String {
    let diff = mine - target;
    let color = if diff < -9 {
        0x00ff_0000
    } else if diff < -6 {
        0x00ff_3000
    } else if diff < -3 {
        0x00ff_7000
    } else if diff < 0 {
        0x00ff_b000
    } else if diff > 9 {
        0x0000_ff00
    } else if diff > 6 {
        0x0040_ff00
    } else if diff > 3 {
        0x0080_ff00
    } else if diff > 0 {
        0x00c0_ff00
    } else {
        0x00ff_ff00
    };
    text_color(color)
}

#[derive(Clone, Debug, Default, PartialEq, Eq, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct UseOn {
    pub kind: String,
    pub target: String,
    pub cursor: i32,
    pub flags: i32,
    pub selected_hash: i32,
}

#[derive(Clone, Debug, Default, PartialEq, Eq, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PlayerOption {
    pub text: String,
    pub reduced_priority: bool,
    pub cursor: i32,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct Me {
    pub index: i32,
    pub combat_level: i32,
    pub cb_level_related: i32,
    pub team_id: i32,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct MenuState {
    pub use_on: Option<UseOn>,
    pub me: Me,
    pub reduce_attack_priority: bool,
    pub default_attack_cursor: i32,
    pub active_cursor: i32,
    pub uptext: String,
    pub player_options: [Option<PlayerOption>; 8],
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct PlayerTarget {
    pub index: i32,
    pub name: String,
    pub combat_level: i32,
    pub cb_level_related: i32,
    pub cb_level_with_summ: i32,
    pub elo: i32,
    pub team_id: i32,
    pub reduce_attack_priority: bool,
    pub different_plane: bool,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct NpcTarget {
    pub index: i32,
    pub name: String,
    pub combat_level: i32,
    pub visible: bool,
    pub game_type: i32,
    pub actions: Vec<Option<String>>,
    pub primary_cursor_index: i32,
    pub primary_cursor: i32,
    pub secondary_cursor_index: i32,
    pub secondary_cursor: i32,
    pub attack_cursor: i32,
    pub different_plane: bool,
    pub use_on_allowed: bool,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct GroundItemTarget {
    pub id: i32,
    pub name: String,
    pub color: i32,
    pub actions: Vec<Option<String>>,
    pub primary_cursor_index: i32,
    pub primary_cursor: i32,
    pub secondary_cursor_index: i32,
    pub secondary_cursor: i32,
    pub use_on_allowed: bool,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct GroundStackTarget {
    pub start_x: i32,
    pub start_z: i32,
    pub same_plane: bool,
    pub items: Vec<GroundItemTarget>,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct LocTarget {
    pub uid: i64,
    pub start_x: i32,
    pub start_z: i32,
    pub hash: i64,
    pub name: String,
    pub actions: Vec<Option<String>>,
    pub primary_cursor_index: i32,
    pub primary_cursor: i32,
    pub secondary_cursor_index: i32,
    pub secondary_cursor: i32,
    pub same_plane: bool,
    pub use_on_allowed: bool,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct TileTarget {
    pub x: i32,
    pub y: i32,
}

#[derive(Clone, Debug, Default, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ComponentTarget {
    pub container_item_id: i32,
    pub slot_id: i32,
    pub id_hash: i32,
    pub op_base: String,
    pub options: [Option<String>; 10],
    pub option_cursors: [i32; 10],
    pub use_option: Option<String>,
    pub target_op_cursor: i32,
    pub continue_text: Option<String>,
    pub targetable: bool,
    pub use_on_allowed: bool,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
#[allow(clippy::large_enum_variant)]
pub enum Target {
    Tile(TileTarget),
    Player(PlayerTarget),
    Npc(NpcTarget),
    Ground(GroundStackTarget),
    Loc(LocTarget),
    Component(ComponentTarget),
}

fn spec(
    action: impl Into<String>,
    colored_name: impl Into<String>,
    cursor: i32,
    click_type: i32,
) -> EntrySpec {
    EntrySpec {
        action: action.into(),
        colored_name: colored_name.into(),
        cursor,
        click_type,
        item_id: -1,
        entity_uid: 0,
        param0: 0,
        param1: 0,
        selectable: true,
        other_level: false,
        bitpacked: 0,
        uncategorized: false,
    }
}

fn cursor_for(
    index: usize,
    primary_index: i32,
    primary: i32,
    secondary_index: i32,
    secondary: i32,
    default: i32,
) -> i32 {
    let index = index as i32;
    if index == primary_index {
        primary
    } else if index == secondary_index {
        secondary
    } else {
        default
    }
}

fn use_flag(state: &MenuState, flag: i32) -> Option<&UseOn> {
    state.use_on.as_ref().filter(|u| u.flags & flag != 0)
}

pub fn player_hovered(menu: &mut MiniMenu, state: &MenuState, player: &PlayerTarget) {
    if menu.entry_count >= ENTRY_CAP {
        return;
    }
    if player.index == state.me.index {
        if let Some(use_on) = use_flag(state, USE_PLAYER_SELF) {
            let mut entry = spec(
                use_on.kind.clone(),
                format!("{} -> {}{SELF_LABEL}", use_on.target, text_color(WHITE)),
                use_on.cursor,
                16,
            );
            entry.bitpacked = i64::from(player.index);
            menu.add_entry(entry);
        }
        return;
    }
    let username = match player.elo {
        0 => {
            let mut colored = true;
            if state.me.cb_level_related != -1 && player.cb_level_related != -1 {
                let related = state.me.cb_level_related.min(player.cb_level_related);
                let delta = (state.me.combat_level - player.combat_level).abs();
                if delta > related {
                    colored = false;
                }
            }
            let color = if colored {
                combat_color(player.combat_level, state.me.combat_level)
            } else {
                text_color(WHITE)
            };
            if player.combat_level >= player.cb_level_with_summ {
                format!("{}{color} ({LEVEL}{})", player.name, player.combat_level)
            } else {
                format!(
                    "{}{color} ({LEVEL}{}+{})",
                    player.name,
                    player.combat_level,
                    player.cb_level_with_summ - player.combat_level
                )
            }
        }
        -1 => player.name.clone(),
        elo => format!("{} (skill: {elo})", player.name),
    };
    if !player.different_plane
        && let Some(use_on) = use_flag(state, USE_PLAYER)
    {
        let mut entry = spec(
            use_on.kind.clone(),
            format!("{} -> {}{username}", use_on.target, text_color(WHITE)),
            use_on.cursor,
            15,
        );
        entry.entity_uid = i64::from(player.index);
        entry.bitpacked = i64::from(player.index);
        menu.add_entry(entry);
    }
    if player.different_plane {
        let mut entry = spec(format!("{}{username}", text_color(GREY)), "", -1, -1);
        entry.selectable = false;
        entry.other_level = true;
        entry.bitpacked = i64::from(player.index);
        menu.add_entry(entry);
        return;
    }
    for index in (0..8).rev() {
        let Some(option) = state.player_options[index].as_ref() else {
            continue;
        };
        let mut priority = 0;
        if option.text.eq_ignore_ascii_case(ATTACK) {
            if state.reduce_attack_priority && player.combat_level > state.me.combat_level {
                priority = 2000;
            }
            if state.me.team_id != 0 && player.team_id != 0 {
                priority = if player.team_id == state.me.team_id {
                    2000
                } else {
                    0
                };
            } else if player.reduce_attack_priority {
                priority = 2000;
            }
        } else if option.reduced_priority {
            priority = 2000;
        }
        let cursor = if option.cursor != -1 {
            option.cursor
        } else {
            state.default_attack_cursor
        };
        let mut entry = spec(
            option.text.clone(),
            format!("{}{username}", text_color(WHITE)),
            cursor,
            PLAYER_OP_BASE_TYPES[index] + priority,
        );
        entry.entity_uid = i64::from(player.index);
        entry.bitpacked = i64::from(player.index);
        menu.add_entry(entry);
    }
    let target = format!("{}{username}", text_color(WHITE));
    if let Some(entry) = menu.entries().into_iter().find(|e| e.click_type() == 23) {
        entry.set_target_name(Some(target));
    }
}

pub fn tile_hovered(menu: &mut MiniMenu, state: &MenuState, tile: &TileTarget) {
    let bitpacked = i64::from(tile.x | tile.y);
    if let Some(use_on) = use_flag(state, USE_TILE) {
        let mut entry = spec(use_on.kind.clone(), " -> ", use_on.cursor, 59);
        entry.param0 = tile.x;
        entry.param1 = tile.y;
        entry.bitpacked = bitpacked;
        entry.uncategorized = true;
        menu.add_entry(entry);
        return;
    }
    if state.use_on.is_some() {
        return;
    }
    if menu.config.show_face_here {
        let mut entry = spec(FACE_HERE, "", -1, 60);
        entry.param0 = tile.x;
        entry.param1 = tile.y;
        entry.bitpacked = bitpacked;
        entry.uncategorized = true;
        menu.add_entry(entry);
    }
    let mut entry = spec(state.uptext.clone(), "", state.active_cursor, 23);
    entry.param0 = tile.x;
    entry.param1 = tile.y;
    entry.bitpacked = bitpacked;
    entry.uncategorized = true;
    menu.add_entry(entry);
}

pub fn npc_hovered(menu: &mut MiniMenu, state: &MenuState, npc: &NpcTarget) {
    if menu.entry_count >= ENTRY_CAP || !npc.visible {
        return;
    }
    let mut name = npc.name.clone();
    if npc.combat_level != 0 {
        name = format!(
            "{name}{} ({LEVEL}{})",
            combat_color(npc.combat_level, state.me.combat_level),
            npc.combat_level
        );
    }
    if !npc.different_plane
        && npc.use_on_allowed
        && let Some(use_on) = use_flag(state, USE_NPC)
    {
        let mut entry = spec(
            use_on.kind.clone(),
            format!("{} -> {}{name}", use_on.target, text_color(YELLOW)),
            use_on.cursor,
            8,
        );
        entry.entity_uid = i64::from(npc.index);
        entry.bitpacked = i64::from(npc.index);
        menu.add_entry(entry);
    }
    if npc.different_plane {
        return;
    }
    let click_for = |index: usize| match index {
        0 => 9,
        1 => 10,
        2 => 11,
        3 => 12,
        4 => 13,
        5 => 1003,
        _ => 0,
    };
    let is_special =
        |option: &str| option.eq_ignore_ascii_case(ATTACK) || option.eq_ignore_ascii_case(EXAMINE);
    let add = |menu: &mut MiniMenu, index: usize, option: &str, click_type: i32| {
        let cursor = cursor_for(
            index,
            npc.primary_cursor_index,
            npc.primary_cursor,
            npc.secondary_cursor_index,
            npc.secondary_cursor,
            state.default_attack_cursor,
        );
        let cursor = if option.eq_ignore_ascii_case(ATTACK) {
            npc.attack_cursor
        } else {
            cursor
        };
        let mut entry = spec(
            option,
            format!("{}{name}", text_color(YELLOW)),
            cursor,
            click_type,
        );
        entry.entity_uid = i64::from(npc.index);
        entry.bitpacked = i64::from(npc.index);
        menu.add_entry(entry);
    };
    for index in (0..npc.actions.len()).rev() {
        if let Some(option) = npc.actions[index].as_deref()
            && (npc.game_type == 0 || !is_special(option))
        {
            add(menu, index, option, click_for(index));
        }
    }
    if npc.game_type == 1 {
        for index in 0..npc.actions.len() {
            if let Some(option) = npc.actions[index].as_deref()
                && is_special(option)
            {
                let priority = if npc.combat_level > state.me.combat_level {
                    2000
                } else {
                    0
                };
                let mut click_type = click_for(index);
                if click_type != 0 {
                    click_type += priority;
                }
                add(menu, index, option, click_type);
            }
        }
    }
}

pub fn ground_hovered(menu: &mut MiniMenu, state: &MenuState, stack: &GroundStackTarget) {
    for (stack_index, item) in stack.items.iter().enumerate() {
        let stack_index = stack_index as i64;
        if stack.same_plane
            && item.use_on_allowed
            && let Some(use_on) = use_flag(state, USE_OBJ)
        {
            let mut entry = spec(
                use_on.kind.clone(),
                format!(
                    "{} -> {}{}",
                    use_on.target,
                    text_color(item.color),
                    item.name
                ),
                use_on.cursor,
                17,
            );
            entry.entity_uid = i64::from(item.id);
            entry.param0 = stack.start_x;
            entry.param1 = stack.start_z;
            entry.bitpacked = stack_index;
            menu.add_entry(entry);
        }
        if !stack.same_plane {
            continue;
        }
        for index in (0..item.actions.len()).rev() {
            let Some(action) = item.actions[index].as_deref() else {
                continue;
            };
            let click_type = match index {
                0 => 18,
                1 => 19,
                2 => 20,
                3 => 21,
                4 => 22,
                5 => 1004,
                _ => 0,
            };
            let cursor = cursor_for(
                index,
                item.primary_cursor_index,
                item.primary_cursor,
                item.secondary_cursor_index,
                item.secondary_cursor,
                state.default_attack_cursor,
            );
            let mut entry = spec(
                action,
                format!("{}{}", text_color(item.color), item.name),
                cursor,
                click_type,
            );
            entry.entity_uid = i64::from(item.id);
            entry.param0 = stack.start_x;
            entry.param1 = stack.start_z;
            entry.bitpacked = stack_index;
            menu.add_entry(entry);
        }
    }
}

pub fn loc_hovered(menu: &mut MiniMenu, state: &MenuState, loc: &LocTarget) {
    if loc.same_plane
        && loc.use_on_allowed
        && let Some(use_on) = use_flag(state, USE_LOC)
    {
        let mut entry = spec(
            use_on.kind.clone(),
            format!("{} -> {}{}", use_on.target, text_color(CYAN), loc.name),
            use_on.cursor,
            2,
        );
        entry.entity_uid = loc.uid;
        entry.param0 = loc.start_x;
        entry.param1 = loc.start_z;
        entry.bitpacked = loc.hash;
        menu.add_entry(entry);
    }
    if !loc.same_plane {
        return;
    }
    for index in (0..loc.actions.len()).rev() {
        let Some(action) = loc.actions[index].as_deref() else {
            continue;
        };
        let click_type = match index {
            0 => 3,
            1 => 4,
            2 => 5,
            3 => 6,
            4 => 1001,
            5 => 1002,
            _ => 0,
        };
        let cursor = cursor_for(
            index,
            loc.primary_cursor_index,
            loc.primary_cursor,
            loc.secondary_cursor_index,
            loc.secondary_cursor,
            state.default_attack_cursor,
        );
        let mut entry = spec(
            action,
            format!("{}{}", text_color(CYAN), loc.name),
            cursor,
            click_type,
        );
        entry.entity_uid = loc.uid;
        entry.param0 = loc.start_x;
        entry.param1 = loc.start_z;
        entry.bitpacked = loc.hash;
        menu.add_entry(entry);
    }
}

pub fn component_hovered(menu: &mut MiniMenu, state: &MenuState, component: &ComponentTarget) {
    let packed = i64::from(component.slot_id | component.id_hash);
    let base = |action: String, name: &str, cursor: i32, click_type: i32, uid: i64| {
        let mut entry = spec(action, name, cursor, click_type);
        entry.item_id = component.container_item_id;
        entry.entity_uid = uid;
        entry.param0 = component.slot_id;
        entry.param1 = component.id_hash;
        entry.bitpacked = packed;
        entry
    };
    if component.targetable
        && component.use_on_allowed
        && let Some(use_on) = use_flag(state, USE_COMPONENT)
    {
        let name = format!("{} -> {}", use_on.target, component.op_base);
        let entry = base(use_on.kind.clone(), &name, use_on.cursor, 58, 0);
        menu.add_entry(entry);
    }
    for option in (5..=9).rev() {
        if let Some(action) = component.options[option].clone() {
            let entry = base(
                action,
                &component.op_base,
                component.option_cursors[option],
                1007,
                option as i64 + 1,
            );
            menu.add_entry(entry);
        }
    }
    if let Some(action) = component.use_option.clone() {
        let entry = base(
            action,
            &component.op_base,
            component.target_op_cursor,
            25,
            0,
        );
        menu.add_entry(entry);
    }
    for option in (0..=4).rev() {
        if let Some(action) = component.options[option].clone() {
            let entry = base(
                action,
                &component.op_base,
                component.option_cursors[option],
                57,
                option as i64 + 1,
            );
            menu.add_entry(entry);
        }
    }
    if let Some(text) = component.continue_text.clone() {
        let entry = base(text, "", -1, 30, 0);
        menu.add_entry(entry);
    }
}

pub fn build(menu: &mut MiniMenu, state: &MenuState, targets: &[Target]) {
    for target in targets {
        match target {
            Target::Tile(t) => tile_hovered(menu, state, t),
            Target::Player(p) => player_hovered(menu, state, p),
            Target::Npc(n) => npc_hovered(menu, state, n),
            Target::Ground(g) => ground_hovered(menu, state, g),
            Target::Loc(l) => loc_hovered(menu, state, l),
            Target::Component(c) => component_hovered(menu, state, c),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::minimenu::MenuConfig;

    fn menu() -> MiniMenu {
        MiniMenu::new(MenuConfig::new(765, 503))
    }

    fn state() -> MenuState {
        let mut state = MenuState {
            me: Me {
                index: 1,
                combat_level: 50,
                cb_level_related: -1,
                team_id: 0,
            },
            default_attack_cursor: 7,
            active_cursor: 3,
            uptext: "Walk here".to_string(),
            ..MenuState::default()
        };
        state.player_options[0] = Some(PlayerOption {
            text: "Attack".to_string(),
            reduced_priority: false,
            cursor: -1,
        });
        state.player_options[2] = Some(PlayerOption {
            text: "Trade with".to_string(),
            reduced_priority: true,
            cursor: 5,
        });
        state
    }

    fn texts(menu: &MiniMenu) -> Vec<(String, i32)> {
        menu.entries()
            .iter()
            .map(|e| (e.text(), e.click_type()))
            .collect()
    }

    #[test]
    fn colors_follow_the_level_difference() {
        assert_eq!(combat_color(80, 50), "<col=ff0000>");
        assert_eq!(combat_color(50, 50), "<col=ffff00>");
        assert_eq!(combat_color(43, 50), "<col=40ff00>");
        assert_eq!(combat_color(49, 50), "<col=c0ff00>");
        assert_eq!(combat_color(10, 50), "<col=ff00>");
        assert_eq!(text_color(0), "<col=0>");
    }

    #[test]
    fn player_options_carry_levels_priorities_and_target_name() {
        let mut m = menu();
        m.reset_entries();
        let s = state();
        tile_hovered(&mut m, &s, &TileTarget { x: 10, y: 12 });
        let player = PlayerTarget {
            index: 9,
            name: "Zezima".to_string(),
            combat_level: 60,
            cb_level_related: -1,
            cb_level_with_summ: 63,
            ..PlayerTarget::default()
        };
        player_hovered(&mut m, &s, &player);
        let types: Vec<i32> = m.entries().iter().map(|e| e.click_type()).collect();
        assert!(types.contains(&44) && types.contains(&2046));
        let attack = m
            .entries()
            .into_iter()
            .find(|e| e.click_type() == 44)
            .unwrap();
        assert_eq!(
            attack.colored_name,
            "<col=ffffff>Zezima<col=ff0000> (level: 60+3)"
        );
        assert_eq!(attack.cursor, 7);
        let walk = m
            .entries()
            .into_iter()
            .find(|e| e.click_type() == 23)
            .unwrap();
        assert_eq!(walk.target_name().unwrap(), attack.colored_name);
    }

    #[test]
    fn the_entry_cap_blocks_players_and_npcs() {
        let mut m = menu();
        m.reset_entries();
        m.entry_count = ENTRY_CAP;
        let before = m.entries().len();
        player_hovered(&mut m, &state(), &PlayerTarget::default());
        npc_hovered(
            &mut m,
            &state(),
            &NpcTarget {
                visible: true,
                actions: vec![Some("Talk-to".to_string())],
                ..NpcTarget::default()
            },
        );
        assert_eq!(m.entries().len(), before);
    }

    #[test]
    fn npc_attack_gets_priority_when_stronger() {
        let mut m = menu();
        m.reset_entries();
        let npc = NpcTarget {
            index: 4,
            name: "Guard".to_string(),
            combat_level: 80,
            visible: true,
            game_type: 1,
            actions: vec![
                Some("Talk-to".to_string()),
                Some("Attack".to_string()),
                None,
                Some("Examine".to_string()),
            ],
            primary_cursor_index: -1,
            secondary_cursor_index: -1,
            ..NpcTarget::default()
        };
        npc_hovered(&mut m, &state(), &npc);
        let seen = texts(&m);
        assert!(
            seen.iter()
                .any(|(t, c)| t.starts_with("Attack") && *c == 2010)
        );
        assert!(
            seen.iter()
                .any(|(t, c)| t.starts_with("Examine") && *c == 2012)
        );
        assert!(
            seen.iter()
                .any(|(t, c)| t.starts_with("Talk-to") && *c == 9)
        );
    }

    #[test]
    fn use_on_replaces_the_walk_entry_and_hides_in_other_planes() {
        let mut m = menu();
        m.reset_entries();
        let mut s = state();
        s.use_on = Some(UseOn {
            kind: "Use".to_string(),
            target: "Logs".to_string(),
            cursor: 2,
            flags: USE_TILE | USE_LOC,
            selected_hash: 0,
        });
        tile_hovered(&mut m, &s, &TileTarget { x: 1, y: 2 });
        loc_hovered(
            &mut m,
            &s,
            &LocTarget {
                name: "Fire".to_string(),
                same_plane: false,
                use_on_allowed: true,
                actions: vec![Some("Light".to_string())],
                ..LocTarget::default()
            },
        );
        let seen = texts(&m);
        assert!(seen.iter().any(|(_, c)| *c == 59));
        assert!(seen.iter().all(|(_, c)| *c != 23 && *c != 3 && *c != 2));
    }

    #[test]
    fn components_emit_option_ranges_in_order() {
        let mut m = menu();
        m.reset_entries();
        let mut options: [Option<String>; 10] = Default::default();
        options[0] = Some("Wield".to_string());
        options[7] = Some("Drop".to_string());
        let component = ComponentTarget {
            container_item_id: 995,
            slot_id: 3,
            id_hash: 0x0067_0000,
            op_base: "<col=ff9040>Coins".to_string(),
            options,
            option_cursors: [-1; 10],
            use_option: Some("Use".to_string()),
            continue_text: Some("Click here to continue".to_string()),
            ..ComponentTarget::default()
        };
        component_hovered(&mut m, &state(), &component);
        let mut seen: Vec<i32> = texts(&m).into_iter().map(|(_, c)| c).collect();
        seen.sort_unstable();
        assert_eq!(seen, vec![25, 30, 57, 1006, 1007]);
    }

    #[test]
    fn ground_stacks_and_locs_use_their_action_types() {
        let mut m = menu();
        m.reset_entries();
        let s = state();
        ground_hovered(
            &mut m,
            &s,
            &GroundStackTarget {
                start_x: 5,
                start_z: 6,
                same_plane: true,
                items: vec![GroundItemTarget {
                    id: 995,
                    name: "Coins".to_string(),
                    color: 0xff_ffff,
                    actions: vec![None, None, Some("Take".to_string())],
                    primary_cursor_index: -1,
                    secondary_cursor_index: -1,
                    ..GroundItemTarget::default()
                }],
            },
        );
        loc_hovered(
            &mut m,
            &s,
            &LocTarget {
                uid: 77,
                name: "Door".to_string(),
                same_plane: true,
                actions: vec![
                    Some("Open".to_string()),
                    None,
                    None,
                    None,
                    None,
                    Some("Examine".to_string()),
                ],
                primary_cursor_index: -1,
                secondary_cursor_index: -1,
                ..LocTarget::default()
            },
        );
        let seen = texts(&m);
        for wanted in [20, 3, 1002] {
            assert!(seen.iter().any(|(_, c)| *c == wanted), "{wanted}");
        }
    }
}
