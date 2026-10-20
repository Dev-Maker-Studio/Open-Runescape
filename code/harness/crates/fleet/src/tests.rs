use crate::config::{Cfg, Slot};
use crate::{at, creds, up};
use std::path::PathBuf;

fn setup() -> PathBuf {
    static ROOT: std::sync::OnceLock<PathBuf> = std::sync::OnceLock::new();
    ROOT.get_or_init(|| {
        let root = std::env::temp_dir().join(format!("openrs-fleet-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&root);
        std::fs::create_dir_all(root.join("home")).unwrap();
        std::env::set_var("HOME", root.join("home"));
        std::env::set_var("OPENRS_STATE", root.join("state"));
        root
    })
    .clone()
}

fn fixture() -> Cfg {
    Cfg::parse(&std::fs::read_to_string(concat!(env!("CARGO_MANIFEST_DIR"), "/testdata/fleet.toml")).unwrap())
}

fn slot(n: usize, account: &str, creds: &str) -> Slot {
    Slot { n, role: "r".into(), account: account.into(), server: "local".into(), creds: creds.into(), start: vec![], heap: "512m".into() }
}

#[test]
fn example_config_defines_nine_local_slots() {
    let cfg = Cfg::parse(&std::fs::read_to_string(concat!(env!("CARGO_MANIFEST_DIR"), "/../../fleet.toml.example")).unwrap());
    for n in 1..=9 {
        let s = cfg.slot(n).unwrap();
        assert_eq!((s.n, s.account.clone(), s.server.as_str(), s.creds.as_str(), s.heap.as_str(), s.start.len()), (n, format!("fleet{n}"), "local", "local", "512m", 3));
    }
    assert_eq!(cfg.slot(1).unwrap().role, "model-pass");
    assert_eq!(cfg.slot(9).unwrap().start, vec![3234, 3228, 0]);
    assert!(cfg.slot(10).is_err());
    assert_eq!(cfg.server("shared").unwrap(), ("lobby.devmakerstudio.com".into(), "43594".into(), "43595".into()));
    assert_eq!(cfg.get("defaults.parallel", "1"), "3");
}

#[test]
fn slots_fall_back_to_defaults_and_builtins() {
    let cfg = fixture();
    let s = cfg.slot(1).unwrap();
    assert_eq!((s.role.as_str(), s.account.as_str(), s.server.as_str(), s.creds.as_str(), s.heap.as_str()), ("model-pass", "Fleet One", "local", "env:OPENRS_TEST_FLEET_PASS", "256m"));
    assert_eq!(s.start, vec![3212, 3428, 0]);
    let s = cfg.slot(2).unwrap();
    assert_eq!((s.server.as_str(), s.creds.as_str(), s.heap.as_str()), ("partial", "file:~/pw#2.txt", "1g"));
    assert_eq!(s.start, vec![3029, 3217]);
    assert_eq!(cfg.slot(3).err().unwrap(), "slot 3: missing role");
    assert_eq!(cfg.slot(4).err().unwrap(), "slot 4: missing role");
    let bare = Cfg::parse("[slot.1]\nrole=a\naccount=b\nstart=1 2 3\n").slot(1).unwrap();
    assert_eq!((bare.server.as_str(), bare.creds.as_str(), bare.heap.as_str()), ("local", "local", "512m"));
}

#[test]
fn servers_and_plain_keys() {
    let cfg = fixture();
    assert_eq!(cfg.server("local").unwrap(), ("127.0.0.1".into(), "24594".into(), "24595".into()));
    assert_eq!(cfg.server("partial").err().unwrap(), "server partial: missing lobby");
    assert_eq!(cfg.server("nowhere").err().unwrap(), "server nowhere: missing host");
    assert_eq!(cfg.get("defaults.window", "800x600"), "800x600");
    assert_eq!(cfg.get("defaults.heap", "x"), "256m");
}

#[test]
fn env_and_file_credentials() {
    let root = setup();
    std::env::set_var("OPENRS_TEST_FLEET_PASS", "s3cret");
    assert_eq!(creds::password(&slot(1, "a", "env:OPENRS_TEST_FLEET_PASS")).unwrap(), "s3cret");
    assert_eq!(creds::password(&slot(1, "a", "env:OPENRS_TEST_FLEET_UNSET")).unwrap_err(), "slot 1: env OPENRS_TEST_FLEET_UNSET is not set");
    std::fs::write(root.join("home/pw#2.txt"), "  hunter2\n").unwrap();
    assert_eq!(creds::password(&slot(2, "b", "file:~/pw#2.txt")).unwrap(), "hunter2");
    let abs = root.join("abs~pw");
    std::fs::write(&abs, "tilde\n").unwrap();
    assert_eq!(creds::password(&slot(2, "b", &format!("file:{}", abs.display()))).unwrap(), "tilde");
    assert!(creds::password(&slot(2, "b", "file:/nonexistent/pw")).unwrap_err().starts_with("slot 2: /nonexistent/pw: "));
    assert_eq!(creds::password(&slot(5, "b", "vault:x")).unwrap_err(), "slot 5: credentials must be local, env:NAME or file:PATH");
    assert!(creds::password(&slot(5, "b", "plain")).is_err());
}

#[test]
fn local_credentials_are_generated_once_per_account() {
    let root = setup();
    let a = creds::password(&slot(1, "fleet1", "local")).unwrap();
    let b = creds::password(&slot(2, "fleet2", "local")).unwrap();
    assert_eq!(a.len(), 24);
    assert!(a.chars().all(|c| c.is_ascii_hexdigit()));
    assert_ne!(a, b);
    assert_eq!(creds::password(&slot(7, "fleet1", "local")).unwrap(), a);
    let file = root.join("home/.config/openrs/fleet-local.json");
    let v: serde_json::Value = serde_json::from_str(&std::fs::read_to_string(&file).unwrap()).unwrap();
    assert_eq!((v["fleet1"].as_str(), v["fleet2"].as_str()), (Some(a.as_str()), Some(b.as_str())));
    assert_eq!(std::os::unix::fs::PermissionsExt::mode(&file.metadata().unwrap().permissions()) & 0o777, 0o600);
}

#[test]
fn slots_get_isolated_state_paths() {
    let root = setup();
    assert_eq!(up::sock(3), root.join("state/s3/c.sock"));
    assert_eq!(at(&["4".to_string(), "pos".to_string()]), Ok(4));
    assert_eq!(std::env::var_os("OPENRS_CONTROL"), Some(root.join("state/s4/c.sock").into()));
    assert_eq!(std::env::var_os("OPENRS_FOOTPRINTS"), Some(root.join("state/s4/fp").into()));
    for bad in [&["0"][..], &["10"], &["x"], &[]] {
        assert!(at(&bad.iter().map(|s| s.to_string()).collect::<Vec<_>>()).is_err());
    }
}
