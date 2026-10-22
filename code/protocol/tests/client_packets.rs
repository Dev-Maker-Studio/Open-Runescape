use openrs_protocol::client_packets::bodies;
use openrs_protocol::client_packets::frame::frame_body;
use openrs_protocol::client_packets::menu::{
    if_continue, if_drag_onto_if, if_on_player_direct, player_operation, process_menuclick,
};
use openrs_protocol::client_packets::preferences::{
    send_preferences_body, Preferences, PREFERENCE_VALUES,
};
use openrs_protocol::client_packets::reflection::{
    answer_without_runtime, encode_results, parse_request, OpResult,
};
use openrs_protocol::client_packets::signup::{check_email_validity, sign_up_form};
use openrs_protocol::client_packets::{
    ClientBody, ClientPacketError, Huffman, KeyPress, KeyRecorder, MenuClick, MenuContext,
    MouseRecord, MouseSubmitter, Text,
};
use openrs_protocol::isaac::Isaac;
use openrs_protocol::registry::{ClientPacket, RegistryEncoder, CLIENT_PACKETS};

const FIXTURE: &str = include_str!("fixtures/client-packets.tsv");

fn cipher() -> Option<Isaac> {
    Some(Isaac::new(&[1, 0xffff_fffe, 0x1234_5678, 0x8000_0000]).unwrap())
}

fn table_a() -> Huffman {
    let lengths: Vec<i8> = (0..256)
        .map(|i| if i < 64 { 7 } else if i < 128 { 8 } else { 9 })
        .collect();
    Huffman::new(&lengths)
}

fn table_b() -> Huffman {
    let mut lengths = vec![0i8; 256];
    for (symbols, length) in [
        (" e", 3),
        ("taoi", 4),
        ("nshrdlcu", 5),
        ("mwfgypbv", 6),
        ("kjxqz0123456789.", 7),
    ] {
        for c in symbols.bytes() {
            lengths[usize::from(c)] = length;
        }
    }
    Huffman::new(&lengths)
}

fn text(token: &str) -> Text {
    if token == "~" {
        return Text::default();
    }
    let units: Vec<u16> = (0..token.len() / 4)
        .map(|i| u16::from_str_radix(&token[i * 4..i * 4 + 4], 16).unwrap())
        .collect();
    Text(units)
}

fn int(token: &str) -> i32 {
    token.parse::<i64>().unwrap() as i32
}

fn list(token: &str) -> Vec<i32> {
    if token == "~" {
        Vec::new()
    } else {
        token.split(',').map(int).collect()
    }
}

fn context(token: &str) -> MenuContext {
    let v = list(token);
    MenuContext {
        run: v[0] != 0,
        base_x: v[1],
        base_y: v[2],
        selected_hash: v[3],
        selected_item: v[4],
        selected_slot: v[5],
        own_player_index: v[6],
        camera_yaw: v[7],
        minimap_rotation: v[8],
        minimap_zoom: v[9],
        absolute_x: v[10],
        absolute_z: v[11],
        container_item: v[12],
        target_hash: v[13],
        target_slot: v[14],
        target_item: v[15],
    }
}

fn packet(name: &str) -> ClientPacket {
    CLIENT_PACKETS
        .iter()
        .find(|m| m.name == name)
        .unwrap_or_else(|| panic!("no client packet {name}"))
        .packet
}

type Body = Result<Option<ClientBody>, ClientPacketError>;

fn plain(name: &str, body: Result<Vec<u8>, ClientPacketError>) -> Body {
    body.map(|body| {
        Some(ClientBody {
            packet: packet(name),
            body,
        })
    })
}

