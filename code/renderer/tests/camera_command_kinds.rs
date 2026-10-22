use renderer::scripted_camera as camera;

const RUNTIME: &str = include_str!("../../client-runtime/src/camera_commands.rs");

fn runtime_constant(name: &str) -> i32 {
    let prefix = format!("pub const {name}: i32 = ");
    let line = RUNTIME
        .lines()
        .find(|line| line.starts_with(&prefix))
        .unwrap_or_else(|| panic!("client-runtime camera_commands.rs has no constant {name}"));
    line[prefix.len()..]
        .trim_end_matches(';')
        .parse()
        .unwrap_or_else(|_| panic!("constant {name} is not an integer literal"))
}

#[test]
fn command_kinds_match_the_client_runtime_encoding() {
    let pairs = [
        ("FORCE_ANGLE", camera::COMMAND_FORCE_ANGLE),
        ("SMOOTH_RESET", camera::COMMAND_SMOOTH_RESET),
        ("RESET", camera::COMMAND_RESET),
        ("LOOK_AT", camera::COMMAND_LOOK_AT),
        ("MOVE_TO", camera::COMMAND_MOVE_TO),
        ("SHAKE", camera::COMMAND_SHAKE),
        ("TARGET", camera::COMMAND_TARGET),
    ];
    for (name, renderer_value) in pairs {
        assert_eq!(runtime_constant(name), renderer_value, "{name}");
    }
}
