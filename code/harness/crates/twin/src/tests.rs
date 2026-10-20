use super::*;

fn args(a: &[&str]) -> Vec<String> {
    a.iter().map(|s| s.to_string()).collect()
}

fn root() -> PathBuf {
    PathBuf::from("/repo")
}

#[test]
fn default_scene_is_lumbridge_with_a_scene_folder() {
    let o = parse(&[], &root()).unwrap();
    assert_eq!(o.scene, "lumbridge");
    assert_eq!(o.out, root().join("code/harness/target/twin/lumbridge"));
    assert!(!o.renderdoc && !o.keep);
}

#[test]
fn named_scene_resolves_to_its_tile() {
    let o = parse(&args(&["varrock"]), &root()).unwrap();
    assert_eq!(tile(&o), Some(["3213".into(), "3428".into(), "0".into()]));
}

#[test]
fn at_overrides_the_scene_and_keeps_the_given_tile() {
    let o = parse(&args(&["--at", "3029", "3217", "0", "--renderdoc", "--keep"]), &root()).unwrap();
    assert_eq!(o.scene, "live");
    assert_eq!(tile(&o), Some(["3029".into(), "3217".into(), "0".into()]));
    assert!(o.renderdoc && o.keep);
}

#[test]
fn login_is_not_a_world_scene_and_unknown_names_list_the_choices() {
    let e = parse(&args(&["login"]), &root()).err().unwrap();
    assert!(e.contains("unknown scene login") && e.contains("lumbridge") && !e.contains(" login "));
    assert!(parse(&args(&["nowhere"]), &root()).is_err());
}

#[test]
fn relative_out_is_resolved_against_the_caller() {
    let o = parse(&args(&["--out", "rel/dir"]), &root()).unwrap();
    assert_eq!(o.out, std::env::current_dir().unwrap().join("rel/dir"));
}

#[test]
fn bad_options_are_rejected() {
    assert!(parse(&args(&["--bogus"]), &root()).is_err());
    assert!(parse(&args(&["--at", "1", "2"]), &root()).is_err());
}

#[test]
fn tail_keeps_the_last_lines_only() {
    let text = (1..=10).map(|i| i.to_string()).collect::<Vec<_>>().join("\n");
    assert_eq!(tail(&text), "5\n6\n7\n8\n9\n10");
}
