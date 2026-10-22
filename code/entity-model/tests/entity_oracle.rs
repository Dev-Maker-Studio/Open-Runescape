#![cfg(not(target_arch = "wasm32"))]

mod common;

use common::{fixture, short_digest, Cache, Canon};
use openrs_entity::config::{
    item, load_mesh, plain_head_mesh, BasType, Customisation, EquipmentDefaults, IdkType, ItemType,
    NpcType, PlayerAppearance, Resources,
};
use openrs_entity::mesh::Mesh;
use openrs_entity::reader::R;
use std::cell::RefCell;
use std::collections::HashMap;

const FIXTURE: &[u8] = include_bytes!("fixtures/entity-oracle.tsv.gz");
const FIXTURE_SHA256: &str = include_str!("fixtures/entity-oracle.sha256");
const MODEL_INDEX: u32 = 7;
const CONFIG_INDEX: u32 = 2;
const NPC_INDEX: u32 = 18;
const ITEM_INDEX: u32 = 19;
const DEFAULTS_INDEX: u32 = 28;
const MODEL_CACHE_BYTES: usize = 256 << 20;

type ModelCache = (usize, HashMap<i32, Option<Vec<u8>>>);

struct Res {
    cache: RefCell<Cache>,
    models: RefCell<ModelCache>,
    items: HashMap<i32, Vec<u8>>,
    npcs: HashMap<i32, Vec<u8>>,
    idks: HashMap<i32, Vec<u8>>,
    bas: HashMap<i32, Vec<u8>>,
}

fn preload(cache: &mut Cache, index: u32, archive: u32, shift: u32) -> HashMap<i32, Vec<u8>> {
    let mut out = HashMap::new();
    let archives = if archive == u32::MAX {
        cache.archive_ids(index)
    } else {
        vec![archive]
    };
    for a in archives {
        for file in cache.file_ids(index, a) {
            if let Some(data) = cache.file(index, a, file) {
                let id = if archive == u32::MAX {
                    a << shift | file
                } else {
                    file
                };
                out.insert(id as i32, data);
            }
        }
        cache.drop_archive(index, a);
    }
    out
}

impl Res {
    fn new(mut cache: Cache) -> (Self, EquipmentDefaults) {
        let items = preload(&mut cache, ITEM_INDEX, u32::MAX, 8);
        let npcs = preload(&mut cache, NPC_INDEX, u32::MAX, 7);
        let idks = preload(&mut cache, CONFIG_INDEX, 3, 0);
        let bas = preload(&mut cache, CONFIG_INDEX, 32, 0);
        let equipment = cache
            .file(DEFAULTS_INDEX, 6, 0)
            .or_else(|| cache.file(DEFAULTS_INDEX, 0, 6))
            .expect("equipment defaults");
        let defaults = EquipmentDefaults::decode(&equipment).unwrap();
        (
            Self {
                cache: RefCell::new(cache),
                models: RefCell::new((0, HashMap::new())),
                items,
                npcs,
                idks,
                bas,
            },
            defaults,
        )
    }
}

impl Resources for Res {
    fn model(&self, id: i32) -> Option<Vec<u8>> {
        if id < 0 {
            return None;
        }
        let mut models = self.models.borrow_mut();
        if let Some(found) = models.1.get(&id) {
            return found.clone();
        }
        let mut cache = self.cache.borrow_mut();
        let data = cache.file(MODEL_INDEX, id as u32, 0);
        cache.drop_archive(MODEL_INDEX, id as u32);
        if models.0 > MODEL_CACHE_BYTES {
            models.0 = 0;
            models.1.clear();
        }
        models.0 += data.as_ref().map_or(0, Vec::len);
        models.1.insert(id, data.clone());
        data
    }

    fn item(&self, id: i32) -> Option<Vec<u8>> {
        self.items.get(&id).cloned()
    }

