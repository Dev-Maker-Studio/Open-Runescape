#[allow(dead_code)]
mod common;

use common::*;
use openrs_cs2::*;
use std::collections::BTreeMap;
use std::sync::Arc;

const KEYS: [i32; 4] = [0x1234567, -0x7edcba98, 0x13579bdf, 0x2468ace0];

struct EffectHost {
    client: ClientState,
}

impl VarHost for EffectHost {
    fn varp(&self, id: i32) -> Result<i32, HostError> {
        Err(HostError::InvalidVarp(id))
    }

    fn set_varp(&mut self, id: i32, _: i32) -> Result<(), HostError> {
        Err(HostError::InvalidVarp(id))
    }

    fn varbit(&self, _: i32) -> Result<VarbitLookup, HostError> {
        Ok(VarbitLookup::Absent)
    }

    fn client(&mut self) -> Result<&mut ClientState, HostError> {
        Ok(&mut self.client)
    }
}

fn number<T: std::str::FromStr>(value: &str) -> T
where
    T::Err: std::fmt::Debug,
{
    value.parse().unwrap()
}

fn integers(value: &str) -> Vec<i32> {
    if value == "-" {
        Vec::new()
    } else {
        value.split(',').map(number).collect()
    }
}

fn decode(value: &str) -> Option<String> {
    if value == "n" {
        return None;
    }
    let digits = &value[1..];
    let bytes: Vec<u8> = (0..digits.len() / 2)
        .map(|index| u8::from_str_radix(&digits[index * 2..index * 2 + 2], 16).unwrap())
        .collect();
    Some(String::from_utf8(bytes).unwrap())
}

fn encode(value: &str) -> String {
    let mut text = String::from("x");
    for byte in value.as_bytes() {
        text.push_str(&format!("{byte:02x}"));
    }
    text
}

fn optional(value: &str) -> Option<i32> {
    (value != "n").then(|| number(value))
}

fn show(value: Option<i32>) -> String {
    value.map_or_else(|| "n".to_string(), |value| value.to_string())
}

fn flag(value: bool) -> &'static str {
    if value {
        "1"
    } else {
        "0"
    }
}

fn apply(client: &mut ClientState, state: &str) {
    let f: Vec<&str> = state.split(',').collect();
    assert_eq!(f.len(), 32);
    let login = &mut client.login;
    login.game_state = number(f[0]);
    login.login_stage = number(f[1]);
    login.lobby_stage = number(f[2]);
    login.selected_world_id = number(f[3]);
    login.social_network_token = number(f[4]);
    login.social_network_nonce = number(f[5]);
    login.social_network_login = f[6] == "1";
    login.has_stored_credentials = f[7] == "1";
    login.login_username = decode(f[8]).unwrap();
    login.login_password = Secret(decode(f[9]).unwrap());
    client.login_queue_position = number(f[10]);
    client.login_response_detail = number(f[11]);
    client.login_response_extra_info = number(f[12]);
    client.lobby_login_response = number(f[13]);
    client.world_login_response = number(f[14]);
    let login = &mut client.login;
    login.last_disconnect_response = number(f[15]);
    login.last_disconnect_arg1 = number(f[16]);
    login.last_disconnect_arg2 = number(f[17]);
    login.create_stage = match f[18] {
        "0" => CreateStage::None,
        "1" => CreateStage::Request,
        _ => CreateStage::Response,
    };
    login.create_request = optional(f[19]);
    login.check_email_status = optional(f[20]);
    login.received_response = optional(f[21]);
    login.lobby_connection_open = f[22] == "1";
    login.game_connection_open = f[23] == "1";
    login.lobby_dropped = f[24] == "1";
    login.game_dropped = f[25] == "1";
    login.current_connection = match f[26] {
        "0" => None,
        "1" => Some(LoginConnection::Lobby),
        _ => Some(LoginConnection::Game),
    };
    login.connection_tick_count = number(f[27]);
    login.connection_retry_count = number(f[28]);
    client.just_created_account = f[29] == "1";
    let login = &mut client.login;
    login.login_xteas = (f[30] == "1").then_some(KEYS);
    login.base64_url = f[31].split_once(':').map(|(a, b)| (number(a), number(b)));
    login.effects.clear();
}