fn build(name: &str, a: &[&str]) -> Body {
    let n = |i: usize| int(a[i]);
    let ok = |b: Vec<u8>| Ok(b);
    match name {
        "SONG_LOADED" => plain(name, ok(bodies::song_loaded(n(0)))),
        "SOUND_EFFECT_MUSIC_ENDED" => plain(name, ok(bodies::sound_effect_music_ended(n(0)))),
        "TRANSMITVAR_VERIFYID" => plain(name, ok(bodies::transmitvar_verifyid(n(0)))),
        "REQUEST_WORLD_LIST" => plain(name, ok(bodies::request_world_list(n(0)))),
        "RESUME_COUNTDIALOG" => plain(name, ok(bodies::resume_countdialog(n(0)))),
        "PLAY_VORBIS" => plain(name, ok(bodies::play_vorbis(n(0)))),
        "RESUME_HSLDIALOG" => plain(name, ok(bodies::resume_hsldialog(n(0)))),
        "GRAND_EXCHANGE_ITEM_SELECT" => plain(name, ok(bodies::grand_exchange_item_select(n(0)))),
        "WRITE_PING" => plain(name, ok(bodies::write_ping(n(0)))),
        "CHAT_TYPE" => plain(name, ok(bodies::chat_type(n(0)))),
        "ACCOUNT_CREATION_STAGE" => plain(name, ok(bodies::account_creation_stage(n(0)))),
        "CUTSCENE_FINISHED" => plain(name, ok(bodies::cutscene_finished(n(0) != 0))),
        "CLOSE_INTERFACE" => plain(name, ok(bodies::empty())),
        "UNK_82" => plain(name, ok(bodies::unk_82())),
        "SEND_FPS" => plain(name, ok(bodies::send_fps(n(0), n(1), n(2)))),
        "CHAT_SETFILTER" => plain(name, ok(bodies::chat_setfilter(n(0), n(1), n(2)))),
        "SCREEN_SIZE" => plain(name, ok(bodies::screen_size_native(n(0), n(1), n(2), n(3)))),
        "WORLD_MAP_CLICK" => plain(name, ok(bodies::world_map_click(n(0), n(1), n(2)))),
        "RESUME_TEXTDIALOG" | "RESUME_NAMEDIALOG" | "RESUME_CLANFORUMQFCDIALOG"
        | "EMAIL_VALIDATION_SUBMIT_CODE" | "ADD_FRIEND" | "REMOVE_FRIEND" | "REMOVE_IGNORE"
        | "FC_KICK" => plain(name, bodies::string_only(text(a[0]))),
        "FC_JOIN" => {
            if a[0] == "leave" {
                plain(name, ok(bodies::fc_leave()))
            } else {
                plain(name, bodies::string_only(text(a[0])))
            }
        }
        "COMMAND" => plain(name, bodies::command(n(0) != 0, n(1) != 0, text(a[2]))),
        "FC_SET_RANK" => plain(name, bodies::fc_set_rank(n(0), text(a[1]))),
        "ADD_IGNORE" => plain(name, bodies::add_ignore(text(a[0]), n(1) != 0)),
        "REPORT_ABUSE" => plain(
            name,
            bodies::report_abuse(text(a[0]), n(1), n(2), Some(text(a[3]))),
        ),
        "UNUSED_CLAN_OP" => plain(name, bodies::send_clan_operation(n(0), text(a[1]))),
        "CLANCHANNEL_KICKUSER" => plain(name, bodies::kick_player(n(0) != 0, n(1), text(a[2]))),
        "LOBBY_HYPERLINK" => plain(
            name,
            bodies::lobby_hyperlink(text(a[0]), text(a[1]), text(a[2]), n(3) != 0, n(4) != 0),
        ),
        "BUG_REPORT" => plain(name, bodies::bug_report(n(0), text(a[1]), text(a[2]))),
        "EMAIL_VALIDATION_ADD_NEW_ADDRESS" => plain(
            name,
            bodies::email_add_new_address(text(a[0]), n(1) != 0, n(2) != 0, n(3) != 0),
        ),
        "EMAIL_VALIDATION_CHANGE_ADDRESS" => {
            plain(name, bodies::email_change_address(text(a[0]), text(a[1])))
        }
        "CHAT" => {
            let table = if a[0] == "A" { table_a() } else { table_b() };
            plain(name, bodies::chat(&table, n(1), n(2), text(a[3])))
        }
        "SEND_PRIVATE_MESSAGE" => {
            let table = if a[0] == "A" { table_a() } else { table_b() };
            plain(
                name,
                bodies::send_private_message(&table, text(a[1]), text(a[2])),
            )
        }
        "QUICKCHAT_PUBLIC" => plain(
            name,
            bodies::quickchat_public(n(0), n(1), &list(a[2]), &list(a[3])),
        ),
        "QUICKCHAT_PRIVATE" => plain(
            name,
            bodies::quickchat_private(text(a[0]), n(1), &list(a[2]), &list(a[3])),
        ),
        "MENU" => {
            let click = MenuClick {
                click_type: n(0),
                entity_uid: a[1].parse::<i64>().unwrap(),
                param0: n(2),
                param1: n(3),
            };
            process_menuclick(&click, &context(a[4]))
        }
        "IF_DRAG_ONTO_IF" => Ok(Some(if_drag_onto_if(n(0), n(1), n(2), &context(a[3])))),
        "IF_ON_PLAYER_DIRECT" => Ok(Some(if_on_player_direct(n(0), &context(a[1])))),
        "PLAYER_OPERATION" => Ok(player_operation(n(0), n(1))),
        "IF_CONTINUE" => Ok(Some(if_continue(n(0), n(1)))),
        "SEND_PREFERENCES" => {
            let mut values = [0i32; PREFERENCE_VALUES];
            for (slot, token) in values.iter_mut().zip(a) {
                *slot = int(token);
            }
            plain(
                name,
                Ok(send_preferences_body(&Preferences::new(values))),
            )
        }
        "SEND_SIGN_UP_FORM" | "CHECK_EMAIL_VALIDITY" => {
            if n(0) == 0 {
                return Ok(None);
            }
            let keys = [n(1), n(2), n(3), n(4)];
            if name == "CHECK_EMAIL_VALIDITY" {
                plain(name, check_email_validity(&keys, text(a[5])))
            } else {
                plain(
                    name,
                    sign_up_form(&keys, text(a[5]), text(a[6]), n(7), n(8) != 0),
                )
            }
        }
        "REFLECTION_REQUEST" => {
            let payload = hex_bytes(a[0]);
            Ok(parse_request(&payload).map(|request| ClientBody {
                packet: packet("REFLECTION_CHECK"),
                body: answer_without_runtime(&request),
            }))
        }
        "REFLECTION_RESULT" => {
            let results: Vec<OpResult> = a[1..].iter().map(|t| op_result(t)).collect();
            plain(
                "REFLECTION_CHECK",
                Ok(encode_results(n(0), &results)),
            )
        }
        other => panic!("unhandled fixture kind {other}"),
    }
}