    fn idk(&self, id: i32) -> Option<Vec<u8>> {
        self.idks.get(&id).cloned()
    }

    fn bas(&self, id: i32) -> Option<Vec<u8>> {
        self.bas.get(&id).cloned()
    }

    fn npc(&self, id: i32) -> Option<Vec<u8>> {
        self.npcs.get(&id).cloned()
    }
}

fn canonical(m: &Mesh) -> Vec<u8> {
    let mut c = Canon::default();
    c.int(m.version);
    c.int(m.num_vertices);
    c.int(m.max_depth);
    c.int(m.num_faces);
    c.byte(m.priority as u8);
    c.int(m.num_textures);
    c.i8s(&m.textures_mapping);
    c.ints(&m.vertices_x);
    c.ints(&m.vertices_y);
    c.ints(&m.vertices_z);
    c.shorts(&m.faces_a);
    c.shorts(&m.faces_b);
    c.shorts(&m.faces_c);
    c.ints(&m.vertex_groups);
    c.i8s(&m.face_render_type);
    c.i8s(&m.faces_layer);
    c.i8s(&m.faces_alpha);
    c.ints(&m.face_groups);
    c.shorts(&m.faces_material);
    c.shorts(&m.faces_color);
    c.i8s(&m.faces_texture);
    c.shorts(&m.texture_face_x);
    c.shorts(&m.texture_face_y);
    c.shorts(&m.texture_face_z);
    c.ints(&m.textures_scale_x);
    c.ints(&m.textures_scale_y);
    c.ints(&m.textures_scale_z);
    c.i8s(&m.textures_rotation);
    c.i8s(&m.textures_direction);
    c.ints(&m.textures_speed);
    c.ints(&m.textures_utrans);
    c.ints(&m.textures_vtrans);
    match &m.particle_emitters {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for e in list {
                c.int(e.producer_id);
                c.int(e.face_a);
                c.int(e.face_b);
                c.int(e.face_c);
                c.byte(e.priority as u8);
            }
        }
    }
    match &m.particle_effectors {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for e in list {
                c.int(e.effect_id);
                c.int(e.applies_on_vertex);
            }
        }
    }
    match &m.billboards {
        None => c.byte(0),
        Some(list) => {
            c.byte(1);
            c.int(list.len() as i32);
            for b in list {
                c.int(b.id);
                c.int(b.applies_on_face);
                c.int(b.depth);
                c.int(b.distance);
            }
        }
    }
    c.shorts(&m.vertices_submeshes);
    c.shorts(&m.faces_submeshes);
    c.bytes
}

fn mesh_text(mesh: R<Option<Mesh>>) -> Option<String> {
    match mesh {
        Err(_) => None,
        Ok(None) => Some("none".to_string()),
        Ok(Some(mesh)) => Some(short_digest(&canonical(&mesh))),
    }
}

fn csv(text: &str) -> Vec<i32> {
    if text == "-" || text.is_empty() {
        return Vec::new();
    }
    text.split(',').map(|v| v.parse().unwrap()).collect()
}

fn join(values: &[i32]) -> String {
    values
        .iter()
        .map(i32::to_string)
        .collect::<Vec<_>>()
        .join(",")
}

fn unhex(text: &str) -> Vec<u8> {
    (0..text.len() / 2)
        .map(|i| u8::from_str_radix(&text[i * 2..i * 2 + 2], 16).unwrap())
        .collect()
}

fn customisation(res: &Res, ty: &ItemType, hex: &str) -> R<Option<Customisation>> {
    let _ = res;
    if hex == "-" {
        return Ok(None);
    }
    Ok(Some(Customisation::decode_effects(ty, &unhex(hex))?))
}