fn render(client: &ClientState) -> String {
    let login = &client.login;
    let stage = match login.create_stage {
        CreateStage::None => "0",
        CreateStage::Request => "1",
        CreateStage::Response => "2",
    };
    let current = match login.current_connection {
        None => "0",
        Some(LoginConnection::Lobby) => "1",
        Some(LoginConnection::Game) => "2",
    };
    let base64 = login
        .base64_url
        .map_or_else(|| "n".to_string(), |(token, nonce)| format!("{token}:{nonce}"));
    [
        login.game_state.to_string(),
        login.login_stage.to_string(),
        login.lobby_stage.to_string(),
        login.selected_world_id.to_string(),
        login.social_network_token.to_string(),
        login.social_network_nonce.to_string(),
        flag(login.social_network_login).to_string(),
        flag(login.has_stored_credentials).to_string(),
        encode(&login.login_username),
        encode(&login.login_password.0),
        client.login_queue_position.to_string(),
        client.login_response_detail.to_string(),
        client.login_response_extra_info.to_string(),
        client.lobby_login_response.to_string(),
        client.world_login_response.to_string(),
        login.last_disconnect_response.to_string(),
        login.last_disconnect_arg1.to_string(),
        login.last_disconnect_arg2.to_string(),
        stage.to_string(),
        show(login.create_request),
        show(login.check_email_status),
        show(login.received_response),
        flag(login.lobby_connection_open).to_string(),
        flag(login.game_connection_open).to_string(),
        flag(login.lobby_dropped).to_string(),
        flag(login.game_dropped).to_string(),
        current.to_string(),
        login.connection_tick_count.to_string(),
        login.connection_retry_count.to_string(),
        flag(client.just_created_account).to_string(),
        flag(login.login_xteas.is_some()).to_string(),
        base64,
    ]
    .join(",")
}

fn connection(connection: LoginConnection) -> &'static str {
    match connection {
        LoginConnection::Lobby => "lobby",
        LoginConnection::Game => "game",
    }
}

fn effects_text(effects: &[LoginEffect]) -> String {
    if effects.is_empty() {
        return "-".to_string();
    }
    effects
        .iter()
        .map(|effect| match effect {
            LoginEffect::GameState(state) => format!("state:{state}"),
            LoginEffect::CloseConnection(which) => format!("close:{}", connection(*which)),
            LoginEffect::PreparePlayer => "prepare".to_string(),
            LoginEffect::OpenLobby => "lobby".to_string(),
            LoginEffect::Logout { reconnect } => format!("logout:{reconnect}"),
            LoginEffect::LobbyPacket(LobbyPacket::CheckEmail { email }) => {
                format!("email:{}", encode(email))
            }
            LoginEffect::LobbyPacket(LobbyPacket::SignUpForm {
                email,
                password,
                age,
                agreed,
            }) => format!(
                "signup:{}:{}:{age}:{}",
                encode(email),
                encode(&password.0),
                u8::from(*agreed)
            ),
            LoginEffect::LobbyPacket(LobbyPacket::AccountCreationStage { stage }) => {
                format!("stage:{stage}")
            }
        })
        .collect::<Vec<_>>()
        .join(",")
}

fn strings_text(strings: &[Option<Arc<str>>]) -> String {
    if strings.is_empty() {
        return "-".to_string();
    }
    strings
        .iter()
        .map(|value| value.as_deref().map_or_else(|| "n".to_string(), encode))
        .collect::<Vec<_>>()
        .join(",")
}

