use super::*;
use openrs_cs2::Opcode;

fn script(code: &[(Opcode, i32)]) -> BTreeMap<i32, Script> {
    BTreeMap::from([(0, Script::decode(&script_bytes(code)).unwrap())])
}

fn script_bytes(code: &[(Opcode, i32)]) -> Vec<u8> {
    let code: Vec<_> = code.iter().copied().chain([(Opcode::RETURN, 0)]).collect();
    let mut bytes = vec![0];
    for (opcode, operand) in &code {
        bytes.extend_from_slice(&opcode.wire().to_be_bytes());
        if opcode.metadata().has_int_constant {
            bytes.extend_from_slice(&operand.to_be_bytes());
        } else {
            bytes.push(*operand as u8);
        }
    }
    bytes.extend_from_slice(&(code.len() as i32).to_be_bytes());
    bytes.extend_from_slice(&[0; 12]);
    bytes.extend_from_slice(&[0, 0, 1]);
    bytes
}

#[test]
fn hosted_script_reads_live_varps_and_reports_stacks_and_instruction_count() {
    let repository = script(&[(Opcode::PUSH_VAR, 3), (Opcode::PUSH_CONSTANT_INT, 5)]);
    let mut variables = WorldVariables::new(8).unwrap();
    variables.set_local(3, 77, 0).unwrap();
    let mut host = WorldVarHost::new(&mut variables, 0);
    let report = run_hosted(
        0,
        &repository,
        &RunInputs::default(),
        Some(&mut host),
        vec!["varps"],
    );
    assert_eq!(report.status, "ok");
    assert_eq!(report.instructions_executed, Some(3));
    assert_eq!(report.ints, [77, 5]);
    assert_eq!(report.error, None);
    assert_eq!(report.hosts, ["varps"]);
}

#[test]
fn hosted_script_failure_reports_the_first_error_without_throwing() {
    let repository = script(&[(Opcode::PUSH_VAR, 9999)]);
    let mut variables = WorldVariables::new(8).unwrap();
    let mut host = WorldVarHost::new(&mut variables, 0);
    let report = run_hosted(
        0,
        &repository,
        &RunInputs::default(),
        Some(&mut host),
        vec![],
    );
    assert_eq!(report.status, "error");
    assert_eq!(report.instructions_executed, None);
    let error = report.error.unwrap();
    assert_eq!((error.script_id, error.instruction), (0, 0));
    assert!(error.message.contains("InvalidVarp"));
}

#[test]
fn hosted_script_without_a_wired_host_method_fails_closed() {
    let repository = script(&[(Opcode::STAT, 0)]);
    let mut variables = WorldVariables::new(1).unwrap();
    let mut host = WorldVarHost::new(&mut variables, 0);
    let report = run_hosted(
        0,
        &repository,
        &RunInputs::default(),
        Some(&mut host),
        vec![],
    );
    assert_eq!(report.status, "error");
}

#[test]
fn argument_parsing_is_bounded_and_typed() {
    let inputs = parse_inputs("[1,-2]", "[\"a\",null]").unwrap();
    assert_eq!(inputs.ints, [1, -2]);
    assert_eq!(inputs.strings[0].as_deref(), Some("a"));
    assert_eq!(inputs.strings[1], None);
    assert!(parse_inputs("[1.5]", "[]").is_err());
    assert!(parse_inputs("[]", "[1]").is_err());
    assert!(parse_inputs(&format!("[{}0]", "0,".repeat(1001)), "[]").is_err());
    assert!(parse_inputs("[]", &format!("[\"{}\"]", "x".repeat(4097))).is_err());
    assert!(parse_inputs(&" ".repeat(MAX_ARGUMENT_BYTES + 1), "[]").is_err());
}

#[test]
fn incomplete_graph_refuses_to_run() {
    let run = Cs2Run::new(b"token", 1).unwrap();
    assert!(run.graph.repository().is_err());
}

#[test]
fn stateless_run_executes_pure_code_and_fails_closed_on_host_opcodes() {
    let pure = script(&[(Opcode::PUSH_CONSTANT_INT, 9)]);
    let report = run_hosted(0, &pure, &RunInputs::default(), None, Vec::new());
    assert_eq!(report.scope, "stateless");
    assert_eq!(report.status, "ok");
    assert_eq!(report.ints, [9]);
    assert!(report.hosts.is_empty());
    let hosted = script(&[(Opcode::PUSH_VAR, 3)]);
    let report = run_hosted(0, &hosted, &RunInputs::default(), None, Vec::new());
    assert_eq!(report.status, "error");
    assert!(report.error.is_some());
}

#[test]
fn bytes_run_uses_a_fresh_varp_host_and_reports_stacks() {
    let bytes = script_bytes(&[(Opcode::PUSH_VAR, 3), (Opcode::PUSH_CONSTANT_INT, 5)]);
    let report = run_script_bytes(&bytes, 8, &RunInputs::default()).unwrap();
    assert_eq!(report.scope, "world-session-host");
    assert_eq!(report.status, "ok");
    assert_eq!(report.ints, [0, 5]);
    assert_eq!(report.hosts, ["varps"]);
    let report = run_script_bytes(&bytes, 2, &RunInputs::default()).unwrap();
    assert_eq!(report.status, "error");
    assert!(report.error.unwrap().message.contains("InvalidVarp"));
}

#[test]
fn bytes_run_rejects_malformed_scripts_and_oversized_varp_tables() {
    assert!(run_script_bytes(&[1, 2, 3], 8, &RunInputs::default()).is_err());
    assert!(run_script_bytes(&[], 8, &RunInputs::default()).is_err());
    let bytes = script_bytes(&[(Opcode::PUSH_CONSTANT_INT, 1)]);
    assert!(run_script_bytes(&bytes, 1 << 20, &RunInputs::default()).is_err());
}