fn player(res: &Res, f: &[&str]) -> R<PlayerAppearance> {
    let appearance = csv(f[3]);
    let recolor = csv(f[7]);
    let mut modifiers = None;
    if f[4] != "-" {
        let mut list: Vec<Option<Customisation>> = vec![None; appearance.len()];
        for entry in f[4].split(';') {
            let (slot, hex) = entry.split_once(':').unwrap();
            let slot: usize = slot.parse().unwrap();
            let ty = item(res, appearance[slot] & 0x3fff_ffff)?;
            list[slot] = customisation(res, &ty, hex)?;
        }
        modifiers = Some(list);
    }
    let mut indices = [0; 10];
    indices.copy_from_slice(&recolor);
    Ok(PlayerAppearance {
        appearance,
        modifiers,
        female: f[1] == "1",
        bas_id: f[2].parse().unwrap(),
        recolor_dst_indices: indices,
    })
}

fn bas_of(res: &Res, defaults: &EquipmentDefaults, id: i32) -> R<Option<BasType>> {
    if id == -1 {
        return Ok(None);
    }
    Ok(Some(BasType::load(res, id, defaults)?))
}

fn compute(res: &Res, defaults: &EquipmentDefaults, f: &[&str]) -> Option<String> {
    match f[0] {
        "EQD" => {
            let hidden = |list: &Option<Vec<i32>>| join(list.as_deref().unwrap_or(&[]));
            Some(format!(
                "{},{}|{}|{}|{}",
                defaults.shield_slot,
                defaults.weapon_slot,
                join(&defaults.customizable_obj_slots),
                hidden(&defaults.hidden_shield_slots),
                hidden(&defaults.hidden_weapon_slots)
            ))
        }
        "IM" => {
            let t = item(res, f[1].parse().unwrap()).ok()?;
            let recol = t.recolor_src.as_ref().map_or(-1, |v| v.len() as i32);
            let retex = t.retex_src.as_ref().map_or(-1, |v| v.len() as i32);
            let mut values = Vec::new();
            values.extend_from_slice(&t.male_equip);
            values.extend_from_slice(&t.female_equip);
            values.extend_from_slice(&t.male_head);
            values.extend_from_slice(&t.female_head);
            values.extend_from_slice(&[t.wear_pos, t.wear_pos2, t.wear_pos3, recol, retex]);
            Some(join(&values))
        }
        "IB" | "IH" => {
            let ty = item(res, f[1].parse().unwrap()).ok()?;
            let female = f[2] == "1";
            let modifier = customisation(res, &ty, f[3]).ok()?;
            if f[0] == "IB" {
                mesh_text(ty.body_mesh(res, female, modifier.as_ref()))
            } else {
                mesh_text(ty.head_mesh(res, female, modifier.as_ref()))
            }
        }
        "KB" => {
            let idk = IdkType::load(res, f[1].parse().unwrap()).ok()?;
            mesh_text(idk.body_mesh(res))
        }
        "KH" => {
            let idk = IdkType::load(res, f[1].parse().unwrap()).ok()?;
            mesh_text(idk.head_mesh(res).map(Some))
        }
        "NB" => {
            let npc = NpcType::load(res, f[1].parse().unwrap()).ok()?;
            if npc.alternative_ids.is_some() {
                return Some("skip".to_string());
            }
            let param: i32 = f[2].parse().unwrap();
            let bas_id = if param != -1 { param } else { npc.bas_id };
            let bas = bas_of(res, defaults, bas_id).ok()?;
            mesh_text(npc.body_mesh(res, bas.as_ref()).map(Some))
        }
        "NH" => {
            let npc = NpcType::load(res, f[1].parse().unwrap()).ok()?;
            if npc.alternative_ids.is_some() {
                return Some("skip".to_string());
            }
            mesh_text(npc.head_mesh(res))
        }
        "PB" => {
            let pa = player(res, f).ok()?;
            let bas = bas_of(res, defaults, pa.bas_id).ok()?;
            let shield = f[5].parse().unwrap();
            let weapon = f[6].parse().unwrap();
            mesh_text(
                pa.body_mesh(res, bas.as_ref(), defaults, shield, weapon)
                    .map(Some),
            )
        }
        "PH" => {
            let pa = player(res, f).ok()?;
            mesh_text(pa.head_mesh(res).map(Some))
        }
        "PHASH" => Some(player(res, f).ok()?.hash().to_string()),
        "PP" => {
            let mut ids = csv(f[1]);
            ids.resize(3, -1);
            mesh_text(plain_head_mesh(res, [ids[0], ids[1], ids[2]]).map(Some))
        }
        other => panic!("unknown case {other}"),
    }
}

