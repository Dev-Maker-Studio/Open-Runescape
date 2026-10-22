use crate::scripts::ScriptGraphProbe;
use crate::vars::config::{ConfigStore, sync_containers};
use crate::vars::{WorldVarHost, WorldVariables};
use crate::world_session::WorldSession;
use openrs_cs2::{Arguments, DecodeLimits, ItemModel, PlayerState, Script, VarHost, Vm, VmLimits};
use serde::Serialize;
use std::collections::BTreeMap;
use std::sync::Arc;

const MAX_ARGUMENT_BYTES: usize = 16 * 1024;
const MAX_INT_ARGUMENTS: usize = 1000;
const MAX_STRING_ARGUMENTS: usize = 64;
const MAX_STRING_ARGUMENT_BYTES: usize = 4096;

#[derive(Debug, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HostRunError {
    pub script_id: i32,
    pub instruction: usize,
    pub message: String,
}

#[derive(Debug, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HostRunReport {
    pub scope: &'static str,
    pub status: &'static str,
    pub script_id: i32,
    pub instructions_executed: Option<u64>,
    pub error: Option<HostRunError>,
    pub ints: Vec<i32>,
    pub strings: Vec<Option<String>>,
    pub longs: Vec<String>,
    pub hosts: Vec<&'static str>,
}

#[derive(Debug, Default, PartialEq, Eq)]
pub struct RunInputs {
    pub ints: Vec<i32>,
    pub strings: Vec<Option<Arc<str>>>,
}

pub fn parse_inputs(ints_json: &str, strings_json: &str) -> Result<RunInputs, String> {
    if ints_json.len() > MAX_ARGUMENT_BYTES || strings_json.len() > MAX_ARGUMENT_BYTES {
        return Err("script argument byte limit".into());
    }
    let ints: Vec<i32> = serde_json::from_str(ints_json)
        .map_err(|_| "invalid script integer arguments".to_string())?;
    let strings: Vec<Option<String>> = serde_json::from_str(strings_json)
        .map_err(|_| "invalid script string arguments".to_string())?;
    if ints.len() > MAX_INT_ARGUMENTS {
        return Err("script argument count limit".into());
    }
    if strings.len() > MAX_STRING_ARGUMENTS
        || strings
            .iter()
            .flatten()
            .any(|value| value.len() > MAX_STRING_ARGUMENT_BYTES)
    {
        return Err("script string argument limit".into());
    }
    Ok(RunInputs {
        ints,
        strings: strings
            .into_iter()
            .map(|value| value.map(|value| Arc::from(value.as_str())))
            .collect(),
    })
}

pub(crate) fn run_hosted(
    root: i32,
    repository: &BTreeMap<i32, Script>,
    inputs: &RunInputs,
    host: Option<&mut dyn VarHost>,
    hosts: Vec<&'static str>,
) -> HostRunReport {
    let mut vm = Vm::new(VmLimits {
        max_instructions: 20_000,
        max_local_values: 4096,
        max_string_bytes: 64 * 1024,
        max_join_bytes: 256 * 1024,
    });
    let scope = if host.is_some() {
        "world-session-host"
    } else {
        "stateless"
    };
    let arguments = Arguments {
        ints: &inputs.ints,
        strings: &inputs.strings,
        longs: &[],
    };
    let result = match host {
        Some(host) => vm.run_with_host(root, repository, arguments, host),
        None => vm.run(root, repository, arguments),
    };
    let (status, instructions_executed, error) = match result {
        Ok(execution) => ("ok", Some(execution.instructions_executed), None),
        Err(error) => (
            "error",
            None,
            Some(HostRunError {
                script_id: error.script_id,
                instruction: error.instruction,
                message: error.to_string(),
            }),
        ),
    };
    HostRunReport {
        scope,
        status,
        script_id: root,
        instructions_executed,
        error,
        ints: vm.int_stack().to_vec(),
        strings: vm
            .string_stack()
            .iter()
            .map(|value| value.as_deref().map(str::to_owned))
            .collect(),
        longs: vm.long_stack().iter().map(ToString::to_string).collect(),
        hosts,
    }
}

pub(crate) fn run_in_world(
    root: i32,
    repository: &BTreeMap<i32, Script>,
    inputs: &RunInputs,
    session: &mut WorldSession,
) -> Result<HostRunReport, String> {
    let mut player = PlayerState::default();
    let stats_bound = session.sync_player_state(&mut player).is_ok();
    let parts = session.cs2_parts().map_err(str::to_owned)?;
    let chat_bound = parts.chat.set;
    if chat_bound {
        player.player_rights = parts.chat.player_rights;
        player.my_player = Some(parts.chat.player.clone());
    }
    let player_bound = stats_bound || chat_bound;
    let store = ConfigStore::new();
    let mut model = ItemModel::default();
    let mut hosts = vec!["varps"];
    let mut host = WorldVarHost::new(parts.variables, parts.now_ms)
        .with_environment(parts.environment, parts.now_ms)
        .with_lobby_state(parts.lobby)
        .with_appearance_state(parts.appearance)
        .with_outbox(parts.outbox)
        .with_entity_state(parts.entity)
        .with_chat_state(parts.chat_state);
    hosts.extend(["clock", "lobby", "appearance", "outbox", "entity"]);
    if player_bound {
        host = host.with_player_state(&player);
        if stats_bound {
            hosts.push("playerStats");
        }
    }
    if chat_bound {
        host = host.with_client_state(&mut parts.chat.client);
        hosts.push("chat");
    }
    if let Some(containers) = parts.containers {
        sync_containers(&mut model, containers);
        host = host.with_config(&store, &mut model);
        hosts.push("inventories");
    }
    Ok(run_hosted(root, repository, inputs, Some(&mut host), hosts))
}

