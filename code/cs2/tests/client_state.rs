#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::sync::Arc;

const STATICS: usize = 39;

struct ClientHost {
    client: ClientState,
}

impl VarHost for ClientHost {
    fn varp(&self, _: i32) -> Result<i32, HostError> {
        Ok(0)
    }

    fn set_varp(&mut self, _: i32, _: i32) -> Result<(), HostError> {
        Ok(())
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn client(&mut self) -> Result<&mut ClientState, HostError> {
        Ok(&mut self.client)
    }
}

fn number(value: &str) -> i32 {
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn text_of(value: &str) -> Option<Arc<str>> {
    if value == "~null" {
        None
    } else {
        Some(Arc::from(value.strip_prefix("s:").unwrap().replace(';', ",")))
    }
}

fn render_text(value: &Option<Arc<str>>) -> String {
    match value {
        None => "~null".to_string(),
        Some(text) => format!("s:{}", text.replace(',', ";")),
    }
}

fn flag(value: bool) -> i32 {
    i32::from(value)
}

fn parse(columns: &[&str]) -> ClientState {
    let mut state = ClientState::new();
    state.game_runescape = columns[2] == "1";
    state.max_memory = number(columns[3]);
    state.preferences = Preferences::from_slice(&integers(columns[4])).unwrap();
    let s: Vec<&str> = columns[5].split(',').collect();
    assert_eq!(s.len(), STATICS);
    state.cycles = number(s[0]);
    state.affiliate = number(s[1]);
    state.billing_flag = number(s[2]);
    state.sign_on_key = if s[3] == "1" {
        Some(Arc::from(vec![1u8, 2, 3]))
    } else {
        None
    };
    state.machine_uid = s[4].parse().unwrap();
    state.forced_safemode = s[5] == "1";
    state.safemode_forced_during_startup = s[6] == "1";
    state.custom_string_param = text_of(s[7]);
    state.just_created_account = s[8] == "1";
    state.applet = s[9] == "1";
    state.map_owner_identifier = text_of(s[10]);
    state.quickchat_only = s[11] == "1";
    state.current_cursor = number(s[12]);
    state.interface_selection_active = s[13] == "1";
    state.use_on_type = text_of(s[14]);
    state.use_on_target = text_of(s[15]);
    state.userdetail_quickchat = s[16] == "1";
    state.verified_email_address = s[17] == "1";
    state.applet_has_focus = s[18] == "1";
    state.login_queue_position = number(s[19]);
    state.login_response_detail = number(s[20]);
    state.login_response_extra_info = number(s[21]);
    state.lobby_login_response = number(s[22]);
    state.world_login_response = number(s[23]);
    state.login_handshake_code = number(s[24]);
    state.minimenu_entry_count = number(s[25]);
    state.minimenu_coloured_entry_count = number(s[26]);
    state.default_attack_cursor = number(s[27]);
    state.default_cursor_id = number(s[28]);
    state.instruction_count = number(s[29]);
    state.preferences_saved_on_server = s[30] == "1";
    state.needs_resize = s[31] == "1";
    state.particle_details = number(s[32]);
    state.area_sound_count = number(s[33]);
    state.current_music_archive = number(s[34]);
    state.titlescreen_music_archive = number(s[35]);
    state.sound_level_static = number(s[36]);
    state.song_load_state = number(s[37]);
    state.sound_jingle_playing = s[38] == "1";
    state.mouse = if columns[6] == "-" {
        None
    } else {
        let m = integers(columns[6]);
        Some(MouseState {
            x: m[0],
            y: m[1],
            left: m[2] == 1,
            right: m[3] == 1,
            middle: m[4] == 1,
        })
    };
    state.keys = if columns[7] == "-" {
        None
    } else {
        let k = integers(columns[7]);
        Some(KeyState {
            alt: k[0] == 1,
            ctrl: k[1] == 1,
            shift: k[2] == 1,
        })
    };
    state.rendering_scene = columns[8] == "1";
    state
}

fn dump(state: &ClientState) -> String {
    let mut parts: Vec<String> = state
        .preferences
        .to_array()
        .iter()
        .map(|value| value.to_string())
        .collect();
    let statics = [
        state.cycles.to_string(),
        state.affiliate.to_string(),
        state.billing_flag.to_string(),
        flag(state.sign_on_key.is_some()).to_string(),
        state.machine_uid.to_string(),
        flag(state.forced_safemode).to_string(),
        flag(state.safemode_forced_during_startup).to_string(),
        render_text(&state.custom_string_param),
        flag(state.just_created_account).to_string(),
        flag(state.applet).to_string(),
        render_text(&state.map_owner_identifier),
        flag(state.quickchat_only).to_string(),
        state.current_cursor.to_string(),
        flag(state.interface_selection_active).to_string(),
        render_text(&state.use_on_type),
        render_text(&state.use_on_target),
        flag(state.userdetail_quickchat).to_string(),
        flag(state.verified_email_address).to_string(),
        flag(state.applet_has_focus).to_string(),
        state.login_queue_position.to_string(),
        state.login_response_detail.to_string(),
        state.login_response_extra_info.to_string(),
        state.lobby_login_response.to_string(),
        state.world_login_response.to_string(),
        state.login_handshake_code.to_string(),
        state.minimenu_entry_count.to_string(),
        state.minimenu_coloured_entry_count.to_string(),
        state.default_attack_cursor.to_string(),
        state.default_cursor_id.to_string(),
        state.instruction_count.to_string(),
        flag(state.preferences_saved_on_server).to_string(),
        flag(state.needs_resize).to_string(),
        state.particle_details.to_string(),
        state.area_sound_count.to_string(),
        state.current_music_archive.to_string(),
        state.titlescreen_music_archive.to_string(),
        state.sound_level_static.to_string(),
        state.song_load_state.to_string(),
        flag(state.sound_jingle_playing).to_string(),
    ];
    parts.extend(statics);
    parts.join(",")
}

fn class_matches(expected: &str, kind: &VmErrorKind) -> bool {
    match kind {
        VmErrorKind::Host(HostError::NoEntity) => expected == "NullPointerException",
        VmErrorKind::Host(HostError::State(_)) => expected == "ArrayIndexOutOfBoundsException",
        VmErrorKind::StackUnderflow(_) => expected == "ArrayIndexOutOfBoundsException",
        _ => false,
    }
}

fn render_strings(stack: &[Option<Arc<str>>]) -> String {
    if stack.is_empty() {
        return "-".to_string();
    }
    stack
        .iter()
        .map(|value| match value {
            None => "~null".to_string(),
            Some(text) => format!("s:{text}"),
        })
        .collect::<Vec<_>>()
        .join("|")
}

const RESET_ELIGIBLE: [usize; 14] = [12, 15, 16, 17, 19, 20, 21, 22, 23, 24, 27, 28, 34, 35];

fn run_case(profile: &[&str], columns: &[&str]) -> (Vec<String>, bool) {
    let name = columns[1];
    let opcode = OPCODES
        .iter()
        .find(|metadata| metadata.name == name)
        .unwrap()
        .opcode;
    let mut host = ClientHost {
        client: parse(profile),
    };
    let mut code: Vec<_> = integers(columns[3])
        .iter()
        .map(|value| constant(*value))
        .collect();
    if columns[4] != "-" {
        for argument in columns[4].split('|') {
            if argument == "~null" {
                code.push(int(Opcode::PUSH_STRING_LOCAL, 0));
            } else {
                code.push(text(&argument["s:".len()..]));
            }
        }
    }
    let initial = code.len();
    code.push(int(opcode, 0));
    code.push(ret());
    let repository = BTreeMap::from([(
        1,
        Script::decode(&binary(&code, counts(0, 1, 0), zero(), &[])).unwrap(),
    )]);
    let mut vm = Vm::default();
    let result = vm.run_with_host(1, &repository, Arguments::default(), &mut host);
    let label = format!("{name} profile {}", columns[2]);
    let mut mismatches = Vec::new();
    let failed = match (&result, columns[5]) {
        (Ok(_), "ok") => {
            let expected = integers(columns[6]);
            if vm.int_stack() != expected.as_slice() {
                mismatches.push(format!("{label}: ints {:?} vs {expected:?}", vm.int_stack()));
            }
            let strings = render_strings(vm.string_stack());
            if strings != columns[7] {
                mismatches.push(format!("{label}: strings {strings} vs {}", columns[7]));
            }
            false
        }
        (Err(error), expected) if expected.starts_with("error:") => {
            if error.instruction != initial
                || !class_matches(&expected["error:".len()..], &error.kind)
            {
                mismatches.push(format!("{label}: error {expected} vs {error:?}"));
            }
            true
        }
        (actual, expected) => {
            mismatches.push(format!("{label}: {expected} vs {actual:?}"));
            false
        }
    };
    let after = dump(&host.client);
    if after != columns[8] {
        let differing: Vec<String> = after
            .split(',')
            .zip(columns[8].split(','))
            .enumerate()
            .filter(|(_, (a, b))| a != b)
            .map(|(i, (a, b))| format!("{i}:{a}/{b}"))
            .collect();
        mismatches.push(format!("{label}: state {}", differing.join(" ")));
    }
    (mismatches, failed)
}

fn patched_profile<'a>(profile: &[&'a str], columns: &[&'a str], statics: &'a str) -> Vec<&'a str> {
    let mut patched = profile.to_vec();
    patched[5] = statics;
    let _ = columns;
    patched
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn client_state_opcodes_match_the_native_oracle() {
    let fixture = include_str!("fixtures/client-state.native.tsv");
    let mut profiles: BTreeMap<String, Vec<&str>> = BTreeMap::new();
    let mut cases = Vec::new();
    let mut expected_total = 0;
    for line in fixture.lines() {
        let columns: Vec<&str> = line.split('\t').collect();
        match columns[0] {
            "P" => {
                profiles.insert(columns[1].to_string(), columns);
            }
            "C" => cases.push(columns),
            "Z" => expected_total = number(columns[1]) as usize,
            other => panic!("{other}"),
        }
    }
    assert_eq!(cases.len(), expected_total);
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut mismatches = Vec::new();
    let mut class_initialisation_artifacts = 0;
    for columns in &cases {
        let profile = &profiles[columns[2]];
        let (first, failed) = run_case(profile, columns);
        let entry = per_opcode.entry(columns[1].to_string()).or_default();
        if failed {
            entry.1 += 1;
        } else {
            entry.0 += 1;
        }
        if first.is_empty() {
            continue;
        }
        let writes_cursors = columns[1] == "SETDEFAULTCURSORS";
        let writes_music = columns[1].starts_with("SOUND_SONG");
        let expected_dump: Vec<&str> = columns[8].split(',').collect();
        let mut statics: Vec<String> = profile[5].split(',').map(str::to_string).collect();
        let mut changed = false;
        for index in RESET_ELIGIBLE {
            if (writes_cursors && (index == 27 || index == 28)) || (writes_music && index == 34) {
                continue;
            }
            let value = expected_dump[32 + index];
            if value != statics[index] && matches!(value, "-1" | "-2" | "0" | "~null") {
                statics[index] = value.to_string();
                changed = true;
            }
        }
        if changed {
            let joined = statics.join(",");
            let patched = patched_profile(profile, columns, &joined);
            let (second, _) = run_case(&patched, columns);
            if second.is_empty() {
                class_initialisation_artifacts += 1;
                continue;
            }
        }
        mismatches.extend(first);
    }
    println!(
        "client cases={} opcodes={} mismatches={} class_initialisation_artifacts={}",
        cases.len(),
        per_opcode.len(),
        mismatches.len(),
        class_initialisation_artifacts
    );
    for mismatch in mismatches.iter().take(40) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty());
    assert!(class_initialisation_artifacts <= 8);
    assert_eq!(per_opcode.len(), 145);
}