fn hex_bytes(token: &str) -> Vec<u8> {
    if token == "~" {
        return Vec::new();
    }
    (0..token.len() / 2)
        .map(|i| u8::from_str_radix(&token[i * 2..i * 2 + 2], 16).unwrap())
        .collect()
}

fn op_result(token: &str) -> OpResult {
    let (kind, value) = token.split_once(':').unwrap_or((token, ""));
    match kind {
        "x" => OpResult::Failed(value.parse().unwrap()),
        "f" => OpResult::FieldValue(int(value)),
        "s" => OpResult::FieldSet,
        "fm" | "mm" => OpResult::Modifiers(int(value)),
        "mn" => OpResult::MethodNull,
        "ml" => OpResult::MethodNumber(value.parse().unwrap()),
        "ms" => OpResult::MethodString(text(value)),
        "mo" => OpResult::MethodOther,
        other => panic!("unknown result token {other}"),
    }
}

const NOW: i64 = 1_700_000_000_000;

fn mouse_record(arg: &str, base: i64) -> MouseRecord {
    let f: Vec<i64> = arg.split(',').map(|v| v.parse().unwrap()).collect();
    MouseRecord {
        x: f[0] as i32,
        y: f[1] as i32,
        click_meta: f[2] as i32,
        constant: f[3] as i32,
        time: base + f[4],
    }
}