#[test]
fn entity_assembly_matches_the_kotlin_oracle() {
    let Some(cache) = Cache::open() else {
        return;
    };
    let text = fixture(FIXTURE, FIXTURE_SHA256);
    let (res, defaults) = Res::new(cache);
    let mut compared: HashMap<String, usize> = HashMap::new();
    let mut errors: HashMap<String, usize> = HashMap::new();
    for line in text.lines() {
        let f: Vec<&str> = line.split('\t').collect();
        let expected = match f[f.len() - 1] {
            "error" => None,
            other => Some(other.to_string()),
        };
        let actual = compute(&res, &defaults, &f[..f.len() - 1]);
        assert_eq!(actual, expected, "case {line}");
        *compared.entry(f[0].to_string()).or_default() += 1;
        if expected.is_none() {
            *errors.entry(f[0].to_string()).or_default() += 1;
        }
    }
    let mut kinds: Vec<_> = compared.iter().collect();
    kinds.sort();
    let mut failures: Vec<_> = errors.iter().collect();
    failures.sort();
    eprintln!("entity oracle: compared {kinds:?} (expected errors {failures:?})");
    let count = |kind: &str| compared.get(kind).copied().unwrap_or(0);
    assert!(count("IB") > 15000 && count("IH") > 15000 && count("NB") > 15000);
    assert!(count("PB") >= 6000 && count("PH") >= 6000 && count("PHASH") >= 6000);
    assert!(count("KB") > 600 && count("KH") > 600 && count("NH") > 15000 && count("IM") > 25000);
}

#[test]
fn config_decoders_reject_truncated_data() {
    assert!(EquipmentDefaults::decode(&[]).is_err());
    assert!(EquipmentDefaults::decode(&[1, 3, 0]).is_err());
    assert!(EquipmentDefaults::decode(&[0]).is_err());
    assert!(ItemType::decode(0, &[]).is_err());
    assert!(IdkType::decode(&[]).is_err());
    assert!(NpcType::decode(0, &[]).is_err());
    let defaults = EquipmentDefaults::default();
    assert!(BasType::decode(&[], &defaults).is_err());
    let item = ItemType::new(0);
    assert!(Customisation::decode_effects(&item, &[]).is_err());
    assert!(Customisation::decode_effects(&item, &[1, 0]).is_err());
    assert!(Customisation::decode_effects(&item, &[4, 0, 0]).is_err());
}

#[test]
fn absent_resources_yield_defaults_and_errors() {
    struct Empty;
    impl Resources for Empty {
        fn model(&self, _: i32) -> Option<Vec<u8>> {
            None
        }
        fn item(&self, _: i32) -> Option<Vec<u8>> {
            None
        }
        fn idk(&self, _: i32) -> Option<Vec<u8>> {
            None
        }
        fn bas(&self, _: i32) -> Option<Vec<u8>> {
            None
        }
        fn npc(&self, _: i32) -> Option<Vec<u8>> {
            None
        }
    }
    assert!(load_mesh(&Empty, 5).unwrap().is_none());
    let ty = item(&Empty, 9).unwrap();
    assert!(ty.body_mesh(&Empty, false, None).unwrap().is_none());
    assert!(ty.head_mesh(&Empty, true, None).unwrap().is_none());
    assert!(plain_head_mesh(&Empty, [-1, -1, -1]).is_ok());
}
