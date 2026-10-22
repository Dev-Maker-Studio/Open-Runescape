use super::*;
use openrs_login::world_packets::chat::IgnoredName;
use openrs_login::world_packets::scene::KnownPlayer;

fn session(overlay: &SceneOverlay) -> SceneSession {
    let mut session = overlay.session_template((3200, 3200));
    session.players.insert(
        7,
        KnownPlayer {
            username: "zezima".to_string(),
            display_name: Some("Zezima".to_string()),
            has_appearance: true,
            prefix_title: None,
            postfix_title: None,
        },
    );
    session
}

fn game_message(kind: u8, name: Option<&str>, message: &str) -> Vec<u8> {
    let mut payload = vec![kind];
    payload.extend_from_slice(&0_i32.to_be_bytes());
    match name {
        Some(name) => {
            payload.push(1);
            payload.extend_from_slice(name.as_bytes());
            payload.push(0);
        }
        None => payload.push(0),
    }
    payload.extend_from_slice(message.as_bytes());
    payload.push(0);
    payload
}

fn arrow_payload(slot: u8, target_type: u8, x: u16, y: u16) -> Vec<u8> {
    let mut payload = vec![slot << 5 | target_type, 1, 1];
    payload.extend_from_slice(&x.to_be_bytes());
    payload.extend_from_slice(&y.to_be_bytes());
    payload.push(10);
    payload.extend_from_slice(&600_u16.to_be_bytes());
    payload.extend_from_slice(&1234_i32.to_be_bytes());
    payload
}

#[test]
fn game_messages_shift_into_the_chat_box_newest_first() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((0, 0));
    for text in ["one", "two", "three"] {
        let effect = overlay.apply(
            ServerPacket::GAME_MESSAGE,
            &game_message(0, None, text),
            &scene,
            0,
        );
        assert_eq!(effect.action, "chatLine");
    }
    assert_eq!(overlay.chat_state().line_count, 3);
    let lines = &overlay.chat_state().lines;
    assert_eq!(lines[0].as_ref().unwrap().message.as_deref(), Some("three"));
    assert_eq!(lines[1].as_ref().unwrap().message.as_deref(), Some("two"));
    assert_eq!(lines[2].as_ref().unwrap().message.as_deref(), Some("one"));
    let identifiers: Vec<i32> = lines
        .iter()
        .flatten()
        .map(|line| line.identifier)
        .collect();
    assert_eq!(identifiers, [2, 1, 0]);
}

#[test]
fn chat_box_drops_the_oldest_line_at_capacity() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((0, 0));
    for index in 0..(CHAT_LINE_CAPACITY + 5) {
        overlay.apply(
            ServerPacket::GAME_MESSAGE,
            &game_message(0, None, &index.to_string()),
            &scene,
            0,
        );
    }
    let state = overlay.chat_state();
    assert_eq!(state.lines.len(), CHAT_LINE_CAPACITY);
    assert_eq!(
        state.lines[0].as_ref().unwrap().message.as_deref(),
        Some("104")
    );
    assert_eq!(
        state.lines[CHAT_LINE_CAPACITY - 1]
            .as_ref()
            .unwrap()
            .message
            .as_deref(),
        Some("5")
    );
}

#[test]
fn unknown_game_message_types_fall_back_to_game() {
    assert_eq!(message_type_for(4), TYPE_GAME);
    assert_eq!(message_type_for(123), TYPE_GAME);
    assert_eq!(message_type_for(TYPE_CLAN_CHAT), TYPE_CLAN_CHAT);
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((0, 0));
    overlay.apply(
        ServerPacket::GAME_MESSAGE,
        &game_message(123, None, "x"),
        &scene,
        0,
    );
    assert_eq!(
        overlay.chat_state().lines[0].as_ref().unwrap().kind,
        Some(TYPE_GAME)
    );
}

#[test]
fn console_types_never_reach_the_chat_box() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((0, 0));
    let effect = overlay.apply(
        ServerPacket::GAME_MESSAGE,
        &game_message(99, None, "log"),
        &scene,
        0,
    );
    assert_eq!(effect.action, "devConsole");
    let effect = overlay.apply(
        ServerPacket::GAME_MESSAGE,
        &game_message(98, None, "text"),
        &scene,
        0,
    );
    assert_eq!(effect.action, "devConsoleText");
    assert_eq!(overlay.console_lines(), ["log"]);
    assert_eq!(overlay.console_text(), "text");
    assert_eq!(overlay.chat_state().line_count, 0);
}

