use crate::{edit, gameval, store};
use serde_json::{json, Value};
use std::path::PathBuf;

fn setup() -> PathBuf {
    static ROOT: std::sync::OnceLock<PathBuf> = std::sync::OnceLock::new();
    ROOT.get_or_init(|| {
        let root = std::env::temp_dir().join(format!("openrs-playerstore-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&root);
        std::fs::create_dir_all(&root).unwrap();
        std::env::set_var("OPENRS_PLAYER_DIR", &root);
        std::env::set_var("OPENRS_GAMEVAL_DIR", concat!(env!("CARGO_MANIFEST_DIR"), "/testdata/gamevals"));
        root
    })
    .clone()
}

fn cli(a: &[&str]) -> i32 {
    setup();
    crate::run(&a.iter().map(|s| s.to_string()).collect::<Vec<_>>())
}

fn read(name: &str) -> Value {
    store::read(name).unwrap()
}

#[test]
fn skill_index_accepts_names_aliases_and_numbers() {
    assert_eq!(edit::skill_index("attack"), Ok(0));
    assert_eq!(edit::skill_index("defense"), Ok(1));
    assert_eq!(edit::skill_index("range"), Ok(4));
    assert_eq!(edit::skill_index("24"), Ok(24));
    assert!(edit::skill_index("25").is_err());
    assert!(edit::skill_index("sailing").is_err());
}

#[test]
fn xp_table_matches_runescape() {
    assert_eq!(edit::xp_for_level(1), 0.0);
    assert_eq!(edit::xp_for_level(2), 83.0);
    assert_eq!(edit::xp_for_level(10), 1154.0);
    assert_eq!(edit::xp_for_level(99), 13034431.0);
}

#[test]
fn set_at_builds_paths_and_get_at_reads_them() {
    let mut v = json!({"keep": 1, "a": 5});
    edit::set_at(&mut v, "a.b.2.c", json!("x"));
    assert_eq!(v, json!({"keep": 1, "a": {"b": [null, null, {"c": "x"}]}}));
    assert_eq!(edit::get_at(&v, "a.b.2.c"), Some(&json!("x")));
    assert_eq!(edit::get_at(&v, "a.b.0"), Some(&Value::Null));
    assert_eq!(edit::get_at(&v, "a.b.9"), None);
    assert_eq!(edit::get_at(&v, "missing.x"), None);
    edit::set_at(&mut v, "keep", json!(2));
    assert_eq!(v["keep"], 2);
}

#[test]
fn get_at_treats_numeric_keys_of_objects_as_keys() {
    let v = json!({"vars": {"281": 7}});
    assert_eq!(edit::get_at(&v, "vars.281"), Some(&json!(7)));
}

#[test]
fn parse_value_falls_back_to_string() {
    assert_eq!(edit::parse_value("12"), json!(12));
    assert_eq!(edit::parse_value("true"), json!(true));
    assert_eq!(edit::parse_value("{\"a\":[1]}"), json!({"a": [1]}));
    assert_eq!(edit::parse_value("hello world"), json!("hello world"));
}

#[test]
fn skills_items_and_varps_edit_in_place() {
    let mut v = json!({"username": "x"});
    edit::set_skill(&mut v, 3, 10, None);
    edit::set_skill(&mut v, 0, 99, Some(5.5));
    assert_eq!(v["skills"]["level"], json!([99, null, null, 10]));
    assert_eq!(v["skills"]["xp"], json!([5.5, null, null, 1154.0]));
    let (inv, size) = edit::container("inv").unwrap();
    assert_eq!((inv, size), ("inventory.items.data", 28));
    assert_eq!(edit::container("equipment").unwrap().1, 15);
    assert!(edit::container("bank").is_err());
    assert_eq!(edit::free_slot(&v, inv, size), Some(0));
    edit::set_item(&mut v, inv, 0, 995, 10);
    edit::set_item(&mut v, inv, 2, 1511, 1);
    assert_eq!(edit::free_slot(&v, inv, size), Some(1));
    edit::clear_slot(&mut v, inv, 0);
    assert_eq!(edit::free_slot(&v, inv, size), Some(0));
    assert_eq!(v["inventory"]["items"]["data"][2], json!({"id": 1511, "amount": 1, "slot": 2}));
    assert_eq!(edit::free_slot(&v, inv, 0), None);
    edit::set_varp(&mut v, 281, 3);
    edit::set_varp(&mut v, 300, -1);
    edit::set_varp(&mut v, 281, 4);
    assert_eq!(v["varManager"]["vars"], json!({"281": 4, "300": -1}));
}

#[test]
fn keys_and_bot_account_format() {
    assert_eq!(store::key("Big Bob/x"), "big_bob_x");
    assert_eq!(
        store::account("Fleet Bot"),
        json!({"username": "fleet_bot", "email": "fleet_bot@bots.darkan.org", "recoveryEmail": "fleet_bot@bots.darkan.org", "displayName": "Fleet Bot", "passwordHash": "", "bot": true})
    );
}

#[test]
fn write_to_is_atomic_and_pretty() {
    let p = setup().join("export/out.json");
    store::write_to(&p, &json!({"a": 1})).unwrap();
    assert_eq!(std::fs::read_to_string(&p).unwrap(), "{\n  \"a\": 1\n}");
    assert!(!p.with_extension("json.tmp").exists());
}

#[test]
fn gameval_resolves_numbers_names_and_components() {
    setup();
    assert_eq!(gameval::resolve("obj", "42"), Ok(42));
    assert_eq!(gameval::resolve("obj", "coins"), Ok(995));
    assert_eq!(gameval::resolve("obj", "nothing"), Err("no obj named nothing".into()));
    assert_eq!(gameval::name("interface", "320").as_deref(), Some("stats"));
    assert_eq!(gameval::name("interface", "1"), None);
    assert_eq!(gameval::component(320, "attack"), Ok(1));
    assert_eq!(gameval::component(320, "stats:defence"), Ok(2));
    assert_eq!(gameval::component(320, "attack_text"), Ok(10));
    assert_eq!(gameval::component(320, "7"), Ok(7));
    assert!(gameval::component(320, "main").is_err());
    assert_eq!(gameval::component(548, "main"), Ok(7));
    assert_eq!(gameval::components(320), vec![(1, "stats:attack".into()), (2, "stats:defence".into()), (10, "attack_text".into())]);
    assert_eq!(gameval::components(548), vec![(7, "toplevel:main".into())]);
    assert!(gameval::load("missing").is_err());
}

#[test]
fn create_writes_default_position_and_refuses_duplicates() {
    assert_eq!(cli(&["create", "Create Test"]), 0);
    let v = read("create test");
    assert_eq!(v["username"], "create_test");
    assert_eq!(v["tile"], json!({"x": 3222, "y": 3218, "plane": 0}));
    assert_eq!(cli(&["create", "create test"]), 2);
    assert_eq!(cli(&["create", "create_at", "3200", "3300", "1"]), 0);
    assert_eq!(read("create_at")["tile"], json!({"x": 3200, "y": 3300, "plane": 1}));
    assert!(!setup().join("accounts/create_test.json").exists());
}

#[test]
fn create_bot_writes_account_file() {
    assert_eq!(cli(&["create", "Helper_Bot"]), 0);
    let text = std::fs::read_to_string(setup().join("accounts/helper_bot.json")).unwrap();
    assert_eq!(serde_json::from_str::<Value>(&text).unwrap(), store::account("Helper_Bot"));
}

#[test]
fn player_edits_through_the_cli() {
    assert_eq!(cli(&["create", "editor"]), 0);
    assert_eq!(cli(&["pos", "editor", "3000", "3100"]), 0);
    assert_eq!(cli(&["skill", "editor", "all", "99"]), 0);
    assert_eq!(cli(&["skill", "editor", "defense", "50", "1000"]), 0);
    assert_eq!(cli(&["item", "editor", "inv", "3", "coins", "500"]), 0);
    assert_eq!(cli(&["give", "editor", "inv", "logs"]), 0);
    assert_eq!(cli(&["give", "editor", "equip", "1511", "2"]), 0);
    assert_eq!(cli(&["varp", "editor", "quest_points", "12"]), 0);
    assert_eq!(cli(&["set", "editor", "chosenAccountType", "true"]), 0);
    assert_eq!(cli(&["set", "editor", "title", "the brave"]), 0);
    let v = read("editor");
    assert_eq!(v["tile"], json!({"x": 3000, "y": 3100, "plane": 0}));
    assert_eq!(v["skills"]["level"].as_array().unwrap().len(), 25);
    assert_eq!((v["skills"]["level"][1].clone(), v["skills"]["xp"][1].clone()), (json!(50), json!(1000.0)));
    assert_eq!(v["skills"]["xp"][0], json!(13034431.0));
    assert_eq!(v["inventory"]["items"]["data"][3], json!({"id": 995, "amount": 500, "slot": 3}));
    assert_eq!(v["inventory"]["items"]["data"][0], json!({"id": 1511, "amount": 1, "slot": 0}));
    assert_eq!(v["equipment"]["items"]["data"][0], json!({"id": 1511, "amount": 2, "slot": 0}));
    assert_eq!(v["varManager"]["vars"]["281"], 12);
    assert_eq!((v["chosenAccountType"].clone(), v["title"].clone()), (json!(true), json!("the brave")));
    assert_eq!(cli(&["clear", "editor", "inv", "3"]), 0);
    assert!(read("editor")["inventory"]["items"]["data"][3].is_null());
    assert_eq!(cli(&["clear", "editor", "inv"]), 0);
    assert!(read("editor")["inventory"]["items"]["data"].as_array().unwrap().iter().all(Value::is_null));
}

#[test]
fn bad_edits_fail_without_changing_the_file() {
    assert_eq!(cli(&["create", "badedits"]), 0);
    let before = read("badedits");
    for a in [
        &["item", "badedits", "inv", "28", "coins"][..],
        &["item", "badedits", "bank", "0", "coins"],
        &["item", "badedits", "inv", "0", "no_such_obj"],
        &["skill", "badedits", "sailing", "5"],
        &["pos", "badedits", "x", "1"],
        &["varp", "badedits", "nope", "1"],
        &["pos", "nobody", "1", "1"],
        &["frobnicate"],
    ] {
        assert_eq!(cli(a), 2, "{a:?}");
    }
    assert_eq!(read("badedits"), before);
}

#[test]
fn give_fails_when_container_is_full() {
    assert_eq!(cli(&["create", "fullbag"]), 0);
    for _ in 0..15 {
        assert_eq!(cli(&["give", "fullbag", "equip", "coins"]), 0);
    }
    assert_eq!(cli(&["give", "fullbag", "equip", "coins"]), 2);
}

#[test]
fn import_of_partial_file_merges_with_later_edits() {
    let src = setup().join("partial.json");
    std::fs::write(&src, r#"{"username":"partial","bank":{"tabs":[1,2]},"skills":{"level":[5]}}"#).unwrap();
    assert_eq!(cli(&["import", "partial", src.to_str().unwrap()]), 0);
    assert_eq!(cli(&["skill", "partial", "magic", "40", "10"]), 0);
    assert_eq!(cli(&["pos", "partial", "1", "2", "3"]), 0);
    let v = read("partial");
    assert_eq!(v["bank"], json!({"tabs": [1, 2]}));
    assert_eq!(v["skills"]["level"], json!([5, null, null, null, null, null, 40]));
    assert_eq!(v["tile"], json!({"x": 1, "y": 2, "plane": 3}));
    let out = setup().join("exported.json");
    assert_eq!(cli(&["export", "partial", out.to_str().unwrap()]), 0);
    assert_eq!(serde_json::from_str::<Value>(&std::fs::read_to_string(out).unwrap()).unwrap(), v);
}

#[test]
fn list_and_delete() {
    assert_eq!(cli(&["create", "zz_listed"]), 0);
    assert!(store::list().contains(&"zz_listed".to_string()));
    let l = store::list();
    assert!(l.windows(2).all(|w| w[0] <= w[1]));
    assert_eq!(cli(&["delete", "zz_listed"]), 0);
    assert!(!store::list().contains(&"zz_listed".to_string()));
}

#[test]
fn summary_lists_the_essentials() {
    let mut v = json!({"username": "sum"});
    edit::set_position(&mut v, 1, 2, 0);
    edit::set_skill(&mut v, 6, 3, Some(200.0));
    edit::set_item(&mut v, "equipment.items.data", 4, 995, 7);
    edit::set_varp(&mut v, 1, 1);
    assert_eq!(crate::cli::summary(&v), "name: sum\nposition: 1 2 0\nskill magic: 3 (200.0 xp)\nequip[4]: 995 x7\nvarps: 1\n");
}

fn iface(a: &[&str]) -> i32 {
    let mut full = vec!["iface", "ifaces"];
    full.extend(a);
    cli(&full)
}

fn entry(id: i64) -> Value {
    read("ifaces")["interfaces"]["open"].as_array().unwrap().iter().find(|e| e["id"] == id).cloned().unwrap_or(Value::Null)
}

#[test]
fn interface_edits() {
    assert_eq!(cli(&["create", "ifaces"]), 0);
    assert_eq!(iface(&["text", "stats", "attack", "hi"]), 2);
    assert_eq!(iface(&["open", "stats"]), 0);
    assert_eq!(entry(320), json!({"id": 320, "name": "stats", "slot": "CENTRAL"}));
    assert_eq!(iface(&["open", "548", "tab_skills"]), 0);
    assert_eq!(entry(548), json!({"id": 548, "name": "toplevel", "slot": "TAB_SKILLS"}));
    assert_eq!(iface(&["open", "77", "toplevel:main"]), 0);
    assert_eq!(entry(77), json!({"id": 77, "name": null, "parent": {"interface": 548, "component": 7}}));
    assert_eq!(iface(&["text", "stats", "attack", "hello there"]), 0);
    assert_eq!(iface(&["hide", "320", "2", "true"]), 0);
    assert_eq!(iface(&["set", "stats", "attack_text", "item", "coins", "5"]), 0);
    assert_eq!(iface(&["set", "stats", "attack_text", "model", "123"]), 0);
    assert_eq!(iface(&["set", "stats", "9", "events", "0", "27", "1026"]), 0);
    assert_eq!(iface(&["set", "stats", "9", "colour", "1"]), 2);
    assert_eq!(iface(&["set", "stats", "9", "model", "x"]), 2);
    let c = &entry(320)["components"];
    assert_eq!(c["1"], json!({"name": "stats:attack", "pin": true, "text": "hello there"}));
    assert_eq!(c["2"], json!({"name": "stats:defence", "pin": true, "hidden": true}));
    assert_eq!(c["10"], json!({"name": "attack_text", "pin": true, "item": {"id": 995, "amount": 5}, "model": 123}));
    assert_eq!(c["9"], json!({"name": null, "pin": true, "events": {"from": 0, "to": 27, "settings": 1026}}));
    assert_eq!(iface(&["close", "548"]), 0);
    assert_eq!(entry(548), json!({"id": 548, "name": "toplevel", "slot": "TAB_SKILLS", "open": false}));
    assert_eq!(iface(&["open", "548"]), 0);
    assert_eq!(entry(548), json!({"id": 548, "name": "toplevel", "slot": "CENTRAL"}));
    assert_eq!(iface(&["close", "stats"]), 0);
    assert_eq!(entry(320)["open"], false);
    assert_eq!(iface(&["open", "stats", "tab_skills"]), 0);
    let e = entry(320);
    assert_eq!((e["slot"].clone(), e["open"].clone(), e["components"]["1"]["text"].clone()), (json!("TAB_SKILLS"), Value::Null, json!("hello there")));
    assert_eq!(iface(&["close", "999", "overlay"]), 0);
    assert_eq!(entry(999), json!({"id": 999, "name": null, "slot": "OVERLAY", "open": false}));
    assert_eq!(read("ifaces")["interfaces"]["open"].as_array().unwrap().iter().map(|e| e["id"].as_i64().unwrap()).collect::<Vec<_>>(), [77, 548, 320, 999]);
    assert_eq!(iface(&["list"]), 0);
    assert_eq!(iface(&["show", "stats"]), 0);
    assert_eq!(iface(&["open", "nowhere"]), 2);
    assert_eq!(iface(&["open", "stats", "toplevel:nothing"]), 2);
    assert_eq!(iface(&["spin"]), 2);
}
