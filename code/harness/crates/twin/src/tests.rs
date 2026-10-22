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
fn frames_get_their_own_folder_and_default_to_every_frame() {
    let o = parse(&args(&["varrock", "--frames", "20", "--every", "2"]), &root()).unwrap();
    assert_eq!((o.frames, o.every), (Some(20), 2));
    assert_eq!(o.out, root().join("code/harness/target/twin/varrock-frames"));
    let o = parse(&args(&["--frames", "5"]), &root()).unwrap();
    assert_eq!((o.frames, o.every), (Some(5), 1));
    assert_eq!(parse(&[], &root()).unwrap().frames, None);
}

#[test]
fn frame_options_are_validated() {
    assert!(parse(&args(&["--every", "2"]), &root()).is_err());
    assert!(parse(&args(&["--every", "1"]), &root()).is_err());
    assert!(parse(&args(&["--frames", "0"]), &root()).is_err());
    assert!(parse(&args(&["--frames", "601"]), &root()).is_err());
    assert!(parse(&args(&["--frames", "x"]), &root()).is_err());
    assert!(parse(&args(&["--frames", "3", "--every", "0"]), &root()).is_err());
}

#[test]
fn python_gets_the_frame_options() {
    assert_eq!(frames::python_args(30, 2), ["--frames", "30", "--every", "2"]);
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

#[test]
fn here_skips_the_teleport_and_names_the_scene() {
    let o = parse(&args(&["--here"]), &root()).unwrap();
    assert!(o.here);
    assert_eq!(o.scene, "here");
    assert_eq!(tile(&o), None);
    assert_eq!(o.out, PathBuf::from("/repo/code/harness/target/twin/here"));
}

#[test]
fn no_teleport_is_an_alias_and_works_with_frames() {
    let o = parse(&args(&["--no-teleport", "--frames", "4"]), &root()).unwrap();
    assert!(o.here);
    assert_eq!(o.out, PathBuf::from("/repo/code/harness/target/twin/here-frames"));
}

#[test]
fn here_rejects_a_scene_or_a_tile() {
    assert!(parse(&args(&["--here", "lumbridge"]), &root()).is_err());
    assert!(parse(&args(&["--here", "--at", "1", "2", "0"]), &root()).is_err());
    assert!(parse(&args(&["lumbridge", "--here"]), &root()).is_err());
}