#[test]
fn ignored_names_drop_game_messages() {
    let mut overlay = SceneOverlay::default();
    overlay.set_ignored(vec![IgnoredName {
        username: Some("bob".to_string()),
        display_name: None,
    }]);
    let scene = overlay.session_template((0, 0));
    let effect = overlay.apply(
        ServerPacket::GAME_MESSAGE,
        &game_message(4, Some("Bob"), "hi"),
        &scene,
        0,
    );
    assert_eq!(effect.action, "ignored");
    let effect = overlay.apply(
        ServerPacket::GAME_MESSAGE,
        &game_message(4, Some("Alice"), "hi"),
        &scene,
        0,
    );
    assert_eq!(effect.action, "chatLine");
    assert_eq!(overlay.chat_state().line_count, 1);
    let line = overlay.chat_state().lines[0].as_ref().unwrap();
    assert_eq!(line.name.as_deref(), Some("Alice"));
    assert_eq!(line.crowned_name.as_deref(), Some("Alice"));
    assert_eq!(line.quickchat_message_id, -1);
    assert_eq!(line.clan, None);
}

#[test]
fn public_chat_sets_overhead_text_and_a_chat_line() {
    let mut overlay = SceneOverlay::default();
    overlay.advance_cycles(0, 40);
    let scene = session(&overlay);
    let effect = overlay.apply(
        ServerPacket::MESSAGE_PUBLIC,
        &[0, 7, 0x12, 0x34, 2, 0],
        &scene,
        0,
    );
    assert_eq!((effect.action, effect.index), ("publicChat", Some(7)));
    let entry = overlay.overhead().next().unwrap().clone();
    assert_eq!(entry.player_index, 7);
    assert_eq!((entry.colour, entry.effect), (0x12, 0x34));
    assert_eq!(entry.ticks_left, 150);
    assert_eq!(entry.ticks_visible, 150);
    let line = overlay.chat_state().lines[0].as_ref().unwrap();
    assert_eq!(line.kind, Some(TYPE_STAFF_CHAT));
    assert_eq!(line.time, 40);
}

#[test]
fn overhead_text_expires_after_its_ticks_and_keeps_visible_count() {
    let mut overlay = SceneOverlay::default();
    let scene = session(&overlay);
    overlay.apply(
        ServerPacket::MESSAGE_PUBLIC,
        &[0, 7, 0, 0, 2, 0],
        &scene,
        0,
    );
    overlay.advance_cycles(149, 149);
    let entry = overlay.overhead().next().unwrap();
    assert_eq!((entry.ticks_left, entry.ticks_visible), (1, 150));
    overlay.advance_cycles(1, 150);
    assert!(overlay.overhead().next().is_none());
}

#[test]
fn a_second_public_chat_replaces_the_overhead_text() {
    let mut overlay = SceneOverlay::default();
    let scene = session(&overlay);
    overlay.apply(ServerPacket::MESSAGE_PUBLIC, &[0, 7, 1, 0, 2, 0], &scene, 0);
    overlay.advance_cycles(10, 10);
    overlay.apply(ServerPacket::MESSAGE_PUBLIC, &[0, 7, 2, 0, 2, 0], &scene, 0);
    assert_eq!(overlay.overhead().count(), 1);
    let entry = overlay.overhead().next().unwrap();
    assert_eq!((entry.colour, entry.ticks_left), (2, 150));
    assert_eq!(overlay.chat_state().line_count, 2);
}

#[test]
fn unknown_public_chat_players_are_decode_errors() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((0, 0));
    let effect = overlay.apply(
        ServerPacket::MESSAGE_PUBLIC,
        &[0, 7, 0, 0, 2, 0],
        &scene,
        0,
    );
    assert_ne!(effect.action, "publicChat");
    assert_eq!(overlay.overhead().count(), 0);
    assert_eq!(overlay.chat_state().line_count, 0);
}