pub fn run_script_bytes(
    bytes: &[u8],
    varp_count: usize,
    inputs: &RunInputs,
) -> Result<HostRunReport, String> {
    let script = Script::decode_with_limits(
        bytes,
        DecodeLimits {
            max_script_bytes: 64 * 1024,
            max_instructions: 8192,
            max_string_bytes: 64 * 1024,
        },
    )
    .map_err(|error| error.to_string())?;
    let repository = BTreeMap::from([(0, script)]);
    let mut variables = WorldVariables::new(varp_count).map_err(|error| error.to_string())?;
    let mut host = WorldVarHost::new(&mut variables, 0);
    Ok(run_hosted(
        0,
        &repository,
        inputs,
        Some(&mut host),
        vec!["varps"],
    ))
}

pub struct Cs2Run {
    graph: ScriptGraphProbe,
}

impl Cs2Run {
    pub fn new(token: &[u8], root: u32) -> Result<Self, String> {
        Ok(Self {
            graph: ScriptGraphProbe::new(token, root)?,
        })
    }

    pub fn graph(&mut self) -> &mut ScriptGraphProbe {
        &mut self.graph
    }

    pub fn run(
        &self,
        inputs: &RunInputs,
        session: &mut WorldSession,
    ) -> Result<HostRunReport, String> {
        let (root, repository) = self.graph.repository()?;
        run_in_world(root, repository, inputs, session)
    }

    pub fn run_stateless(&self, inputs: &RunInputs) -> Result<HostRunReport, String> {
        let (root, repository) = self.graph.repository()?;
        Ok(run_hosted(root, repository, inputs, None, Vec::new()))
    }
}

#[cfg(target_arch = "wasm32")]
pub(crate) mod browser {
    use super::*;
    use crate::world_session::browser::BrowserWorldSession;
    use wasm_bindgen::prelude::*;

    fn script_root(value: JsValue) -> Result<u32, JsValue> {
        value
            .as_f64()
            .filter(|id| id.is_finite() && id.fract() == 0.0 && (0.0..=1_000_000.0).contains(id))
            .map(|id| id as u32)
            .ok_or_else(|| JsValue::from_str("invalid root script identity"))
    }

    fn error(message: String) -> JsValue {
        JsValue::from_str(&message)
    }

    #[wasm_bindgen(js_name = runCs2Bytes)]
    pub fn run_bytes(
        bytes: &[u8],
        varp_count: JsValue,
        ints_json: &str,
        strings_json: &str,
    ) -> Result<String, JsValue> {
        let count = varp_count
            .as_f64()
            .filter(|n| n.is_finite() && n.fract() == 0.0 && (0.0..=65_535.0).contains(n))
            .ok_or_else(|| JsValue::from_str("invalid varp count"))? as usize;
        let inputs = parse_inputs(ints_json, strings_json).map_err(error)?;
        let report = run_script_bytes(bytes, count, &inputs).map_err(error)?;
        serde_json::to_string(&report)
            .map_err(|_| JsValue::from_str("script execution serialization failed"))
    }

    #[wasm_bindgen(js_name = Js5Cs2Run)]
    pub struct BrowserCs2Run(Cs2Run);

    #[wasm_bindgen(js_class = Js5Cs2Run)]
    impl BrowserCs2Run {
        #[wasm_bindgen(constructor)]
        pub fn new(token: &str, root: JsValue) -> Result<BrowserCs2Run, JsValue> {
            Cs2Run::new(token.as_bytes(), script_root(root)?)
                .map(Self)
                .map_err(error)
        }

        pub fn outgoing(&mut self) -> Vec<u8> {
            self.0.graph().take_outgoing()
        }

        pub fn complete(&mut self) -> bool {
            self.0.graph().complete()
        }

        pub fn feed(&mut self, bytes: &[u8]) -> Result<(), JsValue> {
            self.0.graph().feed(bytes).map_err(error)
        }

        pub fn finish(&mut self) -> Result<(), JsValue> {
            self.0.graph().finish().map_err(error)
        }

        #[wasm_bindgen(js_name = runStateless)]
        pub fn run_stateless(
            &self,
            ints_json: &str,
            strings_json: &str,
        ) -> Result<String, JsValue> {
            let inputs = parse_inputs(ints_json, strings_json).map_err(error)?;
            let report = self.0.run_stateless(&inputs).map_err(error)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("script execution serialization failed"))
        }

        pub fn run(
            &self,
            session: &mut BrowserWorldSession,
            ints_json: &str,
            strings_json: &str,
        ) -> Result<String, JsValue> {
            let inputs = parse_inputs(ints_json, strings_json).map_err(error)?;
            let report = self.0.run(&inputs, &mut session.0).map_err(error)?;
            serde_json::to_string(&report)
                .map_err(|_| JsValue::from_str("script execution serialization failed"))
        }
    }
}

#[cfg(test)]
mod tests;