#[cfg_attr(target_arch = "wasm32", wasm_bindgen_test::wasm_bindgen_test)]
#[cfg_attr(not(target_arch = "wasm32"), test)]
fn every_native_login_effect_case_matches_the_actual_kotlin_classes() {
    let text = include_str!("fixtures/login-effects.native.tsv");
    let mut cases = Vec::new();
    let mut total = 0;
    for line in text.lines() {
        let c: Vec<&str> = line.split('\t').collect();
        match c[0] {
            "C" => cases.push(c),
            "Z" => total = number::<usize>(c[1]),
            other => panic!("{other}"),
        }
    }
    let mut mismatches = Vec::new();
    let mut per_opcode: BTreeMap<String, (usize, usize)> = BTreeMap::new();
    let mut effect_cases = 0;
    for c in &cases {
        let names: Vec<&str> = c[1].split(',').collect();
        let int_groups: Vec<&str> = c[2].split('|').collect();
        let string_groups: Vec<&str> = c[3].split('|').collect();
        let mut strings: Vec<Option<Arc<str>>> = Vec::new();
        let mut code = Vec::new();
        for (index, name) in names.iter().enumerate() {
            let opcode = OPCODES
                .iter()
                .find(|metadata| metadata.name == *name)
                .unwrap()
                .opcode;
            if string_groups[index] != "-" {
                for value in string_groups[index].split(',') {
                    code.push(int(Opcode::PUSH_STRING_LOCAL, strings.len() as i32));
                    strings.push(decode(value).map(|text| Arc::from(text.as_str())));
                }
            }
            for value in integers(int_groups[index]) {
                code.push(constant(value));
            }
            code.push(int(opcode, 0));
        }
        code.push(ret());
        let count = strings.len() as u16;
        let repository = BTreeMap::from([(
            1,
            Script::decode(&binary(&code, counts(0, count, 0), counts(0, count, 0), &[])).unwrap(),
        )]);
        let mut host = EffectHost {
            client: ClientState::default(),
        };
        apply(&mut host.client, c[4]);
        let mut vm = Vm::default();
        let result = vm.run_with_host(
            1,
            &repository,
            Arguments {
                ints: &[],
                strings: &strings,
                longs: &[],
            },
            &mut host,
        );
        let label = format!("{} {} {} pre {}", c[1], c[2], c[3], c[4]);
        let entry = per_opcode.entry(names[0].to_string()).or_default();
        match (&result, c[5]) {
            (Ok(_), "ok") => {
                entry.0 += 1;
                let expected = integers(c[6]);
                if vm.int_stack() != expected.as_slice() {
                    mismatches.push(format!("{label}: ints {:?} vs {expected:?}", vm.int_stack()));
                }
                let actual = strings_text(vm.string_stack());
                if actual != c[7] {
                    mismatches.push(format!("{label}: strings {actual} vs {}", c[7]));
                }
                let state = render(&host.client);
                if state != c[8] {
                    mismatches.push(format!("{label}: state {state} vs {}", c[8]));
                }
                let effects = effects_text(&host.client.login.effects);
                if effects != c[9] {
                    mismatches.push(format!("{label}: effects {effects} vs {}", c[9]));
                }
                if c[9] != "-" {
                    effect_cases += 1;
                }
            }
            (Err(_), "error") => entry.1 += 1,
            (actual, expected) => mismatches.push(format!("{label}: {expected} vs {actual:?}")),
        }
    }
    println!(
        "login effects cases={} effect_cases={effect_cases} mismatches={}",
        cases.len(),
        mismatches.len()
    );
    for (name, (ok, error)) in &per_opcode {
        println!("{name} ok={ok} error={error}");
    }
    for mismatch in mismatches.iter().take(8) {
        println!("{mismatch}");
    }
    assert!(mismatches.is_empty(), "{} mismatches", mismatches.len());
    assert_eq!(cases.len(), total);
    assert_eq!(per_opcode.len(), 18);
    assert!(effect_cases > 800);
}
