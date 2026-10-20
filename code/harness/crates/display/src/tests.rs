use crate::state;

#[test]
fn state_files_and_pids() {
    let d = std::env::temp_dir().join(format!("openrs-display-{}", std::process::id()));
    let _ = std::fs::remove_dir_all(&d);
    std::env::set_var("OPENRS_STATE", d.join("nested"));
    assert_eq!(crate::dir(), d.join("nested"));
    assert!(d.join("nested").is_dir());
    state::write("client", &format!(" {}\n", std::process::id()));
    assert_eq!(state::read("client"), Some(std::process::id().to_string()));
    assert_eq!(crate::client_pid(), None);
    state::write("xserver", &format!(":101 {}", std::process::id()));
    assert_eq!(crate::xserver_pid(), None);
    state::write("xserver", ":101 4000000000");
    assert_eq!(crate::xserver_pid(), None);
    state::write("client", "garbage");
    assert_eq!(crate::client_pid(), None);
    assert_eq!(state::read("missing"), None);
    std::env::remove_var("OPENRS_ROOT");
    let found = crate::root().unwrap();
    assert!(found.join("_APP-SPECIFICATION.md").exists());
    assert!(std::path::Path::new(env!("CARGO_MANIFEST_DIR")).starts_with(&found));
    std::env::set_var("OPENRS_ROOT", &d);
    assert_eq!(crate::root().unwrap(), d);
}