#[test]
fn hint_arrows_fill_and_clear_slots() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((3200, 3200));
    let effect = overlay.apply(
        ServerPacket::HINT_ARROW,
        &arrow_payload(2, 2, 3210, 3195),
        &scene,
        1,
    );
    assert_eq!((effect.action, effect.index), ("hintArrow", Some(2)));
    let set = overlay.hint_set();
    assert_eq!(set.arrows.len(), 1);
    let spec = &set.arrows[0];
    assert_eq!((spec.slot, spec.target_type), (2, 2));
    assert_eq!((spec.x, spec.y), (10 * 512 + 256, 256 - 5 * 512));
    assert_eq!((spec.distance, spec.model_id), (600, 1234));
    assert!(overlay.arrow(2).is_some());
    let effect = overlay.apply(ServerPacket::HINT_ARROW, &[2 << 5], &scene, 1);
    assert_eq!(effect.action, "hintArrowClear");
    assert!(overlay.hint_set().is_empty());
}

#[test]
fn arrow_sprites_beyond_the_cache_count_are_ignored() {
    let mut overlay = SceneOverlay::default();
    overlay.set_hint_sprite_count(1);
    let scene = overlay.session_template((3200, 3200));
    let effect = overlay.apply(ServerPacket::HINT_ARROW, &[2, 1], &scene, 0);
    assert_eq!(effect.action, "ignored");
    assert!(overlay.hint_set().is_empty());
}

#[test]
fn hint_trails_keep_the_plane_at_receipt() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((3200, 3200));
    let payload = [1, 0, 5, 65, 0, 10, 0, 20, 1, 0xff];
    let effect = overlay.apply(ServerPacket::HINT_TRAIL, &payload, &scene, 2);
    assert_eq!((effect.action, effect.index), ("hintTrail", Some(1)));
    let set = overlay.hint_set();
    assert_eq!(set.trails.len(), 1);
    assert_eq!(set.trails[0].plane, 2);
    assert_eq!(set.trails[0].slot, 1);
    assert_eq!(set.trails[0].placements, vec![(11, 19)]);
    let effect = overlay.apply(
        ServerPacket::HINT_TRAIL,
        &[1, 0x7f, 0xff],
        &scene,
        2,
    );
    assert_eq!(effect.action, "hintTrailClear");
    assert!(overlay.hint_set().is_empty());
}

#[test]
fn cutscene_starts_and_ends_with_the_next_map() {
    let mut overlay = SceneOverlay::default();
    let scene = overlay.session_template((0, 0));
    let mut payload = vec![0, 9, 0, 1];
    for value in [1_i32, -2, 3, -4] {
        payload.extend_from_slice(&value.to_be_bytes());
    }
    payload.extend_from_slice(&[2, 0xaa, 0xbb]);
    overlay.advance_cycles(5, 77);
    let effect = overlay.apply(ServerPacket::CUTSCENE, &payload, &scene, 0);
    assert_eq!((effect.action, effect.index), ("cutscene", Some(9)));
    let state = overlay.cutscene().unwrap();
    assert_eq!(
        (state.key, state.xtea_sets, state.buffer_bytes, state.started_cycle),
        (9, 1, 2, 77)
    );
    assert!(overlay.map_rebuilt());
    assert!(overlay.cutscene().is_none());
    assert!(!overlay.map_rebuilt());
}

#[test]
fn reset_world_keeps_chat_history_and_identity_tables() {
    let mut overlay = SceneOverlay::default();
    let scene = session(&overlay);
    overlay.apply(ServerPacket::MESSAGE_PUBLIC, &[0, 7, 0, 0, 2, 0], &scene, 0);
    overlay.apply(ServerPacket::HINT_ARROW, &arrow_payload(0, 2, 3210, 3195), &scene, 0);
    overlay.reset_world();
    assert_eq!(overlay.overhead().count(), 0);
    assert!(overlay.hint_set().is_empty());
    assert_eq!(overlay.chat_state().line_count, 1);
}

#[test]
fn handled_packets_cover_the_chat_and_scene_events() {
    for packet in [
        ServerPacket::MESSAGE_PUBLIC,
        ServerPacket::HINT_ARROW,
        ServerPacket::HINT_TRAIL,
        ServerPacket::CUTSCENE,
        ServerPacket::GAME_MESSAGE,
    ] {
        assert!(SceneOverlay::handles(packet));
    }
    assert!(!SceneOverlay::handles(ServerPacket::REGION));
}
