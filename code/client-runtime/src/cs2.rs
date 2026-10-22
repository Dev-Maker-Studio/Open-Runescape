use openrs_cs2::{Arguments, DecodeLimits, Script, Vm, VmLimits};
use serde::Serialize;
use std::collections::BTreeMap;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CoreScriptReport {
    pub scope: &'static str,
    pub instructions: usize,
    pub instructions_executed: u64,
    pub ints: Vec<i32>,
    pub strings: Vec<Option<String>>,
    pub longs: Vec<String>,
    pub local_ints: Vec<i32>,
    pub local_strings: Vec<Option<String>>,
    pub local_longs: Vec<String>,
    pub arrays: Vec<Vec<i32>>,
}

pub fn run_core_script(bytes: &[u8]) -> Result<CoreScriptReport, String> {
    let script = Script::decode_with_limits(
        bytes,
        DecodeLimits {
            max_script_bytes: 64 * 1024,
            max_instructions: 8192,
            max_string_bytes: 64 * 1024,
        },
    )
    .map_err(|error| error.to_string())?;
    let repo = BTreeMap::from([(0, script)]);
    run_repository(0, &repo, Arguments::default())
}

pub(crate) fn run_repository(
    root: i32,
    repo: &BTreeMap<i32, Script>,
    arguments: Arguments<'_>,
) -> Result<CoreScriptReport, String> {
    let instructions = repo
        .get(&root)
        .ok_or_else(|| "root script not loaded".to_string())?
        .instructions()
        .len();
    let mut vm = Vm::new(VmLimits {
        max_instructions: 20_000,
        max_local_values: 4096,
        max_string_bytes: 64 * 1024,
        max_join_bytes: 256 * 1024,
    });
    let execution = vm
        .run(root, repo, arguments)
        .map_err(|error| error.to_string())?;
    Ok(CoreScriptReport {
        scope: "pure-core-no-host",
        instructions,
        instructions_executed: execution.instructions_executed,
        ints: vm.int_stack().to_vec(),
        strings: vm
            .string_stack()
            .iter()
            .map(|value| value.as_deref().map(str::to_owned))
            .collect(),
        longs: vm.long_stack().iter().map(ToString::to_string).collect(),
        local_ints: execution.locals.ints,
        local_strings: execution
            .locals
            .strings
            .iter()
            .map(|value| value.as_deref().map(str::to_owned))
            .collect(),
        local_longs: execution
            .locals
            .longs
            .iter()
            .map(ToString::to_string)
            .collect(),
        arrays: (0..5)
            .map(|slot| vm.array(slot).unwrap_or_default().to_vec())
            .collect(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn native_flow_fixture_runs_without_a_game_host() {
        let result =
            run_core_script(include_bytes!("../../cs2/tests/fixtures/vm-flow.bin")).unwrap();
        assert_eq!(result.instructions_executed, 158);
        assert_eq!(&result.ints[result.ints.len() - 2..], &[900, 901]);
        assert_eq!(result.scope, "pure-core-no-host");
    }

    #[test]
    fn native_string_fixture_preserves_null_join_and_branding() {
        let result =
            run_core_script(include_bytes!("../../cs2/tests/fixtures/vm-strings.bin")).unwrap();
        assert_eq!(result.instructions_executed, 8);
        assert_eq!(result.strings[0].as_deref(), Some(""));
        assert!(
            result.strings[1]
                .as_deref()
                .unwrap()
                .starts_with("nulldarkan Darkan Darkan RUNESCAPE €?")
        );
        assert_eq!(result.local_strings[0], result.strings[1]);
    }

    #[test]
    fn rejects_host_effects_and_exhausts_infinite_branch_fuel() {
        for (opcode, operand, expected) in [
            (openrs_cs2::Opcode::PUSH_VAR, 0i32, "UnsupportedOpcode"),
            (openrs_cs2::Opcode::BRANCH, -1i32, "InstructionLimit"),
        ] {
            let mut bytes = vec![0];
            bytes.extend_from_slice(&opcode.wire().to_be_bytes());
            bytes.extend_from_slice(&operand.to_be_bytes());
            bytes.extend_from_slice(&1i32.to_be_bytes());
            bytes.extend_from_slice(&[0; 12]);
            bytes.extend_from_slice(&[0, 0, 1]);
            assert!(run_core_script(&bytes).unwrap_err().contains(expected));
        }
    }

    #[test]
    fn rejects_oversized_and_invalid_scripts() {
        assert!(run_core_script(&[]).is_err());
        assert!(run_core_script(&vec![0; 64 * 1024 + 1]).is_err());
    }
}