fn run_mouse(encoder: &mut RegistryEncoder<ClientPacket>, spec: &[&str]) -> String {
    let base = if spec[0] == "F" { NOW + 100_000_000 } else { 1000 };
    let mut submitter = MouseSubmitter::new();
    submitter.reset();
    submitter.camera_cooldown = 0;
    let mut camera: Option<(f32, f32)> = None;
    let mut focus = true;
    let mut cycles: Vec<String> = Vec::new();
    let mut keys: Vec<KeyPress> = Vec::new();
    let mut head: Option<MouseRecord> = None;
    let mut flush = |submitter: &mut MouseSubmitter,
                     keys: &mut Vec<KeyPress>,
                     head: &mut Option<MouseRecord>,
                     camera: &mut Option<(f32, f32)>,
                     focus: bool,
                     cycles: &mut Vec<String>| {
        let bodies = submitter.submit_queued(NOW, head.take(), keys, camera, focus);
        keys.clear();
        let frames: Vec<String> = bodies
            .iter()
            .map(|b| hex(&encoder.encode(b.packet, &b.body).unwrap()))
            .collect();
        cycles.push(if frames.is_empty() {
            "-".to_string()
        } else {
            frames.join("|")
        });
    };
    for token in &spec[1..] {
        if *token == ";" {
            flush(&mut submitter, &mut keys, &mut head, &mut camera, focus, &mut cycles);
            continue;
        }
        let (kind, arg) = token.split_once(':').unwrap();
        match kind {
            "m" => submitter.append_movement_batch(mouse_record(arg, base)),
            "c" => submitter.append_click_batch(mouse_record(arg, base)),
            "q" => submitter.queue_click(mouse_record(arg, base)),
            "h" => head = Some(mouse_record(arg, base)),
            "k" => {
                let (code, time) = arg.split_once(',').unwrap();
                keys.push(KeyPress {
                    code: code.parse().unwrap(),
                    time: base + time.parse::<i64>().unwrap(),
                });
            }
            "cam" => {
                let (x, y) = arg.split_once(',').unwrap();
                camera = Some((x.parse().unwrap(), y.parse().unwrap()));
            }
            "foc" => focus = arg == "1",
            other => panic!("unknown mouse op {other}"),
        }
    }
    flush(&mut submitter, &mut keys, &mut head, &mut camera, focus, &mut cycles);
    cycles.join("/")
}

fn run_keys(spec: &[&str]) -> String {
    let mut recorder = KeyRecorder::new();
    let mut out = String::new();
    for token in spec {
        if *token == "F" {
            recorder.focus_lost(0);
        } else if *token == "W" {
            recorder.prepare_awaiting_keys();
            while let Some(e) = recorder.pop_head() {
                if !out.is_empty() {
                    out.push(',');
                }
                out.push_str(&format!(
                    "{}/{}/{}/{}",
                    e.entry_type, e.character, e.code, e.flags
                ));
            }
            out.push(';');
        } else {
            let (kind, arg) = token.split_once(':').unwrap();
            let v: i32 = arg.parse().unwrap();
            match kind {
                "P" => {
                    recorder.key_pressed(v, 0);
                }
                "R" => {
                    recorder.key_released(v, 0);
                }
                "T" => {
                    recorder.key_typed(v as u16, 0);
                }
                other => panic!("unknown key op {other}"),
            }
        }
    }
    if out.is_empty() {
        "~".to_string()
    } else {
        out
    }
}

fn hex(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{b:02x}")).collect()
}

#[test]
fn client_packet_bodies_match_the_kotlin_oracle() {
    let mut encoder = RegistryEncoder::<ClientPacket>::new(cipher(), 65535).unwrap();
    let mut shadow = cipher().unwrap();
    let mut compared = 0usize;
    let mut errors = 0usize;
    let mut silent = 0usize;
    let mut shadow_in_step = true;
    for (line_no, line) in FIXTURE.lines().enumerate() {
        let mut fields = line.split('\t');
        let name = fields.next().unwrap();
        let args = fields.next().unwrap();
        let expected = fields.next().unwrap();
        let tokens: Vec<&str> = args.split(' ').collect();
        if name == "MOUSE" || name == "KEYS" {
            shadow_in_step = false;
            let actual = if name == "MOUSE" {
                run_mouse(&mut encoder, &tokens)
            } else {
                run_keys(&tokens)
            };
            assert_eq!(actual, expected, "line {}: {name} {args}", line_no + 1);
            compared += 1;
            continue;
        }
        let actual = match build(name, &tokens) {
            Err(_) => "err".to_string(),
            Ok(None) => "-".to_string(),
            Ok(Some(body)) => match encoder.encode(body.packet, &body.body) {
                Ok(frame) => {
                    if shadow_in_step {
                        assert_eq!(
                            frame_body(&mut shadow, &body).as_deref(),
                            Ok(frame.as_slice()),
                            "line {}: frame_body diverges",
                            line_no + 1
                        );
                    }
                    hex(&frame)
                }
                Err(e) => panic!("line {}: {name} {args}: encode failed: {e:?}", line_no + 1),
            },
        };
        assert_eq!(actual, expected, "line {}: {name} {args}", line_no + 1);
        match expected {
            "err" => errors += 1,
            "-" => silent += 1,
            _ => compared += 1,
        }
    }
    assert!(compared > 5000);
    assert!(errors > 0 && silent > 0);
}
